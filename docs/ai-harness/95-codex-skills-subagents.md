# Codex Skills And Subagents

## Why This Split Exists

Codex automatically loads `AGENTS.md`, so the root file should contain only rules needed for nearly every task. Repeatable procedures live in skills and load only when triggered. Specialized personas live in project custom agent files and run in separate threads with narrow sandbox and ownership rules.

## Repository Skills

Codex discovers repository skills under `.agents/skills/<skill-name>/SKILL.md`.

| Skill | Use it for |
| --- | --- |
| `$account-issue-loop` | Issue, branch, worktree, Draft PR, merge/close/cleanup gates |
| `$account-hexagonal-change` | API/core/batch/domain/persistence implementation or review |
| `$account-review-handoff` | Diff review, verification, shared records, rollback, handoff |

Invoke a skill explicitly when the workflow matters:

```text
Use $account-issue-loop for Issue #4 and create an isolated worktree.
Use $account-hexagonal-change to split this module into api/core/batch.
Use $account-review-handoff before preparing the Draft PR.
```

Codex may also invoke these skills implicitly from their descriptions. Each skill has `agents/openai.yaml` UI metadata.

## Project Custom Agents

Codex discovers project personas under `.codex/agents/*.toml`.

- Read-only: `planner`, `explorer`, `reviewer`, `integrator`.
- Writers: `coder`, `controller`, `service`, `batch`, `sql`, `test`, `documentation`.
- Parent-owned operations: shared logs, conflict edits, commit, push, PR/Issue mutations, merge, and cleanup.

Example prompt:

```text
Use the planner and explorer agents first.
Then delegate controller and service changes with disjoint file allowlists.
Run the test and reviewer agents after implementation.
Wait for required results, then let the parent Integrator summarize and update the shared logs once.
```

## Concurrency Policy

`.codex/config.toml` sets a maximum of six open threads and one child level.

- Prefer parallel read-heavy tasks.
- Parallelize writers only when their file sets do not overlap.
- Do not let a subagent spawn additional subagents.
- Keep one parent responsible for final decisions and external state.

## Persona Contract

Every custom agent file declares `name`, `description`, and `developer_instructions`. Read-only roles use `sandbox_mode = "read-only"`; writing roles use `workspace-write` but remain constrained by the Issue allowlist. Model names are not pinned. Reasoning effort reflects role risk and the actual model is inherited from the parent environment.

## Validation

After changing skills or custom agents:

1. Run the skill creator `quick_validate.py` for each skill when Python is available.
2. Parse each custom agent TOML file.
3. Confirm `AGENTS.md` is the canonical uppercase filename.
4. Check `git diff --check` and conflict markers.
5. Start a new Codex session if discovery appears stale.
6. Forward-test at least one read-only subagent before trusting role routing.

## Compatibility Notes

- Root `SKILL.md` is a legacy pointer; Codex discovery uses `.agents/skills/`.
- `.agent/workflows/feature-dev.md` remains for Antigravity/Cline-compatible workflows but defers to `AGENTS.md` and this harness.
- `.clinerules` is legacy context and must not override current safety, Git, or architecture policy.
