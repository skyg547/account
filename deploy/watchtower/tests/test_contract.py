"""Offline guardrails for the opt-in updater; live-smoke.py tests engine behavior.

Requires PyYAML. Mutations exercise the guardrails themselves so removing a
safety setting cannot silently turn this into a test of YAML readability only.
"""

import copy
from pathlib import Path
import re
import shlex
import unittest

import yaml


COMPOSE = Path(__file__).resolve().parents[1] / "compose.yml"
UPDATE_COMPOSE = COMPOSE.with_name("compose.update.yml")
LABEL = "com.centurylinklabs.watchtower."


def default_value(value):
    """Resolve only Compose's explicit fallback, never the caller's environment."""
    match = re.fullmatch(r"\$\{[A-Z_]+:-([^}]+)\}", str(value))
    return match.group(1) if match else str(value)


def enabled(value):
    return default_value(value).lower() in {"true", "1", "yes"}


def options(service):
    command = service.get("command", [])
    tokens = shlex.split(command) if isinstance(command, str) else command
    result = {}
    for index, token in enumerate(tokens):
        if token.startswith("--"):
            key, separator, value = token[2:].partition("=")
            if not separator:
                value = tokens[index + 1] if index + 1 < len(tokens) and not tokens[index + 1].startswith("--") else "true"
            result[key] = value
    return result


def assert_safe(document, *, update=False):
    """Check reviewable template defaults, not resolved operator overrides.

    This intentionally fails closed on new privileges/mounts. Engine socket
    access is still administrator access; these checks do not sandbox it.
    """
    service = document["services"]["watchtower"]
    flags = options(service)
    env = service.get("environment", {})
    labels = service.get("labels", {})

    def require(condition, reason):
        if not condition:
            raise AssertionError(reason)

    def setting(name, fallback=None):
        return flags.get(name, env.get("WATCHTOWER_" + name.upper().replace("-", "_"), fallback))

    require(enabled(setting("label-enable")), "selection needs explicit enable labels")
    require(setting("scope") == "account-dev", "selection needs the development scope")
    # Watchtower 1.7.1 refuses to start with both global monitor-only and
    # rolling-restart. Mode changes require the explicit update overlay.
    monitor = setting("monitor-only")
    require(str(env.get("WATCHTOWER_MONITOR_ONLY")).lower() == str(not update).lower(),
            "mode must be fixed explicitly, without environment interpolation")
    require(str(monitor).lower() == str(not update).lower(), "incorrect monitor mode")
    require(enabled(setting("rolling-restart", "false")) == update,
            "rolling restart belongs only to update mode")
    require(str(labels.get(LABEL + "enable")).lower() == "false", "updater must exclude itself")
    require(labels.get(LABEL + "scope") == setting("scope"), "updater scope must match selection")
    for flag in ("cleanup", "remove-volumes", "include-stopped", "revive-stopped", "label-take-precedence",
                 "enable-lifecycle-hooks", "http-api-update", "http-api-metrics"):
        require(not enabled(setting(flag, "false")), "dangerous option: " + flag)
    require(not any("lifecycle" in key for key in labels), "lifecycle hooks are not permitted")
    require(service.get("profiles") and all(service["profiles"]), "updater must be opt-in")
    require(not service.get("ports"), "updater must not publish ports")
    require(service.get("network_mode") != "host", "host networking is not permitted")
    require("DOCKER_HOST" not in env and "host" not in flags, "use only the local Unix socket")
    require(not service.get("privileged"), "privileged mode is not permitted")
    require(not service.get("cap_add"), "extra capabilities are not permitted")
    require("ALL" in service.get("cap_drop", []), "drop capabilities")
    require(service.get("read_only") is True, "root filesystem must be read-only")
    require("no-new-privileges:true" in service.get("security_opt", []), "prevent new privileges")
    require(not service.get("devices"), "host devices are not permitted")
    require(not service.get("env_file"), "unreviewed environment files are not permitted")
    require(not service.get("entrypoint"), "entrypoint override bypasses reviewed options")
    mounts = service.get("volumes", [])
    require(len(mounts) == 1 and isinstance(mounts[0], dict), "only the explicit socket mount is allowed")
    socket = mounts[0]
    require(socket.get("type") == "bind" and socket.get("target") == "/var/run/docker.sock", "bind only the engine socket")
    source = socket.get("source", "")
    require(bool(re.fullmatch(r"\$\{WATCHTOWER_SOCKET_PATH:\?[^}]+\}", source)), "socket path must be explicitly provided")
    require(socket.get("bind", {}).get("create_host_path") is False, "never create a missing socket directory")
    require("account-network" in service.get("networks", []), "join the account network")
    network = document.get("networks", {}).get("account-network", {})
    require(network.get("external") is True, "reuse the external account network")
    # Updater-only configured cap: this does not prove the Issue's combined
    # GUI/updater actual overhead objective of 100 MB; measure that at runtime.
    memory = re.fullmatch(r"(\d+)([mk])", str(service.get("mem_limit", "")), re.IGNORECASE)
    require(memory is not None, "memory must have an explicit limit")
    memory_kib = int(memory[1]) * (1024 if memory[2].lower() == "m" else 1)
    require(0 < memory_kib <= 100 * 1024, "memory ceiling must not exceed 100 MiB")
    require(0 < float(service.get("cpus", 0)) <= 1, "bound CPU usage")
    require(0 < int(service.get("pids_limit", 0)) <= 256, "bound process count")


def merge_update(base, overlay):
    """Apply the narrowly allowed Compose command replacement/environment merge.

    Restrict overlay shape first: new mounts, privileges, profiles or services
    must not sneak past the base contract when an operator enables updates.
    This is deliberately not a general-purpose Compose merger.
    """
    if set(overlay) != {"services"} or set(overlay["services"]) != {"watchtower"}:
        raise AssertionError("update overlay may only affect watchtower")
    changes = overlay["services"]["watchtower"]
    if set(changes) != {"command", "environment"}:
        raise AssertionError("update overlay may only change command and environment")
    if set(changes["environment"]) != {"WATCHTOWER_MONITOR_ONLY"}:
        raise AssertionError("update overlay may only change the monitor environment setting")
    merged = copy.deepcopy(base)
    service = merged["services"]["watchtower"]
    service["command"] = copy.deepcopy(changes["command"])
    service["environment"].update(changes["environment"])
    return merged


class WatchtowerContractTests(unittest.TestCase):
    def setUp(self):
        self.document = yaml.safe_load(COMPOSE.read_text())
        self.overlay = yaml.safe_load(UPDATE_COMPOSE.read_text())

    def reject(self, mutate):
        candidate = copy.deepcopy(self.document)
        mutate(candidate["services"]["watchtower"])
        with self.assertRaises(AssertionError):
            assert_safe(candidate)

    def test_checked_in_template_is_safe(self):
        assert_safe(self.document)

    def test_explicit_update_overlay_preserves_common_safety(self):
        assert_safe(merge_update(self.document, self.overlay), update=True)

    def test_incompatible_modes_and_nonrolling_updates_are_rejected(self):
        self.reject(lambda s: s["command"].append("--rolling-restart"))
        self.reject(lambda s: s["environment"].update(WATCHTOWER_ROLLING_RESTART="true"))
        updated = merge_update(self.document, self.overlay)
        updated["services"]["watchtower"]["command"].remove("--rolling-restart")
        with self.assertRaisesRegex(AssertionError, "rolling restart"):
            assert_safe(updated, update=True)
        for value in ("true", "${WATCHTOWER_MONITOR_ONLY:-false}", "${WATCHTOWER_MONITOR_ONLY}"):
            with self.subTest(update_monitor=value):
                updated = merge_update(self.document, self.overlay)
                updated["services"]["watchtower"]["environment"]["WATCHTOWER_MONITOR_ONLY"] = value
                with self.assertRaises(AssertionError):
                    assert_safe(updated, update=True)

    def test_update_overlay_cannot_change_unrelated_configuration(self):
        for key, value in (("ports", ["8080:8080"]), ("profiles", []), ("volumes", [])):
            with self.subTest(setting=key):
                overlay = copy.deepcopy(self.overlay)
                overlay["services"]["watchtower"][key] = value
                with self.assertRaisesRegex(AssertionError, "command and environment"):
                    merge_update(self.document, overlay)
        self.overlay["services"]["watchtower"]["environment"]["WATCHTOWER_REMOVE_VOLUMES"] = "true"
        with self.assertRaisesRegex(AssertionError, "monitor environment"):
            merge_update(self.document, self.overlay)

    def test_boolean_and_command_representations(self):
        service = self.document["services"]["watchtower"]
        service["command"] = shlex.join(service["command"])
        service["environment"]["WATCHTOWER_MONITOR_ONLY"] = True
        service["labels"][LABEL + "enable"] = False
        assert_safe(self.document)

    def test_selection_and_monitor_guards_cannot_be_removed(self):
        for flag in ("--label-enable", "--scope"):
            with self.subTest(flag=flag):
                self.reject(lambda s: s["command"].remove(flag))
        for value in (None, False, "${WATCHTOWER_MONITOR_ONLY:-true}",
                      "${WATCHTOWER_MONITOR_ONLY:-false}", "${WATCHTOWER_MONITOR_ONLY}"):
            with self.subTest(monitor=value):
                self.reject(lambda s: s["environment"].update(WATCHTOWER_MONITOR_ONLY=value))
        self.reject(lambda s: s["command"].append("--monitor-only=false"))
        self.reject(lambda s: s["labels"].update({LABEL + "enable": "true"}))
        self.reject(lambda s: s["labels"].pop(LABEL + "enable"))

    def test_dangerous_cli_and_environment_options_are_rejected(self):
        for option in ("cleanup", "remove-volumes", "include-stopped", "revive-stopped", "label-take-precedence",
                       "enable-lifecycle-hooks", "http-api-update", "http-api-metrics"):
            with self.subTest(option=option, location="CLI"):
                self.reject(lambda s: s["command"].append("--" + option))
            with self.subTest(option=option, location="environment"):
                self.reject(lambda s: s["environment"].update({"WATCHTOWER_" + option.upper().replace("-", "_"): "true"}))
        self.reject(lambda s: s["labels"].update({LABEL + "lifecycle.pre-update": "echo unexpected"}))

    def test_exposure_and_privilege_regressions_are_rejected(self):
        variants = {
            "ports": ["8080:8080"], "network_mode": "host", "privileged": True,
            "cap_add": ["SYS_ADMIN"], "cap_drop": [], "read_only": False,
            "security_opt": [], "devices": ["/dev/sda:/dev/sda"],
            "env_file": ["unreviewed.env"], "entrypoint": ["sh", "-c"],
        }
        for key, value in variants.items():
            with self.subTest(setting=key):
                self.reject(lambda s: s.update({key: value}))
        self.reject(lambda s: s["environment"].update(DOCKER_HOST="tcp://engine:2375"))
        self.reject(lambda s: s["command"].extend(["--host", "tcp://engine:2375"]))

    def test_mount_regressions_are_rejected(self):
        self.reject(lambda s: s["volumes"].append("/:/host"))
        self.reject(lambda s: s["volumes"][0].update(source="/"))
        self.reject(lambda s: s["volumes"][0].update(target="/host"))
        self.reject(lambda s: s["volumes"][0]["bind"].update(create_host_path=True))
        self.reject(lambda s: s.update(volumes=[]))

    def test_resource_boundaries_are_enforced(self):
        for key in ("mem_limit", "cpus", "pids_limit"):
            with self.subTest(missing=key):
                self.reject(lambda s: s.pop(key))
            with self.subTest(zero=key):
                self.reject(lambda s: s.update({key: 0}))
        for memory in ("101m", "0m", "-1m"):
            with self.subTest(memory=memory):
                self.reject(lambda s: s.update(mem_limit=memory))
        self.document["services"]["watchtower"]["mem_limit"] = "100m"
        assert_safe(self.document)

    def test_profile_and_network_are_required(self):
        self.reject(lambda s: s.pop("profiles"))
        self.reject(lambda s: s.update(profiles=[]))
        self.reject(lambda s: s.update(networks=[]))
        self.document["networks"]["account-network"].pop("external")
        with self.assertRaisesRegex(AssertionError, "external"):
            assert_safe(self.document)


if __name__ == "__main__":
    unittest.main()
