# Codex Skills And Subagents

## Why This Split Exists

Codex automatically loads `AGENTS.md`, so the root file should contain only rules needed for nearly every task. Repeatable procedures live in skills and load only when triggered. Specialized personas live in project custom agent files and run in separate threads with narrow sandbox and ownership rules.

## Repository Skills

Codex discovers repository skills under `.agents/skills/<skill-name>/SKILL.md`.

| Skill | Use it for |
| --- | --- |
| `$account-issue-loop` | Issue, branch, worktree, Draft PR, merge/close/cleanup gates |
| `$account-hexagonal-change` | API/core/batch/domain/persistence implementation or review |
| `$account-module-parallel` | Parallel execution across multiple MSA microservice modules with disjoint allowlists |
| `$account-review-handoff` | Diff review, verification, shared records, rollback, handoff |

Invoke a skill explicitly when the workflow matters:

```text
Use $account-issue-loop for Issue #4 and create an isolated worktree.
Use $account-hexagonal-change to split this module into api/core/batch.
Use $account-module-parallel to implement changes across journal-ledger and deposit in parallel.
Use $account-review-handoff before preparing the Draft PR.
```

Codex may also invoke these skills implicitly from their descriptions. `account-issue-loop`, `account-hexagonal-change`, and `account-review-handoff` have `agents/openai.yaml` UI metadata. `account-module-parallel` currently has only `SKILL.md`; its missing UI metadata is an explicit inventory exception, not a missing-file success rule for the other three skills.

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
- Multi-module MSA changes should be parallelized across microservices using `$account-module-parallel` with disjoint module allowlists (`<module>/**`).
- Parallelize writers only when their file sets do not overlap.
- Do not let a subagent spawn additional subagents.
- Keep one parent responsible for final decisions and external state.

## Persona Contract

Every custom agent file declares `name`, `description`, and `developer_instructions`. Read-only roles use `sandbox_mode = "read-only"`; writing roles use `workspace-write` but remain constrained by the Issue allowlist. Model names are not pinned. Reasoning effort reflects role risk and the actual model is inherited from the parent environment.

## Validation

From the repository root, use Python 3.11+ with `tomllib`, existing PyYAML 6.0.1, and Node.js 22. CI uses `ubuntu-24.04`'s system `/usr/bin/python3`, not a downloaded Python toolchain. The runtime step prints versions and fails if the YAML parser is absent or differs; there is no package-install fallback. A runtime change requires a reviewed update, not an automatic dependency download.

```bash
/usr/bin/python3 tools/ci/validate-harness.py
/usr/bin/python3 -m unittest discover -s tools/ci -p test_validate_harness.py -v
node --test tools/ci/harness-pr-contract.test.cjs
git diff --check
```

Expected: schema category counts, a nonzero Python test count with `OK`, Node `# tests`/`# pass` counts equal and failures/cancellations/skips/todo all zero, and exit code 0 for each command. The workflow also checks the captured test summaries with `--test-report unittest <log>` and `--test-report node <tap>` so zero tests or skipped tests cannot certify success. Error messages omit source contents and parser snippets.

초보자 설명: 업무 코드 시험은 작업 지시서가 맞는지 검사하지 않는다. 이 명령은 역할 이름·권한·문법과 필요한 문서 주소를 확인하고, 일부러 잘못 만든 예제가 실패하는지도 시험한다. 파일을 못 읽거나 시험을 한 건도 실행하지 못했다면 통과가 아니다.

### Fixed input contract

`tools/ci/validate-harness.py` declares the complete `CONTENT_PATHS` and `METADATA_ONLY` inventories. It does not recursively read the repository or follow links to arbitrary targets.

| Input | Check |
| --- | --- |
| `.codex/config.toml` | TOML syntax, only the existing agent limits (6 threads, depth 1, 1800 seconds); other configuration fields rejected |
| `.codex/agents/{planner,explorer,reviewer,integrator,coder,controller,service,batch,sql,test,documentation}.toml` | Exact inventory, name/filename agreement, required nonempty fields, reasoning enum, read-only versus workspace-write role boundary, no pinned model or extra fields |
| Four listed `.agents/skills/*/SKILL.md` and the three UI files above | YAML frontmatter, name/directory agreement, nonempty description/body, interface fields, matching skill invocation and boolean policy |
| Four named workflow YAML files and two current Issue forms | Safe YAML syntax, duplicate-key rejection, literal `on` key, required jobs/steps and form fields; additional event/permission/execution/result contract for the new harness workflow |
| `AGENTS.md`, the fixed 18 active harness guides, architecture boundary reference and CI scripts | Required regular paths, root canonical document references and local Markdown links in root/skill instructions |
| `GEMINI.md`, `GEMINI_REVIEW_PROMPT.md`, `docs/WORKLOG.md`, `docs/history/CODEX_WORKLOG.md` and harness worklog/status/handoff | Existence/type metadata only; bodies are not read by the validator |

Unlisted paths, `.env*`, credentials, personal/local settings, `.git`, historical archives, `_backup` and operational log bodies are excluded. Symlinks in required paths or their ancestors are rejected before content reads. New persona/skill inventory entries require an explicit validator update; arbitrary unlisted content is never parsed to discover its purpose. Skill link targets are normalized and checked against the fixed list before metadata access; external URLs and anchors are not fetched or validated. File size and UTF-8 errors fail the check.

The Python fixtures cover valid, missing, malformed and boundary inputs, actual result-shell outcomes, and the existing Guard JavaScript executed with synthetic PR/review/API objects and no network. The Node command runs #662's PR lifecycle regression tests. Other local Node suites (quality, review scope, Gemini template) remain outside this CI job; their existing manual commands and limits still apply.

### Limits and follow-up

This is a repository-specific schema subset, not the complete upstream Codex/GitHub schema or an AI permission sandbox. It does not prove that free-text instructions are semantically correct, validate every Markdown link/anchor, or enforce a GitHub merge rule. Guard mocks exercise current behavior; an ownerless PR or a PR without independent approval may still produce a notice rather than a failure under the existing Guard. Its trust model is unchanged.

The fixed **Harness Validation Result** and failure behavior are described in [CI Module Validation](45-ci-module-validation.md). After changing discovery metadata, start a fresh session if needed and forward-test a read-only subagent. Independent review, current head/base evidence and separately approved Ready/merge/close/cleanup remain required.

## Compatibility Notes

- Codex discovers repository skills in `.agents/skills/<skill-name>/SKILL.md`; no root `SKILL.md` is required.
- `.agent/workflows/feature-dev.md` remains for Antigravity/Cline-compatible workflows but defers to `AGENTS.md` and this harness.
- `.clinerules` is legacy context and must not override current safety, Git, or architecture policy.
