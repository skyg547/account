# Agent Roles

## Planner Agent

- Analyzes requirements.
- Breaks work into tasks.
- Identifies impact scope.
- Does not edit code.

## Explorer Agent

- Searches the codebase.
- Lists candidate files.
- Summarizes dependencies and call relationships.
- Does not edit code.

## Coder Agent

- Edits code only inside approved scope.
- Follows minimal-change principles.
- Records changed files and reasoning.

## Controller Agent

- Checks controllers, URL mappings, `RequestMapping`, request/response DTOs, and screen/API connections.
- Focuses on files such as `*Controller.java`, `adapter/in/web`, `api`, and `web` packages.

## Service Agent

- Checks service and use-case flow.
- Focuses on `*Service.java`, `*ServiceImpl.java`, `application/service`, and service packages.
- Keeps business rules in application/domain layers, not controllers.

## SQL Agent

- Checks Query XML, Mapper XML, SQL, migrations, JPA repository query methods, parameter mapping, and performance impact.
- Uses high-reasoning review for financial data, joins, indexes, and bulk processing.

## Test Agent

- Writes or proposes tests.
- Defines manual verification steps.
- Documents build and log checks.

## Reviewer Agent

- Reviews security, performance, exception handling, layer responsibility, and regression risk.
- Does not edit code.
- Reports findings with file/line evidence.

## Integrator Agent

- Integrates multiple agent branches.
- Manages rebase/merge conflicts.
- Updates `conflict-log.md`.
- Confirms final tests.
- Drafts MR/PR body.

## Documentation Agent

- Updates change docs, API docs, runbooks, test docs, and handoff docs.
- Keeps beginner-friendly explanations where needed.

## Codex, Gemini CLI, And Antigravity Usage

- Codex CLI is the default Integrator/Coder/Test execution agent.
- Gemini CLI is the default independent Reviewer Agent.
- Antigravity can coordinate planning, IDE-assisted review, and human-visible agent loops.
- Any tool can act as Planner or Explorer if it follows the no-code-edit rule for those roles.

