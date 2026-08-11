# File Ownership

This policy maps custom agent roles to default edit areas. The task or Issue must provide a narrower explicit allowlist before any writer edits files.

| Role | Default editable area | Prohibited or escalation area |
| --- | --- | --- |
| Planner | None | All code, tests, docs, logs, Git/GitHub state |
| Explorer | None | All edits and Git/GitHub state |
| Coder | Explicit allowlist only | Any unlisted file and shared harness logs |
| Controller | `**/*Controller.java`, `**/adapter/in/web/**`, approved API DTOs | Domain rules, persistence, batch |
| Service | Approved `**/application/service/**`, `**/application/pipeline/**`, ports, domain, core tests | Controller, batch, persistence unless reassigned |
| Batch | Approved `**/batch/**` Job/Step/trigger/config/test files | Business formulas and state transitions |
| SQL | Persistence adapters, repository queries, mapper XML, migrations, persistence tests | Production DB access and unapproved port changes |
| Test | `**/src/test/**`, `tests/**`, approved fixtures/test docs | Production behavior |
| Reviewer | None | All edits and Git/GitHub state |
| Integrator subagent | None | Conflict edits, logs, commits, push, PR/Issue mutations |
| Documentation | Approved `docs/**`, module `README.md`, runbooks, API docs | Shared harness logs unless parent assigns them |
| Module Writer | Approved target module directory (`<module-name>/**`) | Files outside assigned module and shared harness logs |

## Shared Records

Only the parent Integrator updates:

- `docs/ai-harness/worklog.md`
- `docs/ai-harness/agent-status.md`
- `docs/ai-harness/conflict-log.md`
- `docs/ai-harness/integration-log.md`
- `docs/ai-harness/handoff.md`
- `CODEX_WORKLOG.md`
- `docs/WORKLOG.md` for durable project milestones

## Boundary Rules

- Controller agents map transport concerns and never absorb business logic.
- Service agents preserve transaction, application, domain, and port boundaries.
- Batch agents orchestrate and call core; they do not implement financial calculations.
- SQL agents document mapping, index/cardinality, locking, bulk, rollback, and migration impact.
- Test agents return production defects to the owning writer rather than modifying production code.
- Documentation agents record verified behavior only.
- The parent Integrator may resolve conflicts in an integration branch, but must record conflict evidence and decision.
