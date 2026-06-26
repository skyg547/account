# File Ownership

This policy maps agent roles to safe edit areas for this repository.

## Planner Agent

- Code edits prohibited.
- May update:
  - `docs/ai-harness/worklog.md`
  - `docs/ai-harness/decision-log.md`
  - planning docs under `docs/`

## Explorer Agent

- Code edits prohibited.
- May write analysis summaries to:
  - `docs/ai-harness/worklog.md`
  - `docs/ai-harness/agent-status.md`
  - task-specific docs when approved

## Controller Agent

May edit:

- `**/*Controller.java`
- `**/adapter/in/web/**`
- `**/api/**`
- `**/web/**`
- request/response DTOs when explicitly tied to controller changes

Must not move business logic into controllers.

## Service Agent

May edit:

- `**/*Service.java`
- `**/*ServiceImpl.java`
- `**/application/service/**`
- `**/service/**`
- `**/application/port/in/**`
- `**/application/port/out/**`

Must preserve domain rules and port/adapter boundaries.

## SQL Agent

May edit:

- `**/src/main/resources/db/migration/**`
- `**/repository/**` query methods
- `**/adapter/out/persistence/**`
- `**/infrastructure/persistence/**`
- `**/*Mapper.xml`
- `**/*mapper*.xml`
- `**/*query*.xml`
- `sqlmap/**` if introduced

Must document parameter mapping, index/performance impact, and rollback path.

## Test Agent

May edit:

- `**/src/test/**`
- `tests/**`
- `docs/test/**`
- test sections in module docs

## Reviewer Agent

- Code edits prohibited.
- May write:
  - review findings
  - `docs/ai-harness/agent-status.md`
  - `docs/ai-harness/handoff.md`
  - agent-specific review logs

## Integrator Agent

May edit in an integration branch:

- conflict files after reviewing both sides
- `docs/ai-harness/conflict-log.md`
- `docs/ai-harness/integration-log.md`
- `docs/ai-harness/handoff.md`
- Draft MR/PR body files if used

Must record each conflict and final decision.

## Documentation Agent

May edit:

- `docs/**`
- module `README.md`
- runbooks
- API docs
- harness docs

## Codex/Gemini/Antigravity Notes

- Codex commonly acts as Coder, Test, Documentation, and Integrator.
- Gemini commonly acts as Reviewer.
- Antigravity can coordinate Planner, Explorer, and human-visible integration loops.

