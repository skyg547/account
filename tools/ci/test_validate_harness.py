"""Offline regression fixtures for #672; run with Python's unittest discovery.

Only the validator's explicit content allowlist is copied. History paths get
empty placeholders, and GitHub scripts run against in-memory API doubles.
"""

import copy
import importlib.util
import json
import os
import shutil
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("validate_harness", ROOT / "tools/ci/validate-harness.py")
harness = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(harness)


def contract(relative):
    return harness.read_contract(ROOT, relative)


def workflow():
    return harness.load_yaml(contract(".github/workflows/harness-validation.yml"))


def run_node(script, input_text=""):
    # Hosted runners expose Node through toolcache PATH entries. Resolve that
    # executable before scrubbing the child's environment to avoid credentials.
    executable = shutil.which("node")
    if executable is None:
        raise RuntimeError("Node executable is required for offline workflow tests")
    return subprocess.run([str(Path(executable).absolute()), "-e", script], input=input_text,
        capture_output=True, text=True, timeout=10, env={"PATH": os.defpath})


class FixtureTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for relative in harness.REQUIRED_PATHS:
            target = self.root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            # Never open actual histories, even to prepare test inputs.
            target.write_text(contract(relative) if relative in harness.CONTENT_PATHS else "", encoding="utf-8")

    def test_real_repository_and_isolated_fixture(self):
        expected = {"roles": 11, "skills": 4, "ui_metadata": 3, "workflows": 4, "issue_forms": 2}
        for root in (ROOT, self.root):
            with self.subTest(root="repository" if root == ROOT else "fixture"):
                actual = harness.validate(root)
                self.assertEqual({key: actual[key] for key in expected}, expected)
                self.assertEqual(actual["required_paths"], len(harness.REQUIRED_PATHS))
                self.assertEqual(harness.validate(root), actual)

    def test_missing_required_files(self):
        for relative in (".codex/agents/test.toml", "docs/ai-harness/42-code-documentation-quality.md",
                         ".agents/skills/account-hexagonal-change/references/architecture-boundaries.md",
                         "tools/ci/test_validate_harness.py", "tools/ci/harness-pr-contract.test.cjs"):
            with self.subTest(path=relative):
                target = self.root / relative
                body = target.read_bytes()
                target.unlink()
                with self.assertRaises(harness.ContractError):
                    harness.validate(self.root)
                target.write_bytes(body)

    def test_symlink_leaf_and_ancestor(self):
        target = self.root / "AGENTS.md"
        target.unlink()
        target.symlink_to(ROOT / "AGENTS.md")
        with self.assertRaisesRegex(harness.ContractError, "symlink"):
            harness.read_contract(self.root, "AGENTS.md")
        ancestor = self.root / ".codex"
        ancestor.rename(self.root / "saved-codex")
        ancestor.symlink_to(self.root / "saved-codex", target_is_directory=True)
        with self.assertRaisesRegex(harness.ContractError, "symlink"):
            harness.read_contract(self.root, ".codex/config.toml")

    def test_invalid_utf8_and_oversized_input_are_redacted(self):
        target = self.root / "AGENTS.md"
        for body in (b"fixture-private-marker\xff", b"x" * 512001):
            with self.subTest(length=len(body)):
                target.write_bytes(body)
                with self.assertRaises(harness.ContractError) as caught:
                    harness.read_contract(self.root, "AGENTS.md")
                self.assertNotIn("fixture-private-marker", str(caught.exception))

    def test_unlisted_and_history_contents_are_never_opened(self):
        for relative in (".env", ".codex/config.local.toml", "_backup/secret.md", "docs/history/private.md"):
            target = self.root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(b"synthetic-private-fixture\xff")
        opened = set()
        original_open = Path.open

        def bounded_open(path, *args, **kwargs):
            relative = path.relative_to(self.root).as_posix()
            self.assertIn(relative, harness.CONTENT_PATHS)
            opened.add(relative)
            return original_open(path, *args, **kwargs)

        # Guard the filesystem open boundary, not just the validator helper.
        with patch.object(Path, "open", bounded_open):
            harness.validate(self.root)
            for relative in (*harness.METADATA_ONLY, ".env", "../AGENTS.md"):
                with self.subTest(path=relative), self.assertRaises(harness.ContractError):
                    harness.read_contract(self.root, relative)
        self.assertTrue(opened)
        self.assertFalse(opened.intersection(harness.METADATA_ONLY))

    def test_missing_entry_reference_and_unlisted_skill_link(self):
        target = self.root / "AGENTS.md"
        target.write_text(target.read_text().replace("95-codex-skills-subagents.md", "missing.md"))
        with self.assertRaises(harness.ContractError):
            harness.validate(self.root)
        target.write_text(contract("AGENTS.md"))
        target = self.root / harness.SKILL_FILES[0]
        target.write_text(target.read_text() + "\n[private](../../../../.env)\n")
        with self.assertRaisesRegex(harness.ContractError, "link outside"):
            harness.validate(self.root)

    def test_invalid_project_toml(self):
        target = self.root / ".codex/config.toml"
        for text in ("[agents", "[agents]\nmax_threads=6\nmax_threads=7", "[agents]\nmax_threads='6'",
                     "[agents]\nmax_threads=6\nmax_depth=true\njob_max_runtime_seconds=1800"):
            with self.subTest(text=text):
                target.write_text(text)
                with self.assertRaises(harness.ContractError):
                    harness.validate(self.root)


    def test_unreviewed_role_and_skill_inventory_additions(self):
        for relative in (".codex/agents/unreviewed.toml", ".agents/skills/unreviewed/SKILL.md"):
            with self.subTest(path=relative):
                target = self.root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(b"synthetic unreadable inventory fixture\xff")
                with self.assertRaisesRegex(harness.ContractError, "inventory"):
                    harness.validate(self.root)
                target.unlink()
                if relative.endswith("SKILL.md"):
                    target.parent.rmdir()


class SchemaTests(unittest.TestCase):
    def test_yaml_boolean_keys_and_duplicate_rejection(self):
        parsed = harness.load_yaml("on: {pull_request: null}\nflag: false\nword: yes\n")
        self.assertIn("on", parsed)
        self.assertIs(parsed["flag"], False)
        self.assertEqual(parsed["word"], "yes")
        for text in ("on: [", "on: {}\non: {}", "outer: {key: 1, key: 2}", "- list", "42: value"):
            with self.subTest(text=text), self.assertRaises(harness.ContractError):
                harness.load_yaml(text)

    def test_role_schema_and_permissions(self):
        for name in harness.ROLES:
            text = contract(f".codex/agents/{name}.toml")
            harness.validate_role(name, text)
            sandbox = 'read-only' if name in harness.READ_ONLY_ROLES else 'workspace-write'
            for mutated in (text + '\nname = "duplicate"', text + '\nmodel = "unexpected"',
                            text.replace(f'name = "{name}"', 'name = "other"'),
                            text.replace(f'sandbox_mode = "{sandbox}"', 'sandbox_mode = "danger-full-access"')):
                with self.subTest(role=name), self.assertRaises(harness.ContractError):
                    harness.validate_role(name, mutated)
        base = 'name="test"\ndescription={description}\nmodel_reasoning_effort={effort}\nsandbox_mode="workspace-write"\ndeveloper_instructions="run tests"'
        for description, effort in (('""', '"medium"'), ('12', '"medium"'), ('"test"', '[]'), ('"test"', '"invalid"')):
            with self.subTest(description=description, effort=effort), self.assertRaises(harness.ContractError):
                harness.validate_role("test", base.format(description=description, effort=effort))

    def test_skill_metadata_and_ui_contract(self):
        name = "account-issue-loop"
        text = contract(f".agents/skills/{name}/SKILL.md")
        ui = harness.load_yaml(contract(f".agents/skills/{name}/agents/openai.yaml"))
        harness.validate_skill(name, text, ui)
        for mutated in (text.replace(f"name: {name}", "name: wrong"), "---\nname: incomplete",
                        f"---\nname: {name}\ndescription: ''\n---\nbody",
                        f"---\nname: {name}\ndescription: valid\n---\n"):
            with self.subTest(text=mutated[:40]), self.assertRaises(harness.ContractError):
                harness.validate_skill(name, mutated, ui)
        for section, key, value in (("interface", "default_prompt", "no invocation"),
                                    ("interface", "display_name", ""),
                                    ("policy", "allow_implicit_invocation", False),
                                    ("policy", "allow_implicit_invocation", "true"),
                                    ("policy", "allow_implicit_invocation", 1)):
            changed = copy.deepcopy(ui)
            changed[section][key] = value
            with self.subTest(key=key, value=value), self.assertRaises(harness.ContractError):
                harness.validate_skill(name, text, changed)

    def test_workflow_schema_and_removed_tests(self):
        for mutate in (lambda data: data.update(on=[]), lambda data: data.update(jobs={}),
                       lambda data: data["jobs"]["validate"].update(**{"runs-on": 42}),
                       lambda data: data["jobs"]["validate"]["steps"][0].update(run="true"),
                       lambda data: data["jobs"]["validate"]["steps"].pop(3)):
            data = workflow()
            mutate(data)
            with self.subTest(mutation=mutate), self.assertRaises(harness.ContractError):
                harness.validate_harness_workflow(data)

    def test_workflow_scripts_reject_early_exit_and_masked_failures(self):
        for job, indexes in (("validate", (1, 2, 3, 4)), ("result", (0,))):
            for index in indexes:
                original = workflow()["jobs"][job]["steps"][index]["run"]
                for script in ("exit 0\n" + original, original.rstrip() + " || true\n",
                               "# " + original.replace("\n", "\n# ")):
                    with self.subTest(job=job, step=index, script=script):
                        changed = workflow()
                        changed["jobs"][job]["steps"][index]["run"] = script
                        with self.assertRaises(harness.ContractError):
                            harness.validate_harness_workflow(changed)

    def test_issue_form_duplicate_id_and_invalid_types(self):
        data = harness.load_yaml(contract(harness.ISSUE_FORMS[0]))
        harness.validate_issue_form(data)
        field = next(field for field in data["body"] if field["type"] != "markdown")
        for mutate in (lambda doc: doc["body"].append(copy.deepcopy(field)),
                       lambda doc: doc.update(labels=[]),
                       lambda doc: doc["body"].append({"type": "dropdown", "id": "invalid-option",
                           "attributes": {"label": "Choice", "options": [None]}}),
                       lambda doc: doc["body"].append({"type": "unknown", "attributes": {}}),
                       lambda doc: doc["body"].append({**field, "id": "new", "validations": {"required": "true"}})):
            changed = copy.deepcopy(data)
            mutate(changed)
            with self.subTest(mutation=mutate), self.assertRaises(harness.ContractError):
                harness.validate_issue_form(changed)


class WorkflowExecutionTests(unittest.TestCase):
    def test_tools_only_pr_and_read_only_checkout(self):
        data = workflow()
        self.assertEqual(data["on"], {"pull_request": None, "push": {"branches": ["main"]}, "workflow_dispatch": None})
        self.assertEqual(data["permissions"], {"contents": "read"})
        self.assertEqual(data["jobs"]["validate"]["steps"][0]["with"], {"persist-credentials": False})
        harness.validate_harness_workflow(data)
        for change in (lambda doc: doc["on"].update(pull_request={"paths": ["src/**"]}),
                       lambda doc: doc.update(permissions={"contents": "write"}),
                       lambda doc: doc["jobs"]["validate"]["steps"][0]["with"].update({"persist-credentials": True})):
            mutated = copy.deepcopy(data)
            change(mutated)
            with self.subTest(change=change), self.assertRaises(harness.ContractError):
                harness.validate_harness_workflow(mutated)

    def test_actual_result_script_only_accepts_success(self):
        result = workflow()["jobs"]["result"]
        self.assertEqual(result["if"], "always()")
        self.assertEqual(result["needs"], ["validate"])
        for state in ("success", "failure", "skipped", "cancelled", ""):
            with self.subTest(state=state):
                process = subprocess.run(["bash", "-eu", "-c", result["steps"][0]["run"]],
                    env={"PATH": os.defpath, "VALIDATION_RESULT": state}, capture_output=True, text=True, timeout=10)
                self.assertEqual(process.returncode == 0, state == "success", process.stdout + process.stderr)

    def test_unittest_report_requires_executed_passing_tests(self):
        self.assertEqual(harness.check_test_report("unittest", "Ran 2 tests in 0.01s\n\nOK\n"), 2)
        for report in ("", "Ran 0 tests in 0.0s\n\nOK\n", "Ran 2 tests in 0.0s\n\nFAILED (failures=1)\n",
                       "Ran 2 tests in 0.0s\n\nOK (skipped=1)\n", "Ran 1 test in 0s\nRan 1 test in 0s\nOK\n"):
            with self.subTest(report=report), self.assertRaises(harness.ContractError):
                harness.check_test_report("unittest", report)

    def test_node_report_rejects_zero_failed_skipped_cancelled_todo(self):
        counts = {"tests": 2, "pass": 2, "fail": 0, "cancelled": 0, "skipped": 0, "todo": 0}
        render = lambda data: "\n".join(f"# {key} {value}" for key, value in data.items())
        self.assertEqual(harness.check_test_report("node", render(counts)), 2)
        invalid = [{**counts, "tests": 0, "pass": 0}, {**counts, "pass": 1}]
        invalid.extend({**counts, key: 1} for key in ("fail", "skipped", "cancelled", "todo"))
        for report in ["", render(counts) + "\n# tests 2", *map(render, invalid)]:
            with self.subTest(report=report), self.assertRaises(harness.ContractError):
                harness.check_test_report("node", report)

    def test_test_step_pipelines_reject_empty_reports_and_runner_failures(self):
        steps = workflow()["jobs"]["validate"]["steps"]
        runners = ((steps[3], "/usr/bin/python3 -m unittest discover -s tools/ci -p test_validate_harness.py -v",
                    "Ran 1 test in 0.01s\n\nOK\n", "Ran 0 tests in 0.01s\n\nOK\n"),
                   (steps[4], "node --test tools/ci/harness-pr-contract.test.cjs",
                    "# tests 1\n# pass 1\n# fail 0\n# cancelled 0\n# skipped 0\n# todo 0\n",
                    "# tests 0\n# pass 0\n# fail 0\n# cancelled 0\n# skipped 0\n# todo 0\n"))
        with tempfile.TemporaryDirectory() as directory:
            for step, command, passing, zero in runners:
                # Replace only the runner with a controlled output/exit fixture;
                # execute the real pipefail, tee and report-check commands.
                script = step["run"].replace(command, '(printf "%s" "$FIXTURE_REPORT"; exit "$FIXTURE_EXIT")', 1)
                for report, exit_code, expected in ((passing, 0, True), (passing, 1, False),
                                                     (zero, 0, False), ("", 0, False)):
                    with self.subTest(step=step["name"], report=report, exit_code=exit_code):
                        process = subprocess.run(["bash", "-c", script], cwd=ROOT,
                            env={"PATH": os.defpath, "RUNNER_TEMP": directory,
                                 "FIXTURE_REPORT": report, "FIXTURE_EXIT": str(exit_code)},
                            capture_output=True, text=True, timeout=10)
                        self.assertEqual(process.returncode == 0, expected, process.stdout + process.stderr)

    def test_node_toolcache_path_is_resolved_before_environment_scrub(self):
        executable = shutil.which("node")
        self.assertIsNotNone(executable, "Node executable is required for offline workflow tests")
        with tempfile.TemporaryDirectory() as directory:
            link = Path(directory) / "node"
            link.symlink_to(Path(executable).resolve())
            with patch.dict(os.environ, {"PATH": directory, "HARNESS_SYNTHETIC_SECRET": "fixture"}):
                process = run_node("console.log(JSON.stringify(process.env))")
                self.assertEqual(process.returncode, 0, process.stderr)
                self.assertEqual(process.args[0], str(link))
                self.assertEqual(json.loads(process.stdout), {"PATH": os.defpath})
                link.unlink()
                with self.assertRaisesRegex(RuntimeError, "Node executable is required"):
                    run_node("process.exit(0)")

    def test_actual_merge_guard_lifecycle_and_api_failure(self):
        data = harness.load_yaml(contract(".github/workflows/agent-merge-guard.yml"))
        script = data["jobs"]["guard"]["steps"][0]["with"]["script"]
        # Execute the checked-in script, not a restatement of its policy. No
        # network client or credentials enter the vm; paginate is an API double.
        runner = r'''
const vm = require('node:vm');
let input = '';
process.stdin.on('data', chunk => input += chunk);
process.stdin.on('end', async () => {
  const test = JSON.parse(input);
  const summary = {};
  for (const name of ['addHeading', 'addRaw', 'addList']) summary[name] = () => summary;
  summary.write = async () => {};
  const core = {summary, info() {}, setFailed() { process.exitCode = 1; }};
  const github = {rest: {pulls: {listReviews() {}}}, paginate: async () => {
    if (test.apiError) throw new Error('synthetic API failure');
    return test.reviews;
  }};
  const context = {repo: {owner: 'fixture', repo: 'fixture'}, payload: {pull_request: test.pr}};
  try {
    await vm.runInNewContext('(async () => {' + test.script + '\n})()', {core, github, context}, {timeout: 1000});
  } catch (error) { process.exitCode = 1; }
});
'''
        proof = 'Merge authority: independent reviewer\n```\ntests passed\n```'
        cases = [("draft owner", True, "", [], False, True),
                 ("ready no proof", False, "Merge authority: reviewer", [], False, False),
                 ("ready no authority", False, "```\ntests passed\n```", [], False, False),
                 ("self approval", False, proof, ["author"], False, False),
                 ("valid ready", False, proof, ["reviewer"], False, True),
                 ("API failure", True, "", [], True, False)]
        for label, draft, body, reviewers, api_error, expected in cases:
            payload = {"script": script, "apiError": api_error,
                "pr": {"number": 1, "draft": draft, "body": body, "labels": [{"name": "agent:codex"}], "user": {"login": "author"}},
                "reviews": [{"state": "APPROVED", "user": {"login": user}} for user in reviewers]}
            with self.subTest(case=label):
                process = run_node(runner, json.dumps(payload))
                self.assertEqual(process.returncode == 0, expected, process.stdout + process.stderr)


if __name__ == "__main__":
    unittest.main()
