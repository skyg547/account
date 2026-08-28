"""Standard-library tests for the bounded Issue #520 runner."""

from __future__ import annotations

import importlib.util
import io
import subprocess
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock


RUNNER_PATH = Path(__file__).with_name("run-minimal-auth-external-dev.py")
SPEC = importlib.util.spec_from_file_location("minimal_auth_runner", RUNNER_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("could not load minimal Auth runner")
runner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(runner)


def completed(stdout: str = "", returncode: int = 0) -> subprocess.CompletedProcess[str]:
    return subprocess.CompletedProcess([], returncode, stdout=stdout, stderr="")


class MinimalAuthRunnerTest(unittest.TestCase):
    def test_env_file_identity_rejects_symlink_before_validation(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "target.env"
            target.write_text("fixture=value\n", encoding="utf-8")
            link = root / "linked.env"
            link.symlink_to(target)

            with self.assertRaisesRegex(runner.RunnerError, "non-symlink"):
                runner.env_file_identity(link)

    @mock.patch.object(runner.subprocess, "run")
    def test_legacy_compose_service_label_is_rejected(self, subprocess_run) -> None:
        subprocess_run.side_effect = (
            completed("container-id\n"),
            completed("auth-api|/account-auth-api-1\n"),
        )

        with self.assertRaisesRegex(runner.RunnerError, "account-auth-api-1"):
            runner.ensure_legacy_targets_are_stopped("podman")

    @mock.patch.object(runner.subprocess, "run")
    def test_minimal_project_service_label_is_not_a_legacy_conflict(
            self, subprocess_run) -> None:
        subprocess_run.side_effect = (
            completed("container-id\n"),
            completed("minimal-auth|/account-minimal-auth-external-dev-minimal-auth-1\n"),
        )

        runner.ensure_legacy_targets_are_stopped("podman")

    def test_status_uses_engine_specific_cpu_field(self) -> None:
        for engine, expected, rejected in (
                ("docker", "{{.CPUPerc}}", "{{.CPU}}"),
                ("podman", "{{.CPU}}", "{{.CPUPerc}}")):
            with self.subTest(engine=engine):
                commands: list[list[str]] = []
                with mock.patch.object(runner, "run", side_effect=lambda command: commands.append(command)), \
                        mock.patch.object(
                            runner, "project_container_ids", return_value=["container-id"]):
                    runner.status(engine)
                stats_command = commands[-1]
                rendered = " ".join(stats_command)
                self.assertIn(expected, rendered)
                self.assertNotIn(rejected, rendered)

    def test_smoke_retries_registration_without_printing_response_body(self) -> None:
        env_file = Path("fixture.env")
        effects = [None] * 6 + [runner.RunnerError(), runner.RunnerError(), None]
        with mock.patch.object(
                runner,
                "preflight",
                return_value=(["podman", "compose"], (1, 2, 3, 4, 5))), \
                mock.patch.object(runner, "compose_exec", side_effect=effects) as compose_exec, \
                mock.patch.object(runner.time, "sleep") as sleep:
            with redirect_stdout(io.StringIO()):
                runner.smoke("podman", env_file)

        self.assertEqual(9, compose_exec.call_count)
        self.assertEqual(2, sleep.call_count)
        login_command = compose_exec.call_args_list[-1].args[-1]
        self.assertIn("AbortSignal.timeout(4000)", login_command[-1])

    @mock.patch.object(
        runner.subprocess,
        "run",
        side_effect=subprocess.TimeoutExpired(["fixture"], timeout=15),
    )
    def test_compose_exec_timeout_becomes_bounded_runner_error(self, unused_run) -> None:
        del unused_run
        with self.assertRaisesRegex(runner.RunnerError, "timed out"):
            runner.run(["fixture"], timeout_seconds=15)

    def test_all_stops_only_project_scope_after_smoke_failure(self) -> None:
        env_file = Path("fixture.env")
        with mock.patch.object(runner, "build"), \
                mock.patch.object(runner, "up"), \
                mock.patch.object(
                    runner, "smoke", side_effect=runner.RunnerError("fixture failure")), \
                mock.patch.object(runner, "stop") as stop:
            with self.assertRaisesRegex(runner.RunnerError, "project-scoped stop completed"):
                runner.run_all("podman", env_file)
        stop.assert_called_once_with("podman")


if __name__ == "__main__":
    unittest.main()
