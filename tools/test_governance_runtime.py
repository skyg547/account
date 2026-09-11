"""Offline runtime policy regressions; never read credentials or contact containers.

Run with: python3 -m unittest discover -s tools -p test_governance_runtime.py -v
Requires the same PyYAML dependency as the existing runtime tooling tests.
These checks complement, and do not replace, live health/discovery verification.
"""

from pathlib import Path
import re
import shlex
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[1]


def read_yaml(relative_path):
    return yaml.safe_load((ROOT / relative_path).read_text())


class GovernanceRuntimePolicyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.apis = read_yaml("tools/compose.governance-external-dev.yml")
        cls.proxy = read_yaml("tools/compose.governance-proxy-dev.yml")
        cls.grafana = read_yaml("grafana/docker-compose.yml")

    def test_api_scope_ports_and_health_targets(self):
        expected = {"budget-api": 8096, "internal-audit-api": 8083}
        self.assertEqual(set(self.apis["services"]), set(expected))
        for name, port in expected.items():
            with self.subTest(service=name):
                service = self.apis["services"][name]
                self.assertEqual(service["pids_limit"], 256)
                self.assertEqual(service["deploy"]["resources"]["limits"]["pids"], 256)
                self.assertEqual(service["ports"], [f"127.0.0.1:{port}:{port}"])
                self.assertEqual(str(service["environment"]["SERVER_PORT"]), str(port))
                check = " ".join(service["healthcheck"]["test"])
                self.assertIn(f"http://127.0.0.1:{port}/actuator/health", check)
                self.assertIn("exit 1", check)

    def test_all_four_services_enforce_both_compose_resource_limits(self):
        services = {**self.apis["services"], **self.proxy["services"],
                    **self.grafana["services"]}
        self.assertEqual(len(services), 4)
        for name, service in services.items():
            with self.subTest(service=name):
                self.assertEqual(float(service["cpus"]), 0.5)
                self.assertEqual(service["mem_limit"], "768m")
                limits = service["deploy"]["resources"]["limits"]
                self.assertEqual(float(limits["cpus"]), 0.5)
                self.assertEqual(limits["memory"], "768m")

    def test_api_discovery_uses_case_sensitive_jvm_property(self):
        for name, service in self.apis["services"].items():
            with self.subTest(service=name):
                env = service["environment"]
                options = shlex.split(env["JAVA_TOOL_OPTIONS"])
                self.assertIn("-Deureka.client.serviceUrl.defaultZone="
                              "http://minimal-discovery:8761/eureka/", options)
                self.assertIn("-XX:MaxRAMPercentage=65.0", options)
                for key in ("SPRING_CLOUD_DISCOVERY_ENABLED", "EUREKA_CLIENT_ENABLED",
                            "EUREKA_CLIENT_REGISTER_WITH_EUREKA"):
                    self.assertEqual(env[key], "true")
                self.assertEqual(env["SPRING_CLOUD_CONFIG_ENABLED"], "false")

    def test_api_database_configuration_cannot_mutate_schema_at_startup(self):
        expected = {
            "SPRING_DATASOURCE_DRIVER_CLASS_NAME": "org.postgresql.Driver",
            "SPRING_JPA_HIBERNATE_DDL_AUTO": "validate",
            "SPRING_FLYWAY_ENABLED": "false",
            "SPRING_SQL_INIT_MODE": "never",
            "SPRING_BATCH_JDBC_INITIALIZE_SCHEMA": "never",
        }
        for name, service in self.apis["services"].items():
            for key, value in expected.items():
                with self.subTest(service=name, property=key):
                    self.assertEqual(service["environment"][key], value)

    def test_db_credentials_and_budget_jwt_require_explicit_nonempty_input(self):
        for name, prefix in (("budget-api", "BUDGET"),
                             ("internal-audit-api", "INTERNAL_AUDIT")):
            env = self.apis["services"][name]["environment"]
            for key, suffix in (("URL", "URL"), ("USERNAME", "USER"),
                                ("PASSWORD", "PASSWORD")):
                with self.subTest(service=name, property=key):
                    self.assertRegex(env[f"SPRING_DATASOURCE_{key}"],
                                     rf"^\$\{{{prefix}_DB_{suffix}:\?[^}}]+\}}$")
        self.assertRegex(self.apis["services"]["budget-api"]["environment"]["AUTH_JWT_SECRET"],
                         r"^\$\{AUTH_JWT_SECRET:\?[^}]+\}$")

    def test_infra_publications_remain_loopback_only(self):
        for compose in (self.proxy, self.grafana):
            for name, service in compose["services"].items():
                with self.subTest(service=name):
                    self.assertTrue(service["ports"])
                    for port in service["ports"]:
                        self.assertTrue(port.startswith("127.0.0.1:"), port)
                    self.assertTrue(compose["networks"]["account-network"]["external"])

    def test_nginx_routes_api_through_frontend_and_preserves_origin_port(self):
        config = (ROOT / "tools/nginx-governance-dev.conf").read_text()
        config = re.sub(r"#.*", "", config)
        # Reject added locations that could bypass the frontend BFF for /api.
        locations = re.findall(r"location\s+([^{}]+)\{([^{}]*)\}", config)
        self.assertEqual({name.strip() for name, _ in locations},
                         {"= /healthz", "= /grafana", "/grafana/", "/"})
        blocks = {name.strip(): body for name, body in locations}
        self.assertIn("set $frontend_backend http://minimal-frontend:3000;", blocks["/"])
        self.assertIn("proxy_pass $frontend_backend;", blocks["/"])
        self.assertIn("proxy_pass $grafana_backend;", blocks["/grafana/"])
        for path in ("/", "/grafana/"):
            with self.subTest(location=path):
                self.assertIn("proxy_set_header Host $http_host;", blocks[path])
                self.assertIn("proxy_set_header X-Forwarded-Host $http_host;", blocks[path])
        self.assertIn("resolver ${NGINX_LOCAL_RESOLVERS}", config)

    def test_grafana_reuses_existing_volume_without_replacing_admin_credentials(self):
        self.assertEqual(self.grafana["volumes"]["grafana-storage"],
                         {"name": "grafana-storage", "external": True})
        service = self.grafana["services"]["grafana"]
        self.assertIn("grafana-storage:/var/lib/grafana", service["volumes"])
        self.assertIn("./provisioning:/etc/grafana/provisioning:ro", service["volumes"])
        self.assertEqual(service["environment"]["GF_SERVER_SERVE_FROM_SUB_PATH"], "true")
        self.assertFalse(any(key.startswith("GF_SECURITY_ADMIN_")
                             for key in service["environment"]))

    def test_grafana_prometheus_datasource_uses_existing_network_service(self):
        provisioning = read_yaml("grafana/provisioning/datasources/datasource.yml")
        sources = [source for source in provisioning["datasources"]
                   if source["type"] == "prometheus" and source.get("isDefault")]
        self.assertEqual(len(sources), 1)
        self.assertEqual(sources[0]["url"], "http://account-prometheus-dev:9090")
        self.assertEqual(sources[0]["access"], "proxy")


if __name__ == "__main__":
    unittest.main()
