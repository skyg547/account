"""Offline regression checks; no real environment input or container engine access."""
import argparse
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import shlex
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import MagicMock, call, patch

import yaml


SPEC = importlib.util.spec_from_file_location(
    "business_runner", Path(__file__).with_name("run-business-external-dev.py"))
runner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(runner)


def result(stdout="", returncode=0, stderr=""):
    return subprocess.CompletedProcess([], returncode, stdout, stderr)


class CommandTests(unittest.TestCase):
    def test_default_timeout_tolerates_host_io_pressure(self):
        process = MagicMock(returncode=0)
        process.communicate.return_value = ("", "")
        with patch.object(runner.subprocess, "Popen", return_value=process) as popen:
            runner.run(["mock-engine", "ps"])
        self.assertGreaterEqual(process.communicate.call_args.kwargs["timeout"], 120)
        self.assertTrue(popen.call_args.kwargs["start_new_session"])
        self.assertEqual(popen.call_args.kwargs["env"]["COMPOSE_PARALLEL_LIMIT"], "1")

    def test_timeout_kills_entire_process_group_and_drains_output(self):
        process = MagicMock(pid=12345)
        process.communicate.side_effect = [subprocess.TimeoutExpired("mock-engine", 120),
                                           ("suppressed", "suppressed")]
        with patch.object(runner.subprocess, "Popen", return_value=process), \
                patch.object(runner.os, "killpg") as killpg:
            with self.assertRaisesRegex(runner.GateError, "output suppressed"):
                runner.run(["mock-engine", "ps"], timeout=120)
        killpg.assert_called_once_with(12345, runner.signal.SIGKILL)
        self.assertEqual(process.communicate.call_args_list, [call(timeout=120), call()])

    def test_actual_nonzero_process_suppresses_both_streams(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            with self.assertRaises(runner.GateError) as caught:
                runner.run([sys.executable, "-c",
                            "import sys; print('secret-stdout'); "
                            "print('secret-stderr', file=sys.stderr); sys.exit(9)"])
        self.assertEqual(str(caught.exception), "command failed; output suppressed")
        self.assertEqual(output.getvalue(), "")

    def test_actual_timeout_suppresses_both_streams(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            with self.assertRaises(runner.GateError) as caught:
                runner.run([sys.executable, "-c",
                            "import sys,time; print('secret-stdout', flush=True); "
                            "print('secret-stderr', file=sys.stderr, flush=True); time.sleep(10)"],
                           timeout=0.2)
        self.assertEqual(str(caught.exception), "command unavailable or timed out; output suppressed")
        self.assertEqual(output.getvalue(), "")


class EnvIdentityTests(unittest.TestCase):
    def test_private_file_metadata_only(self):
        with tempfile.NamedTemporaryFile() as fixture:
            path = Path(fixture.name)
            with patch.object(Path, "read_text", side_effect=AssertionError("content read")):
                self.assertEqual(runner.identity(path), runner.identity(path))

    def test_symlink_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "target"
            target.touch(mode=0o600)
            link = Path(directory) / "link"
            link.symlink_to(target)
            with self.assertRaises(runner.GateError):
                runner.identity(link)

    def test_nonprivate_file_and_directory_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "public"
            path.touch(mode=0o644)
            for entry in (path, Path(directory)):
                with self.subTest(entry=entry.name), self.assertRaises(runner.GateError):
                    runner.identity(entry)

    def test_missing_file_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(runner.GateError):
                runner.identity(Path(directory) / "absent")


class MemoryGateTests(unittest.TestCase):
    def test_exact_headroom_threshold_and_above_are_accepted(self):
        for available_kib in ((2048 + 768) * 1024, (2048 + 768) * 1024 + 1):
            with self.subTest(available_kib=available_kib), \
                    patch.object(Path, "read_text", return_value=f"MemAvailable: {available_kib} kB\n"):
                runner.memory_gate()

    def test_below_headroom_threshold_is_rejected(self):
        for available_kib in ((2048 + 768) * 1024 - 1, 2048 * 1024, 0):
            with self.subTest(available_kib=available_kib), \
                    patch.object(Path, "read_text", return_value=f"MemAvailable: {available_kib} kB\n"):
                with self.assertRaisesRegex(runner.GateError, "host memory headroom gate failed"):
                    runner.memory_gate()

    def test_missing_or_malformed_memavailable_fails_closed(self):
        for raw in ("", "MemFree: 99999999 kB\n", "MemAvailable: invalid kB\n",
                    "MemAvailable: -1 kB\n"):
            with self.subTest(raw=raw), patch.object(Path, "read_text", return_value=raw):
                with self.assertRaisesRegex(runner.GateError, "memory availability could not be verified"):
                    runner.memory_gate()

    def test_unavailable_meminfo_fails_closed_without_exposing_error(self):
        for error in (FileNotFoundError("private detail"), PermissionError("private detail")):
            with self.subTest(error=type(error).__name__), \
                    patch.object(Path, "read_text", side_effect=error):
                with self.assertRaises(runner.GateError) as caught:
                    runner.memory_gate()
                self.assertEqual(str(caught.exception), "host memory availability could not be verified")


class RuntimeGateTests(unittest.TestCase):
    def test_logstash_connection_refused_is_not_a_database_failure(self):
        for body in (
                "WARN LogstashTcpSocketAppender - connection refused\n"
                "java.net.ConnectException: Connection refused\n\tat example.connect(Client.java:1)",
                "INFO HikariPool-1 - Start completed.\nWARN Logstash connection refused"):
            with self.subTest(body=body), patch.object(runner, "run", return_value=result(body)):
                self.assertEqual(runner.log_counts("docker", "cid"),
                                 {"error": 0, "startup_failure": 0, "database_failure": 0})

    def test_database_failure_markers_survive_multiline_causes(self):
        for body in (
                "org.postgresql.util.PSQLException: connection failed\n"
                "Caused by: java.net.ConnectException: Connection refused",
                "WARN HikariPool-1 - Exception during pool initialization\n"
                "Caused by: java.net.ConnectException: Connection refused",
                "HikariPool-1 - Connection is not available, request timed out",
                "hikaripool-1 - initialization failed",
                "SQLState: 08001", "SQLSTATE[08006]",
                "password authentication failed", "unable to acquire JDBC Connection",
                "could not obtain JDBC connection", "schema-validation: missing table"):
            with self.subTest(body=body), patch.object(runner, "run", return_value=result(stderr=body)):
                self.assertGreaterEqual(runner.log_counts("docker", "cid")["database_failure"], 1)

    def test_stopped_restarted_or_oom_container_rejected(self):
        for raw in ("false 0 false", "true 1 false", "true 0 true"):
            with self.subTest(raw=raw), patch.object(runner, "inspect", return_value=raw):
                self.assertFalse(runner.state("docker", "cid"))

    def test_verify_stopped_state_fails_before_health_checks(self):
        with patch.object(runner, "container", return_value="cid"), \
                patch.object(runner, "state", return_value=False), \
                patch.object(runner, "health") as health, \
                patch.object(runner.time, "sleep") as sleep:
            with self.assertRaisesRegex(runner.GateError, "stopped, restarted or OOM"):
                runner.verify("docker", "risk", "ecl-api")
        health.assert_not_called()
        sleep.assert_not_called()

    def test_verify_deadline_halts_health_or_registration_retries(self):
        for healthy in (False, True):
            with self.subTest(healthy=healthy), contextlib.ExitStack() as stack:
                for name, value in (("container", "cid"), ("state", True),
                                    ("health", healthy), ("registered", False)):
                    stack.enter_context(patch.object(runner, name, return_value=value))
                stack.enter_context(patch.object(runner.time, "monotonic", side_effect=[0, 0, 600]))
                sleep = stack.enter_context(patch.object(runner.time, "sleep"))
                logs = stack.enter_context(patch.object(runner, "log_counts"))
                with self.assertRaisesRegex(runner.GateError, "health/Eureka deadline exceeded"):
                    runner.verify("docker", "risk", "ecl-api", timeout=600)
                sleep.assert_called_once_with(5)
                logs.assert_not_called()

    def test_health_requires_valid_up_object_and_successful_command(self):
        for body, code, expected in (("not-json", 0, False), ("[]", 0, False),
                                     ("null", 0, False), ('{"status":"DOWN"}', 0, False),
                                     ('{"status":"UP"}', 1, False),
                                     ('{"status":"UP"}', 0, True)):
            with self.subTest(body=body, code=code), patch.object(runner, "run", return_value=result(body, code)):
                self.assertEqual(runner.health("docker", "cid"), expected)

    def test_eureka_matches_own_ip_port_and_status(self):
        for ip, port, status, expected in (("10.0.0.2", 8080, "UP", True),
                                           ("10.0.0.9", 8080, "UP", False),
                                           ("10.0.0.2", 9090, "UP", False),
                                           ("10.0.0.2", 8080, "DOWN", False)):
            instance = {"ipAddr": ip, "port": {"$": port}, "status": status}
            for instances in (instance, [instance]):
                body = json.dumps({"application": {"instance": instances}})
                with self.subTest(instance=instance, multiple=isinstance(instances, list)), \
                        patch.object(runner, "inspect", return_value="10.0.0.2 "), \
                        patch.object(runner, "run", return_value=result(body)):
                    self.assertEqual(runner.registered("docker", "cid", "APP"), expected)

    def test_eureka_malformed_response_rejected(self):
        for body in ("invalid", "{}", "null", '{"application":{"instance":[null]}}'):
            with self.subTest(body=body), patch.object(runner, "inspect", return_value="10.0.0.2"), \
                    patch.object(runner, "run", return_value=result(body)):
                self.assertFalse(runner.registered("docker", "cid", "APP"))

    def test_duplicate_missing_invalid_container_ids_rejected(self):
        for ids in ("", "a" * 12 + "\n" + "b" * 12, "not-an-id"):
            with self.subTest(ids=ids), patch.object(runner, "run", return_value=result(ids)):
                with self.assertRaises(runner.GateError):
                    runner.container("docker", "risk", "ecl-api")

    def test_single_container_id_accepted(self):
        with patch.object(runner, "run", return_value=result("a" * 64 + "\n")):
            self.assertEqual(runner.container("docker", "risk", "ecl-api"), "a" * 64)

    def test_cpu_nano_and_quota_limits(self):
        for raw, expected in (("805306368 500000000 0 0", True),
                              ("805306368 0 50000 100000", True),
                              ("805306368 1000000000 50000 100000", False),
                              ("805306368 0 0 0", False),
                              ("805306368 0 -1 100000", False),
                              ("1073741824 500000000 0 0", False)):
            with self.subTest(raw=raw), patch.object(runner, "inspect", return_value=raw):
                self.assertEqual(runner.limits("docker", "cid"), expected)

    def test_logs_return_counts_without_raw_values(self):
        with patch.object(runner, "run", return_value=result(
                "ERROR secret-value APPLICATION FAILED TO START", stderr="SQLState: secret-value")):
            self.assertEqual(runner.log_counts("docker", "cid"),
                             {"error": 1, "startup_failure": 1, "database_failure": 1})

    def test_verify_resource_and_log_failures(self):
        for limit, counts in ((False, {"error": 0}), (True, {"error": 1})):
            with self.subTest(limit=limit), contextlib.ExitStack() as stack:
                for name, value in (("container", "cid"), ("state", True), ("health", True),
                                    ("registered", True), ("limits", limit), ("log_counts", counts)):
                    stack.enter_context(patch.object(runner, name, return_value=value))
                with self.assertRaisesRegex(runner.GateError, "resource/log gate failed"):
                    runner.verify("docker", "risk", "ecl-api")


class ExecutionTests(unittest.TestCase):
    def setUp(self):
        self.fixture = tempfile.NamedTemporaryFile()
        self.addCleanup(self.fixture.close)
        self.args = argparse.Namespace(action="up", package="products", service=None,
                                       engine="docker", env_file=Path(self.fixture.name), timeout=30)
        self.run = self.enterContext(patch.object(runner, "run", return_value=result()))
        self.verify = self.enterContext(patch.object(runner, "verify"))
        self.memory = self.enterContext(patch.object(runner, "memory_gate"))
        self.enterContext(contextlib.redirect_stdout(io.StringIO()))

    def commands(self, action):
        return [entry.args[0] for entry in self.run.call_args_list if action in entry.args[0]]

    def test_build_scope_is_exact_and_sequential(self):
        self.args.action = "build"
        runner.execute(self.args)
        self.assertEqual([cmd[-2:] for cmd in self.commands("build")],
                         [["build", service] for service in runner.PACKAGES["products"]])
        self.assertEqual(self.memory.call_count, 3)
        self.verify.assert_not_called()

    def test_up_scope_allows_recreation_and_reverifies_package(self):
        runner.execute(self.args)
        commands = self.commands("up")
        self.assertEqual([cmd[-1] for cmd in commands],
                         ["products-db-check", *runner.PACKAGES["products"]])
        for cmd in commands:
            self.assertNotIn("--no-recreate", cmd)
            self.assertIn("--no-build", cmd)
            self.assertEqual(cmd[cmd.index("--pull") + 1], "never")
        for cmd in commands[1:]:
            self.assertIn("--no-deps", cmd)
        self.assertEqual(self.verify.call_args_list,
                         [call("docker", "products", service, 30)
                          for service in runner.PACKAGES["products"]] * 2)

    def test_up_failure_halts_remaining_starts(self):
        def fail_first_api(command, **kwargs):
            if command[-1] == "deposit-api":
                raise runner.GateError("command failed; output suppressed")
            return result()
        self.run.side_effect = fail_first_api
        with self.assertRaises(runner.GateError):
            runner.execute(self.args)
        self.assertEqual([cmd[-1] for cmd in self.commands("up")],
                         ["products-db-check", "deposit-api"])
        self.verify.assert_not_called()

    def test_verification_failure_halts_remaining_starts(self):
        self.verify.side_effect = runner.GateError("health failed")
        with self.assertRaises(runner.GateError):
            runner.execute(self.args)
        self.assertEqual([cmd[-1] for cmd in self.commands("up")],
                         ["products-db-check", "deposit-api"])

    def test_nonpositive_timeout_rejected_before_commands(self):
        for timeout in (0, -1):
            self.args.timeout = timeout
            with self.subTest(timeout=timeout), self.assertRaisesRegex(runner.GateError, "timeout must be positive"):
                runner.execute(self.args)
        self.run.assert_not_called()

    def test_foreign_service_rejected(self):
        self.args.service = "ecl-api"
        with self.assertRaises(runner.GateError):
            runner.execute(self.args)
        self.run.assert_not_called()

    def test_missing_env_rejected_before_any_engine_command(self):
        self.args.env_file = None
        with self.assertRaisesRegex(runner.GateError, "--env-file is required"):
            runner.execute(self.args)
        self.run.assert_not_called()

    def test_invalid_env_metadata_rejected_before_any_engine_command(self):
        with patch.object(runner, "identity", side_effect=runner.GateError("invalid metadata")):
            with self.assertRaisesRegex(runner.GateError, "invalid metadata"):
                runner.execute(self.args)
        self.run.assert_not_called()

    def test_selected_service_build_only(self):
        self.args.action = "build"
        self.args.service = "loan-api"
        runner.execute(self.args)
        self.assertEqual([cmd[-2:] for cmd in self.commands("build")], [["build", "loan-api"]])

    def test_env_change_after_command_stops_execution(self):
        with patch.object(runner, "identity", side_effect=[(1,), (1,), (2,)]):
            with self.assertRaisesRegex(runner.GateError, "env input identity changed"):
                runner.execute(self.args)
        self.assertEqual(self.run.call_count, 1)
        self.assertEqual(self.commands("up"), [])


class ComposePolicyTests(unittest.TestCase):
    def test_database_prerequisite_timeout_tolerates_host_io_pressure(self):
        for package in ("accounting", "products", "risk"):
            with self.subTest(package=package):
                compose = yaml.safe_load((runner.ROOT / "tools" /
                                          f"compose.{package}-external-dev.yml").read_text())
                timeout = compose["services"][f"{package}-db-check"]["healthcheck"]["timeout"]
                self.assertRegex(timeout, r"^\d+(?:\.\d+)?s$")
                self.assertGreaterEqual(float(timeout[:-1]), 30)

    def test_all_thirteen_apis_preserve_jvm_defaults_and_runtime_limits(self):
        expected_packages = {
            "accounting": {"journal-ledger-api", "closing-api", "payable-api", "receivable-api",
                           "expenditure-resolution-api", "tax-api", "reporting-api"},
            "products": {"deposit-api", "loan-api", "asset-lease-api"},
            "risk": {"account-mart-api", "ecl-api", "reconciliation-api"},
        }
        expected_options = ["-XX:MaxRAMPercentage=65.0", "-Djava.security.egd=file:/dev/./urandom",
                            "-Deureka.client.serviceUrl.defaultZone=http://minimal-discovery:8761/eureka/"]
        count = 0
        for package, expected_services in expected_packages.items():
            compose = yaml.safe_load((runner.ROOT / "tools" /
                                      f"compose.{package}-external-dev.yml").read_text())
            apis = {name: service for name, service in compose["services"].items()
                    if name.endswith("-api")}
            self.assertEqual(set(apis), expected_services)
            self.assertEqual(set(runner.PACKAGES[package]), expected_services)
            for name, service in apis.items():
                with self.subTest(package=package, service=name):
                    self.assertEqual(shlex.split(service["environment"]["JAVA_TOOL_OPTIONS"]),
                                     expected_options)
                    limits = service["deploy"]["resources"]["limits"]
                    self.assertEqual(float(limits["cpus"]), 0.5)
                    self.assertEqual(limits["memory"], "768m")
                    self.assertIn("external-dev", service["profiles"])
                    self.assertEqual(service["build"]["dockerfile"],
                                     "tools/Containerfile.minimal-auth-java")
                    count += 1
        self.assertEqual(count, 13)

    def test_builder_uses_one_gradle_worker(self):
        builder = (runner.ROOT / "tools" / "Containerfile.minimal-auth-java").read_text()
        self.assertRegex(builder, r'(?s)\./gradlew\s+"\$\{GRADLE_PROJECT\}:bootJar"\s*\\\s*'
                         r'--console=plain\s+--no-daemon\s+--max-workers=1(?:\s|;)')


if __name__ == "__main__":
    unittest.main()
