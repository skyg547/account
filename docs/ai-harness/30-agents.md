# Agent Roles

Project-scoped Codex custom agents live in `.codex/agents/*.toml`. Every writer requires an explicit file allowlist. The parent Integrator owns shared logs and Git/GitHub mutations.

| Role | Custom agent | Primary responsibility | Default write mode |
| --- | --- | --- | --- |
| Planner | `planner` | Requirements, scope, acceptance criteria, work split | Read-only |
| Explorer | `explorer` | Code search, candidate files, call/dependency map | Read-only |
| Coder | `coder` | Small approved implementation not covered by a narrower owner | Allowlist only |
| Controller | `controller` | Controllers, mappings, inbound DTO wiring | Inbound API files |
| Service | `service` | Use cases, pipelines, ports, domain collaboration | Core files |
| Batch | `batch` | Trigger, Job/Step flow, partitioning, executor, chunk, restart parameters | Batch files |
| SQL | `sql` | Persistence adapters, queries, mapper XML, migrations, bulk performance | Persistence files |
| Test | `test` | Automated tests, fixtures, verification procedures | Test files |
| Reviewer | `reviewer` | Correctness, security, performance, architecture, regression review | Read-only |
| Integrator | `integrator` | Integration analysis, semantic conflict and PR recommendation | Read-only advisory |
| Documentation | `documentation` | Module docs, API docs, runbooks, beginner guidance | Approved docs |
| Module Writer | `coder`/`self` | Target microservice module implementation in parallel ($account-module-parallel) | Module allowlist (`<module>/**`) |

## Parent Integrator

The main agent, not an autonomous writer subagent, performs these operations:

- collect and reconcile subagent results;
- decide final file ownership and semantic conflict resolution;
- update `worklog.md`, `agent-status.md`, `conflict-log.md`, `integration-log.md`, and `handoff.md`;
- stage and commit approved files;
- push, create or change PR state, merge, close Issues, and clean resources only at the approved gate.

This single-writer rule prevents parallel agents from racing on shared records or Git state.

## Delegation Rules

- Parallelize independent read-heavy exploration, review, and test analysis.
- Multi-module MSA changes can be parallelized across distinct modules with disjoint module allowlists (`<module-name>/**`).
- Give write agents disjoint file sets.
- Do not ask a child agent to spawn more agents; `.codex/config.toml` keeps `max_depth = 1`.
- Do not pin vendor model names in repository role files. Select capacity by task risk using `70-model-assignment-policy.md`.
- Treat all role boundaries as defaults; the Issue allowlist may narrow them but may not silently broaden them.
