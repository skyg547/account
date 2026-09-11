"""Offline policy regression checks; real Nginx and browser smoke remain required.

Read only the checked-in Compose/Nginx files, never interpolate environment
variables or open password/socket paths. Mutations run entirely in memory.
"""

import copy
from pathlib import Path
import re
import shlex
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[2]


def compose_violations(config):
    """Return violated deployment policies without contacting the container engine."""
    failures = []

    def require(condition, policy):
        if not condition:
            failures.append(policy)

    service = config["services"]["portainer"]
    require(not service.get("ports"), "no host ports")
    require(bool(re.fullmatch(r"(?:docker\.io/)?portainer/portainer-ce:\d+\.\d+\.\d+(?:@sha256:[a-f0-9]{64})?",
                              service.get("image", ""))), "pinned CE image")
    command = service.get("command", [])
    command = shlex.split(command) if isinstance(command, str) else command
    require("--http-enabled" in command, "internal HTTP enabled")
    for flag, value in (("--base-url", "/portainer"),
                        ("--host", "unix:///var/run/docker.sock"),
                        ("--admin-password-file", "/run/secrets/portainer-admin-password")):
        require(any(command[i:i + 2] == [flag, value] for i in range(len(command))), flag)
    require("--admin-password" not in command, "no inline password")

    mounts = {}
    for mount in service.get("volumes", []):
        if isinstance(mount, str):
            source, target, *_ = mount.split(":")
            mount = {"source": source, "target": target, "type": "volume"}
        mounts[mount["target"]] = mount
    data = mounts.get("/data", {})
    volume = config.get("volumes", {}).get(data.get("source"))
    require(data.get("type") == "volume" and isinstance(volume, dict)
            and bool(volume.get("name")) and not data.get("read_only"), "persistent named data")
    for target in ("/var/run/docker.sock", "/run/secrets/portainer-admin-password"):
        mount = mounts.get(target, {})
        require(mount.get("type") == "bind" and mount.get("bind", {}).get("create_host_path") is False,
                "no host auto-creation: " + target)
    password = mounts.get("/run/secrets/portainer-admin-password", {})
    # Compose's :? rejects both unset and empty values; :- would silently weaken bootstrap.
    require(bool(re.fullmatch(r"\$\{PORTAINER_ADMIN_PASSWORD_FILE:\?[^}]+\}", password.get("source", "")))
            and password.get("read_only") is True, "required external private admin file")
    require(service.get("read_only") is True and bool(service.get("tmpfs")), "read-only root and scratch")
    # Read a foreign-owner 0600 bootstrap file without granting DAC write bypass.
    require("ALL" in service.get("cap_drop", []) and not service.get("privileged")
            and service.get("cap_add") == ["DAC_READ_SEARCH"], "restricted capabilities")
    require("no-new-privileges:true" in service.get("security_opt", []), "no privilege escalation")
    for key in ("cpus", "pids_limit"):
        require(float(service.get(key, 0)) > 0, "bounded " + key)
    require(bool(re.fullmatch(r"[1-9]\d*(?:[kmgKMG])?", str(service.get("mem_limit", "")))), "bounded memory")
    logging = service.get("logging", {})
    require(logging.get("driver") == "json-file" and
            bool(re.fullmatch(r"[1-9]\d*[kmgKMG]?", logging.get("options", {}).get("max-size", ""))) and
            int(logging.get("options", {}).get("max-file", 0)) > 0, "bounded logs")
    network = config.get("networks", {}).get("account-network", {})
    require(network.get("external") is True and
            network.get("name") == "${ACCOUNT_NETWORK_NAME:-account-network}" and
            "account-network" in service.get("networks", {}), "shared external network")
    require(service.get("restart") == "unless-stopped", "restart policy")
    return failures


def directives(block):
    """Tokenize simple leaf blocks, ignoring comments/whitespace (not a Nginx parser)."""
    rows = []
    for line in block.splitlines():
        tokens = shlex.split(line, comments=True)
        if tokens:
            tokens[-1] = tokens[-1].removesuffix(";")
            rows.append(tokens)
    return rows


def nginx_violations(config):
    failures = []

    def require(condition, policy):
        if not condition:
            failures.append(policy)

    def location(selector):
        found = re.search(r"location\s+" + re.escape(selector) + r"\s*\{([^{}]*)\}", config)
        require(found is not None, "location " + selector)
        return directives(found.group(1)) if found else []

    redirect = location("= /portainer")
    require(["absolute_redirect", "off"] in redirect and
            any(row in redirect for row in (["return", "301", "/portainer/"],
                                             ["return", "308", "/portainer/"])), "relative slash redirect")
    proxy = location("/portainer/")
    upstreams = [row[1] for row in proxy if len(row) == 3 and row[0] == "set"
                 and row[2] == "http://account-portainer:9000"]
    require(any(["proxy_pass", var] in proxy for var in upstreams) and
            bool(re.search(r"^\s*resolver\s+[^;]+;", config, re.MULTILINE)), "runtime DNS upstream")
    rewrites = [row for row in proxy if row[0] == "rewrite"]
    require(len(rewrites) == 1 and rewrites[0][1:] == ["^/portainer/(.*)$", "/$1", "break"],
            "strip prefix once")
    for header, value in (("Host", "$http_host"), ("X-Forwarded-Host", "$http_host"),
                          ("X-Forwarded-Proto", "$scheme"), ("X-Forwarded-Prefix", "/portainer"),
                          ("Upgrade", "$http_upgrade"), ("Connection", "$connection_upgrade")):
        require(["proxy_set_header", header, value] in proxy, "header " + header)
    require(["proxy_http_version", "1.1"] in proxy, "WebSocket HTTP version")
    for timeout in ("proxy_read_timeout", "proxy_send_timeout"):
        seconds = [float(row[1][:-1]) for row in proxy if row[0] == timeout
                   and re.fullmatch(r"\d+s", row[1])]
        require(bool(seconds) and min(seconds) >= 3600, "terminal " + timeout)
    require(["proxy_buffering", "off"] in proxy, "streaming without buffering")
    require(["access_log", "off"] in proxy and
            all(row == ["access_log", "off"] for row in proxy if row[0] == "access_log"),
            "no request URI access logging")
    # Upstream failures also log the original URI; access_log off alone leaks WS tokens.
    error_logs = [row for row in proxy if row[0] == "error_log"]
    require(bool(error_logs) and all(len(row) >= 2 and row[1] == "/dev/null" for row in error_logs),
            "no request URI error logging")
    upgrade_map = re.search(r"map\s+\$http_upgrade\s+\$connection_upgrade\s*\{([^{}]*)\}", config)
    map_rows = directives(upgrade_map.group(1)) if upgrade_map else []
    require(["default", "upgrade"] in map_rows and ["", "close"] in map_rows, "WebSocket upgrade map")
    return failures


class PortainerContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.compose = yaml.safe_load((ROOT / "portainer/docker-compose.yml").read_text())
        cls.nginx = (ROOT / "frontend-nginx/nginx.conf").read_text()

    def test_checked_in_compose_satisfies_deployment_policies(self):
        self.assertEqual([], compose_violations(self.compose))

    def test_checked_in_nginx_satisfies_proxy_policies(self):
        self.assertEqual([], nginx_violations(self.nginx))

    def test_rejects_unsafe_compose_variants(self):
        variants = [("ports", ["9000:9000"], "no host ports"),
                    ("image", "portainer/portainer-ce:latest", "pinned CE image"),
                    ("image", "portainer/portainer-ee:2.39.7", "pinned CE image"),
                    ("privileged", True, "restricted capabilities"),
                    ("cap_drop", [], "restricted capabilities"),
                    ("cap_add", [], "restricted capabilities"),
                    ("cap_add", ["DAC_OVERRIDE"], "restricted capabilities"),
                    ("cap_add", ["SYS_ADMIN"], "restricted capabilities"),
                    ("cap_add", ["DAC_READ_SEARCH", "DAC_OVERRIDE"], "restricted capabilities"),
                    ("cap_add", ["DAC_READ_SEARCH", "SYS_ADMIN"], "restricted capabilities"),
                    ("cap_add", ["ALL"], "restricted capabilities"),
                    ("cpus", 0, "bounded cpus"),
                    ("pids_limit", -1, "bounded pids_limit"),
                    ("mem_limit", "0m", "bounded memory"),
                    ("read_only", False, "read-only root and scratch")]
        for key, value, expected in variants:
            with self.subTest(key=key, value=value):
                changed = copy.deepcopy(self.compose)
                changed["services"]["portainer"][key] = value
                self.assertIn(expected, compose_violations(changed))

    def test_rejects_optional_password_and_auto_created_socket(self):
        for target, mutate, expected in (
            ("/run/secrets/portainer-admin-password", lambda m: m.update(source="${PORTAINER_ADMIN_PASSWORD_FILE:-}"),
             "required external private admin file"),
            ("/run/secrets/portainer-admin-password", lambda m: m.update(read_only=False),
             "required external private admin file"),
            ("/var/run/docker.sock", lambda m: m["bind"].update(create_host_path=True),
             "no host auto-creation: /var/run/docker.sock"),
        ):
            with self.subTest(target=target, expected=expected):
                changed = copy.deepcopy(self.compose)
                mount = next(m for m in changed["services"]["portainer"]["volumes"]
                             if isinstance(m, dict) and m["target"] == target)
                mutate(mount)
                self.assertIn(expected, compose_violations(changed))

    def test_rejects_loss_of_named_data_and_shared_network(self):
        changed = copy.deepcopy(self.compose)
        changed["volumes"] = {}
        self.assertIn("persistent named data", compose_violations(changed))
        changed = copy.deepcopy(self.compose)
        changed["networks"]["account-network"]["external"] = False
        self.assertIn("shared external network", compose_violations(changed))

    def test_rejects_proxy_regressions(self):
        variants = [
            ("proxy_pass $portainer_upstream;", "proxy_pass http://account-portainer:9000;", "runtime DNS upstream"),
            ("proxy_pass $portainer_upstream;", "proxy_pass $portainer_upstream/;", "runtime DNS upstream"),
            ("rewrite ^/portainer/(.*)$ /$1 break;", "", "strip prefix once"),
            ("rewrite ^/portainer/(.*)$ /$1 break;", "rewrite ^/portainer/(.*)$ /$1 last;", "strip prefix once"),
            ("absolute_redirect off;", "absolute_redirect on;", "relative slash redirect"),
            ("proxy_set_header Host $http_host;", "proxy_set_header Host $host;", "header Host"),
            ("proxy_set_header X-Forwarded-Host $http_host;", "proxy_set_header X-Forwarded-Host $host;", "header X-Forwarded-Host"),
            ("proxy_set_header Upgrade $http_upgrade;", "", "header Upgrade"),
            ("proxy_read_timeout 3600s;", "proxy_read_timeout 60s;", "terminal proxy_read_timeout"),
            ("access_log off;", "access_log /var/log/nginx/access.log;", "no request URI access logging"),
            ("error_log /dev/null;", "", "no request URI error logging"),
            ("error_log /dev/null;", "error_log /var/log/nginx/error.log;", "no request URI error logging"),
            ("error_log /dev/null;", "error_log /dev/null;\n        error_log stderr;", "no request URI error logging"),
        ]
        for original, replacement, expected in variants:
            with self.subTest(policy=expected, replacement=replacement):
                self.assertIn(original, self.nginx, "mutation must change its input")
                self.assertIn(expected, nginx_violations(self.nginx.replace(original, replacement)))

    def test_rejects_double_prefix_rewrite(self):
        rule = "rewrite ^/portainer/(.*)$ /$1 break;"
        changed = self.nginx.replace(rule, rule + "\n        " + rule)
        self.assertIn("strip prefix once", nginx_violations(changed))


if __name__ == "__main__":
    unittest.main()
