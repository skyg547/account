#!/usr/bin/env python3
"""Bounded repository contracts, not a general Codex or GitHub schema validator.

Read only CONTENT_PATHS; required history paths are checked as metadata only.
See docs/ai-harness/95-codex-skills-subagents.md for scope and limitations.
"""

import argparse
import posixpath
import re
import stat
import sys
import tomllib
from pathlib import Path

import yaml


READ_ONLY_ROLES = ("planner", "explorer", "reviewer", "integrator")
WRITE_ROLES = ("coder", "controller", "service", "batch", "sql", "test", "documentation")
ROLES = READ_ONLY_ROLES + WRITE_ROLES
SKILLS = ("account-issue-loop", "account-hexagonal-change",
          "account-module-parallel", "account-review-handoff")
UI_SKILLS = tuple(name for name in SKILLS if name != "account-module-parallel")
DOCS = tuple("docs/ai-harness/" + name + ".md" for name in (
    "00-overview", "10-rules", "20-workflow", "25-issue-claim-verification",
    "30-agents", "40-test-checklist", "42-code-documentation-quality",
    "45-ci-module-validation", "50-git-worktree-guide", "60-rebase-merge-policy",
    "70-model-assignment-policy", "80-file-ownership", "85-github-issue-agent-loop",
    "86-multi-tool-issue-ownership", "87-spec-driven-delegation",
    "88-pr-review-and-merge-runbook", "90-beginner-ai-agent-git-guide",
    "95-codex-skills-subagents"))
WORKFLOWS = tuple(".github/workflows/" + name + ".yml" for name in (
    "harness-validation", "agent-merge-guard", "module-validation", "reporting-validation"))
ISSUE_FORMS = tuple(".github/ISSUE_TEMPLATE/" + name + ".yml" for name in (
    "ai-agent-loop-task", "agent-implementation-spec"))
SKILL_FILES = tuple(f".agents/skills/{name}/SKILL.md" for name in SKILLS)
UI_FILES = tuple(f".agents/skills/{name}/agents/openai.yaml" for name in UI_SKILLS)
ROLE_FILES = tuple(f".codex/agents/{name}.toml" for name in ROLES)
NODE_INPUTS = ("tools/ci/harness-pr-contract.test.cjs",) + tuple(
    f"docs/ai-harness/{name}.md" for name in (
        "20-workflow", "87-spec-driven-delegation", "88-pr-review-and-merge-runbook")) + (
    ".github/ISSUE_TEMPLATE/agent-implementation-spec.yml",)
CONTENT_PATHS = frozenset(("AGENTS.md", ".codex/config.toml",
    ".agents/skills/account-hexagonal-change/references/architecture-boundaries.md",
    "tools/ci/validate-harness.py", "tools/ci/test_validate_harness.py")
    + DOCS + WORKFLOWS + ISSUE_FORMS + SKILL_FILES + UI_FILES + ROLE_FILES + NODE_INPUTS)
METADATA_ONLY = ("GEMINI.md", "GEMINI_REVIEW_PROMPT.md", "docs/WORKLOG.md",
    "docs/history/CODEX_WORKLOG.md", "docs/ai-harness/worklog.md",
    "docs/ai-harness/agent-status.md", "docs/ai-harness/handoff.md")
REQUIRED_PATHS = CONTENT_PATHS | frozenset(METADATA_ONLY)
# No recursive glob/read: histories, _backup, .env*, personal/local settings,
# .git, symlinks and every other unlisted file are excluded by construction.
RESULT_SCRIPT = '''if [ "$VALIDATION_RESULT" != "success" ]; then
  echo "::error::Harness validation did not succeed ($VALIDATION_RESULT)."
  exit 1
fi
echo "Harness schema and Python/Node contract tests passed."
'''
RUNTIME_SCRIPT = '''/usr/bin/python3 -c 'import sys, tomllib, yaml; assert yaml.__version__ == "6.0.1"; print("Python", sys.version.split()[0], "PyYAML", yaml.__version__)'
node --version
'''


class ContractError(ValueError):
    """A redacted diagnostic: never include input contents or parser snippets."""


def require(condition, message):
    if not condition:
        raise ContractError(message)


class ContractLoader(yaml.SafeLoader):
    """Safe YAML with true/false booleans and no silently overwritten keys."""


# YAML 1.1's yes/no/on/off conversion breaks the GitHub Actions `on` key.
ContractLoader.yaml_implicit_resolvers = {
    key: [(tag, pattern) for tag, pattern in rules if tag != "tag:yaml.org,2002:bool"]
    for key, rules in yaml.SafeLoader.yaml_implicit_resolvers.items()
}
ContractLoader.add_implicit_resolver("tag:yaml.org,2002:bool",
    re.compile(r"^(?:true|false)$", re.IGNORECASE), list("tTfF"))


def unique_mapping(loader, node):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=True)
        require(isinstance(key, str), "YAML mapping keys must be strings")
        require(key not in result, "duplicate YAML mapping key")
        result[key] = loader.construct_object(value_node, deep=True)
    return result


ContractLoader.add_constructor("tag:yaml.org,2002:map", unique_mapping)


def load_yaml(text):
    try:
        result = yaml.load(text, Loader=ContractLoader)
    except yaml.YAMLError:
        raise ContractError("invalid YAML syntax") from None
    require(isinstance(result, dict), "YAML document must be a mapping")
    return result


def checked_path(root, relative):
    require(relative in REQUIRED_PATHS, "path outside fixed contract scope")
    path = root
    try:
        for part in Path(relative).parts:
            path = path / part
            require(not path.is_symlink(), f"{relative}: symlink is forbidden")
        require(stat.S_ISREG(path.stat().st_mode), f"{relative}: expected regular file")
    except OSError:
        raise ContractError(f"{relative}: missing or inaccessible required file") from None
    return path


def read_contract(root, relative):
    require(relative in CONTENT_PATHS, "content outside fixed contract scope")
    path = checked_path(root, relative)
    try:
        require(path.stat().st_size <= 512_000, f"{relative}: contract file too large")
        return path.read_text(encoding="utf-8")
    except (OSError, UnicodeError):
        raise ContractError(f"{relative}: unreadable UTF-8 contract") from None


def nonempty_string(value):
    return isinstance(value, str) and bool(value.strip())


def validate_role(name, text):
    try:
        data = tomllib.loads(text)
    except tomllib.TOMLDecodeError:
        raise ContractError("invalid agent TOML syntax") from None
    require(set(data) == {"name", "description", "model_reasoning_effort",
                         "sandbox_mode", "developer_instructions"}, "unexpected agent fields")
    require(data["name"] == name, "agent name must match filename")
    for key in ("description", "developer_instructions"):
        require(nonempty_string(data[key]), f"agent {key} must be nonempty text")
    require(data["model_reasoning_effort"] in ("low", "medium", "high", "xhigh"),
            "invalid agent reasoning effort")
    mode = "read-only" if name in READ_ONLY_ROLES else "workspace-write"
    require(data["sandbox_mode"] == mode, "agent sandbox violates role contract")


def validate_skill(name, text, ui=None):
    frontmatter = re.match(r"\A---\r?\n(.*?)\r?\n---(?:\r?\n|$)", text, re.S)
    require(frontmatter is not None, "skill requires closed YAML frontmatter")
    data = load_yaml(frontmatter[1])
    require(set(data) == {"name", "description"}, "unexpected skill metadata fields")
    require(data["name"] == name, "skill name must match directory")
    require(nonempty_string(data["description"]), "skill description must be nonempty text")
    require(nonempty_string(text[frontmatter.end():]), "skill body must be nonempty")
    if name in UI_SKILLS:
        require(isinstance(ui, dict) and set(ui) == {"interface", "policy"}, "missing UI metadata")
        interface = ui["interface"]
        require(isinstance(interface, dict) and set(interface) == {
            "display_name", "short_description", "default_prompt"}, "invalid UI interface fields")
        require(all(nonempty_string(value) for value in interface.values()), "invalid UI text")
        require(f"${name}" in interface["default_prompt"], "UI prompt must invoke its skill")
        require(isinstance(ui["policy"], dict) and set(ui["policy"]) == {"allow_implicit_invocation"}
                and ui["policy"]["allow_implicit_invocation"] is True, "invalid UI invocation policy")


def validate_workflow_schema(data):
    require(nonempty_string(data.get("name")), "workflow name required")
    require(isinstance(data.get("on"), dict) and data["on"], "workflow events required")
    jobs = data.get("jobs")
    require(isinstance(jobs, dict) and jobs, "workflow jobs required")
    for job in jobs.values():
        require(isinstance(job, dict) and nonempty_string(job.get("runs-on")), "invalid job runner")
        steps = job.get("steps")
        require(isinstance(steps, list) and steps, "job steps required")
        for step in steps:
            require(isinstance(step, dict) and (("uses" in step) != ("run" in step)),
                    "step requires exactly one of uses/run")
            require(nonempty_string(step.get("uses", step.get("run"))), "empty workflow step")


def validate_harness_workflow(data):
    validate_workflow_schema(data)
    require(data["on"] == {"pull_request": None, "push": {"branches": ["main"]},
                           "workflow_dispatch": None}, "harness must run unfiltered PR/main/manual events")
    require(data.get("permissions") == {"contents": "read"}, "harness requires read-only contents")
    require(set(data["jobs"]) == {"validate", "result"}, "harness jobs must be validate/result")
    for job in data["jobs"].values():
        require(not ({"permissions", "continue-on-error", "strategy", "environment"} & set(job)),
                "harness job must not override permissions or mask failure")
        require(job["runs-on"] == "ubuntu-24.04", "harness requires existing Ubuntu 24.04 runtimes")
        for step in job["steps"]:
            require(not ({"if", "continue-on-error"} & set(step)), "harness steps must execute and fail closed")
    validation = data["jobs"]["validate"]
    require("if" not in validation and "needs" not in validation, "validation must always be scheduled")
    steps = validation["steps"]
    require(len(steps) == 5, "harness requires checkout/runtime/schema/Python/Node steps")
    require(steps[0].get("uses") == "actions/checkout@v4" and
            steps[0].get("with") == {"persist-credentials": False}, "checkout must not persist credentials")
    require(steps[1].get("run") == RUNTIME_SCRIPT, "existing parser version check required")
    require(steps[2].get("run") == "/usr/bin/python3 tools/ci/validate-harness.py", "schema command required")
    for step, command, kind, logfile in (
        (steps[3], "/usr/bin/python3 -m unittest discover -s tools/ci -p test_validate_harness.py -v 2>&1", "unittest", "harness-python.log"),
        (steps[4], "node --test tools/ci/harness-pr-contract.test.cjs", "node", "harness-node.tap")):
        # Exact small scripts reject early exit/|| true/commented commands;
        # mere substring checks could certify a test that was never executed.
        expected = (f'set -euo pipefail\n{command} | tee "$RUNNER_TEMP/{logfile}"\n'
                    f'/usr/bin/python3 tools/ci/validate-harness.py --test-report {kind} "$RUNNER_TEMP/{logfile}"\n')
        require(step.get("shell") == "bash" and step.get("run") == expected,
                "explicit test execution, pipefail and count check required")
    result = data["jobs"]["result"]
    require(result.get("name") == "Harness Validation Result" and result.get("needs") == ["validate"]
            and result.get("if") == "always()", "fixed result must always depend on validation")
    require(len(result["steps"]) == 1 and result["steps"][0].get("env") == {
        "VALIDATION_RESULT": "${{ needs.validate.result }}"}, "result must use actual dependency outcome")
    require(result["steps"][0].get("run") == RESULT_SCRIPT,
            "result must reject failure, cancellation and skipped validation")


def validate_issue_form(data):
    require(all(nonempty_string(data.get(key)) for key in ("name", "description", "title")),
            "issue form identity required")
    require(isinstance(data.get("labels"), list) and "status:draft" in data["labels"],
            "issue form must start in draft")
    body = data.get("body")
    require(isinstance(body, list) and body, "issue form body required")
    ids = set()
    for field in body:
        require(isinstance(field, dict) and field.get("type") in (
            "markdown", "input", "textarea", "dropdown", "checkboxes"), "invalid issue form field")
        attrs = field.get("attributes")
        require(isinstance(attrs, dict), "issue form attributes required")
        if field["type"] == "markdown":
            require(nonempty_string(attrs.get("value")), "markdown field value required")
            continue
        identity = field.get("id")
        require(nonempty_string(identity) and identity not in ids, "issue field IDs must be unique")
        ids.add(identity)
        require(nonempty_string(attrs.get("label")), "issue field label required")
        if field["type"] in ("dropdown", "checkboxes"):
            require(isinstance(attrs.get("options"), list) and attrs["options"], "field options required")
            for option in attrs["options"]:
                if field["type"] == "dropdown":
                    require(nonempty_string(option), "dropdown option must be nonempty text")
                else:
                    require(isinstance(option, dict) and nonempty_string(option.get("label")),
                            "checkbox option requires label")
                    require("required" not in option or type(option["required"]) is bool,
                            "checkbox required must be boolean")
        if "validations" in field:
            rules = field["validations"]
            require(isinstance(rules, dict) and type(rules.get("required")) is bool,
                    "issue required validation must be boolean")


def validate_links(root, source, text):
    # Skill links are a small, explicit set. Never open an arbitrary link target.
    for target in re.findall(r"\[[^\]]+\]\(([^)]+)\)", text):
        if re.match(r"https?://|#", target):
            continue
        relative = posixpath.normpath(posixpath.join(posixpath.dirname(source), target.split("#")[0]))
        require(relative in REQUIRED_PATHS, f"{source}: link outside required path contract")
        checked_path(root, relative)


def validate(root):
    root = Path(root)
    for relative in sorted(REQUIRED_PATHS):
        checked_path(root, relative)
    # Enumerate names only; new personas/skills need an explicit reviewed schema
    # extension instead of silently living outside the gate's fixed read list.
    require({p.name for p in (root / ".codex/agents").iterdir() if p.suffix == ".toml"}
            == {f"{name}.toml" for name in ROLES}, "agent inventory differs from fixed contract")
    require({p.name for p in (root / ".agents/skills").iterdir() if p.is_dir() or p.is_symlink()}
            == set(SKILLS), "skill inventory differs from fixed contract")
    try:
        config = tomllib.loads(read_contract(root, ".codex/config.toml"))
    except tomllib.TOMLDecodeError:
        raise ContractError("invalid project TOML syntax") from None
    require(config == {"agents": {"max_threads": 6, "max_depth": 1, "job_max_runtime_seconds": 1800}},
            "unexpected project agent limits/fields")
    require(all(type(value) is int for value in config["agents"].values()), "agent limits must be integers")
    for role, relative in zip(ROLES, ROLE_FILES):
        validate_role(role, read_contract(root, relative))
    for name, relative in zip(SKILLS, SKILL_FILES):
        ui = load_yaml(read_contract(root, f".agents/skills/{name}/agents/openai.yaml")) if name in UI_SKILLS else None
        text = read_contract(root, relative)
        validate_skill(name, text, ui)
        validate_links(root, relative, text)
    for relative in WORKFLOWS:
        data = load_yaml(read_contract(root, relative))
        validate_workflow_schema(data)
        if relative == WORKFLOWS[0]:
            validate_harness_workflow(data)
    for relative in ISSUE_FORMS:
        validate_issue_form(load_yaml(read_contract(root, relative)))
    agents = read_contract(root, "AGENTS.md")
    for relative in DOCS:
        # Only enforce root entry points actually advertised by the root guide.
        if relative.split("/")[-1][:2] in {"10", "20", "30", "42", "50", "60", "80", "85", "95", "90"}:
            filename = posixpath.basename(relative)
            # Root entries abbreviate later filenames on a line after one full
            # docs/ai-harness path. Preserve that established grouped notation.
            grouped = any(re.search(r"`docs/ai-harness/[^`]+`.*`" + re.escape(filename) + "`", line)
                          for line in agents.splitlines())
            require(relative in agents or grouped, f"AGENTS.md: missing required reference {relative}")
    validate_links(root, "AGENTS.md", agents)
    return {"roles": len(ROLES), "skills": len(SKILLS), "ui_metadata": len(UI_FILES),
            "workflows": len(WORKFLOWS), "issue_forms": len(ISSUE_FORMS), "required_paths": len(REQUIRED_PATHS)}


def check_test_report(kind, text):
    """Exit zero from a test runner alone does not prove any tests executed."""
    if kind == "unittest":
        counts = re.findall(r"^Ran (\d+) tests? in .+$", text, re.M)
        require(len(counts) == 1 and int(counts[0]) > 0 and
                re.search(r"^OK\s*\Z", text, re.M) is not None,
                "unittest report must contain executed tests and OK without skips")
        return int(counts[0])
    counters = {}
    for key in ("tests", "pass", "fail", "cancelled", "skipped", "todo"):
        values = re.findall(rf"^# {key} (\d+)$", text, re.M)
        require(len(values) == 1, "Node report missing or duplicate counters")
        counters[key] = int(values[0])
    require(counters["tests"] > 0 and counters["pass"] == counters["tests"] and
            all(counters[key] == 0 for key in ("fail", "cancelled", "skipped", "todo")),
            "Node report must contain passing executed tests without skips")
    return counters["tests"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--test-report", nargs=2, metavar=("KIND", "PATH"))
    args = parser.parse_args()
    try:
        if args.test_report:
            kind, path = args.test_report
            require(kind in ("unittest", "node"), "unknown test report kind")
            count = check_test_report(kind, Path(path).read_text(encoding="utf-8"))
            print(f"PASS: {kind} executed {count} tests, no failures or skips")
        else:
            counts = validate(Path(__file__).resolve().parents[2])
            print("PASS: harness schema " + ", ".join(f"{key}={value}" for key, value in counts.items()))
    except (ContractError, OSError, UnicodeError) as error:
        print(f"FAIL: {error if isinstance(error, ContractError) else 'cannot read test report'}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
