"""Contract tests: fake engines capture publication commands without network access."""

import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools/docker/push-ghcr.sh"
MANIFEST = ROOT / "deploy/image-targets.json"
REGISTRY = "ghcr.io/skyg547/account"
BUSINESS_MODULES = (
    "account-mart", "asset-lease", "budget", "closing", "deposit", "ecl",
    "expenditure-resolution", "journal-ledger", "loan", "master-data",
    "payable", "receivable", "reconciliation", "reporting", "tax",
)
EXPECTED_TARGETS = {
    f"{module}-{kind}" for module in BUSINESS_MODULES for kind in ("api", "batch")
} | {"auth-api", "internal-audit-api", "config-server", "discovery", "gateway", "frontend"}

FAKE_ENGINE = r'''
import hashlib
import json
import os
from pathlib import Path
import sys

args = sys.argv[1:]
with open(os.environ["FAKE_ENGINE_LOG"], "a", encoding="utf-8") as log:
    log.write(json.dumps([Path(sys.argv[0]).name, *args]) + "\n")
operation = "inspect" if args[:2] == ["image", "inspect"] else args[0]
if operation == os.environ.get("FAKE_FAIL_OPERATION") and (
    not os.environ.get("FAKE_FAIL_MATCH") or
    os.environ["FAKE_FAIL_MATCH"] in args[-1]
):
    print("injected engine failure", file=sys.stderr)
    sys.exit(19)
if operation == "inspect":
    if args[:4] != ["image", "inspect", "--format", "{{.Id}}"] or len(args) != 5:
        sys.exit(29)
    print(os.environ.get("FAKE_IMAGE_ID", "sha256:" + hashlib.sha256(args[-1].encode()).hexdigest()))
elif operation not in ("tag", "push"):
    sys.exit(39)
'''


class PushGhcrTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="ghcr-contract-")
        self.addCleanup(self.directory.cleanup)
        self.temp = Path(self.directory.name)
        self.bin = self.temp / "bin"
        self.bin.mkdir()
        self.log = self.temp / "engine.jsonl"
        for engine in ("docker", "podman"):
            executable = self.bin / engine
            executable.write_text(f"#!{sys.executable}\n" + FAKE_ENGINE, encoding="utf-8")
            executable.chmod(0o700)
        self.env = {
            **os.environ,
            "PATH": str(self.bin) + os.pathsep + os.environ.get("PATH", ""),
            "FAKE_ENGINE_LOG": str(self.log),
        }
        for name in ("FAKE_FAIL_OPERATION", "FAKE_FAIL_MATCH", "FAKE_IMAGE_ID"):
            self.env.pop(name, None)

    def invoke(self, *args, script=SCRIPT, **env):
        self.log.unlink(missing_ok=True)
        return subprocess.run(
            ["bash", str(script), *args], cwd=self.temp,
            env={**self.env, **env}, text=True, capture_output=True, timeout=15,
        )

    def calls(self):
        if not self.log.exists():
            return []
        return [json.loads(line) for line in self.log.read_text().splitlines()]

    def assert_success(self, result):
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def assert_rejected_without_engine(self, result):
        self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.calls(), [])

    def copied_script(self, manifest):
        root = self.temp / "copy"
        (root / "tools/docker").mkdir(parents=True, exist_ok=True)
        (root / "deploy").mkdir(exist_ok=True)
        launcher = root / "tools/docker/push-ghcr.sh"
        shutil.copyfile(SCRIPT, launcher)
        (root / "deploy/image-targets.json").write_text(
            json.dumps(manifest), encoding="utf-8"
        )
        return launcher

    def test_default_dry_run_maps_all_36_targets_from_arbitrary_directory(self):
        result = self.invoke("--tag", "v1", "--dry-run")
        self.assert_success(result)
        mappings = [line.strip() for line in result.stdout.splitlines() if " -> " in line]
        self.assertEqual(len(mappings), 36)
        self.assertEqual(set(mappings), {
            f"account/{name}:local -> {REGISTRY}/{name}:v1" for name in EXPECTED_TARGETS
        })
        self.assertEqual(self.calls(), [])

    def test_selected_dry_run_custom_source(self):
        result = self.invoke(
            "--tag", "release_1.2-rc", "--source-prefix", "localhost:5000/team/account",
            "--source-tag", "candidate", "--target", "gateway", "--target", "frontend",
            "--engine", "podman", "--dry-run",
        )
        self.assert_success(result)
        mappings = [line.strip() for line in result.stdout.splitlines() if " -> " in line]
        self.assertEqual(set(mappings), {
            f"localhost:5000/team/account/{name}:candidate -> {REGISTRY}/{name}:release_1.2-rc"
            for name in ("gateway", "frontend")
        })
        self.assertEqual(self.calls(), [])

    def test_tag_boundaries(self):
        for tag in ("x", "_", "A" * 128, "v1.2_rc-4"):
            with self.subTest(tag=tag):
                self.assert_success(self.invoke("--tag", tag, "--dry-run"))
                self.assertEqual(self.calls(), [])
        for tag in ("", "x" * 129, "-bad", ".bad", "v1/2", "v1:2", "v 1", "한글", "v1\n"):
            with self.subTest(tag=repr(tag)):
                self.assert_rejected_without_engine(self.invoke("--tag", tag))

    def test_invalid_arguments_never_invoke_engine(self):
        cases = (
            (), ("--tag",), ("--tag", "v1", "--engine", "nerdctl"),
            ("--tag", "v1", "--unknown"), ("--tag", "v1", "extra"),
            ("--tag", "v1", "--target", "unknown"),
            ("--tag", "v1", "--target", "gateway", "--target", "gateway"),
            ("--tag", "v1", "--source-tag", "bad/tag"),
        )
        for args in cases:
            with self.subTest(args=args):
                self.assert_rejected_without_engine(self.invoke(*args))

    def test_live_engine_choice_preflight_and_pinned_image_ids(self):
        for engine in ("docker", "podman"):
            with self.subTest(engine=engine):
                result = self.invoke(
                    "--tag", "v1", "--engine", engine,
                    "--target", "config-server", "--target", "gateway",
                )
                self.assert_success(result)
                expected = [
                    [engine, "image", "inspect", "--format", "{{.Id}}", f"account/{name}:local"]
                    for name in ("config-server", "gateway")
                ]
                for name in ("config-server", "gateway"):
                    image_id = "sha256:" + hashlib.sha256(f"account/{name}:local".encode()).hexdigest()
                    dest = f"{REGISTRY}/{name}:v1"
                    expected += [[engine, "tag", image_id, dest], [engine, "push", dest]]
                self.assertEqual(self.calls(), expected)

    def test_all_targets_live_have_preflight_before_any_publication(self):
        result = self.invoke("--tag", "v1")
        self.assert_success(result)
        calls = self.calls()
        self.assertEqual(len(calls), 108)
        self.assertTrue(all(call[1:3] == ["image", "inspect"] for call in calls[:36]))
        self.assertEqual({call[-1] for call in calls[:36]}, {
            f"account/{name}:local" for name in EXPECTED_TARGETS
        })
        self.assertEqual({call[-1] for call in calls if call[1] == "push"}, {
            f"{REGISTRY}/{name}:v1" for name in EXPECTED_TARGETS
        })

    def test_missing_later_image_prevents_all_tagging_and_pushes(self):
        result = self.invoke(
            "--tag", "v1", "--target", "config-server", "--target", "gateway",
            FAKE_FAIL_OPERATION="inspect", FAKE_FAIL_MATCH="gateway",
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(len(self.calls()), 2)
        self.assertTrue(all(call[1:3] == ["image", "inspect"] for call in self.calls()))

    def test_tag_or_push_failure_stops_later_targets(self):
        for operation, expected_count in (("tag", 3), ("push", 4)):
            with self.subTest(operation=operation):
                result = self.invoke(
                    "--tag", "v1", "--target", "config-server", "--target", "gateway",
                    FAKE_FAIL_OPERATION=operation, FAKE_FAIL_MATCH="config-server",
                )
                self.assertNotEqual(result.returncode, 0)
                calls = self.calls()
                self.assertEqual(len(calls), expected_count)
                self.assertEqual(calls[-1][1], operation)
                self.assertTrue(all("gateway" not in call[-1] for call in calls[2:]))

    def test_invalid_inspect_output_prevents_publication(self):
        for image_id in ("", "sha256:abc", "account/gateway:local", "a" * 64 + "\n" + "b" * 64):
            with self.subTest(image_id=image_id):
                result = self.invoke(
                    "--tag", "v1", "--target", "gateway", FAKE_IMAGE_ID=image_id,
                )
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(len(self.calls()), 1)
                self.assertEqual(self.calls()[0][1:3], ["image", "inspect"])

    def test_partial_push_failure_reports_count_and_same_release_can_be_retried(self):
        args = ("--tag", "v1", "--target", "config-server", "--target", "gateway")
        result = self.invoke(*args, FAKE_FAIL_OPERATION="push", FAKE_FAIL_MATCH="gateway")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("pushed 1/2", result.stderr)
        failed_calls = self.calls()
        result = self.invoke(*args)
        self.assert_success(result)
        self.assertEqual(self.calls(), failed_calls)

    def test_disabled_target_is_omitted_and_cannot_be_selected(self):
        manifest = json.loads(MANIFEST.read_text())
        next(target for target in manifest["targets"] if target["name"] == "gateway")["enabled"] = False
        launcher = self.copied_script(manifest)
        result = self.invoke("--tag", "v1", "--dry-run", script=launcher)
        self.assert_success(result)
        self.assertNotIn("account/gateway:", result.stdout)
        self.assertEqual(sum(" -> " in line for line in result.stdout.splitlines()), 35)
        self.assert_rejected_without_engine(self.invoke(
            "--tag", "v1", "--target", "gateway", script=launcher
        ))

    def test_corrupt_manifest_fails_before_engine_calls(self):
        baseline = json.loads(MANIFEST.read_text())
        corruptions = (
            {**baseline, "schemaVersion": 999},
            {**baseline, "targets": baseline["targets"] + [baseline["targets"][0]]},
            {**baseline, "targets": [{"name": "../other", "enabled": True}]},
            {**baseline, "targets": [{"name": "gateway", "enabled": "true"}]},
        )
        for index, manifest in enumerate(corruptions):
            with self.subTest(index=index):
                self.assert_rejected_without_engine(self.invoke(
                    "--tag", "v1", script=self.copied_script(manifest)
                ))


if __name__ == "__main__":
    unittest.main()
