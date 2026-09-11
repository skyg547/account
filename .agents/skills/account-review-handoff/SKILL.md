---
name: account-review-handoff
description: Verify an account repository change, review its diff, synchronize AI harness records, and prepare a safe handoff or Draft PR summary. Use after implementation, before commit or PR, during review, or when resuming and auditing another agent's branch.
---

# Account Review And Handoff

## Rebuild Context

1. Read the Issue or task contract, applicable `AGENTS.md`, module docs, and current branch status.
2. Compare the actual diff with the promised file allowlist and acceptance criteria.
3. Treat unrelated pre-existing changes as out of scope and do not revert them.

## Review

1. Check correctness, financial invariants, security, exception behavior, architecture boundaries, batch restartability, SQL performance, and regression risk.
2. Check test quality and identify missing boundary, failure, and repeat-run cases.
3. Report review findings by severity with file and line evidence.
4. Keep Reviewer and Integrator subagents read-only; send required edits back to the owning writer or parent.

## Verify

1. Run targeted tests for changed behavior.
2. Expand to affected module build, wiring, SQL, or smoke tests when the blast radius requires it.
3. Run `git diff --check`.
4. Search tracked source and docs for unresolved conflict markers.
5. Record every skipped command with reason and residual risk.

## Synchronize Records

Let only the parent Integrator update shared records:

- `docs/ai-harness/worklog.md`
- `docs/ai-harness/agent-status.md`
- `docs/ai-harness/handoff.md`
- `docs/ai-harness/conflict-log.md` when conflicts occurred
- `docs/ai-harness/integration-log.md` after integration
- `docs/history/CODEX_WORKLOG.md` for Codex implementation or verification

Include Issue, branch, worktree, PR, changed files, impact, commands/results, rollback, risks, and next owner. Keep `docs/WORKLOG.md` for durable project-level milestones rather than every subagent action.

## Gate The Result

- Do not claim completion without verification evidence or an explicit blocked reason.
- Keep the PR draft until required checks and review are satisfied.
- Do not merge, close the Issue, or remove branches/worktrees without the relevant user-approved gate.
