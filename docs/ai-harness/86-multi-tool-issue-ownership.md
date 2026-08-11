# Multi-Tool GitHub Issue Ownership

## Purpose

This runbook coordinates Codex, Gemini, and Claude Code through GitHub Issues. It prevents two coding tools from implementing the same Issue while still allowing independent review and parallel work on different Issues.

초보자 설명: GitHub Issue의 상태 라벨은 작업 표지판이고 `agent:*` 라벨은 현재 열쇠를 가진 도구를 뜻한다. `status:ready`가 아닌 Issue를 별도 확인 없이 구현하면 안 된다.

## Source Of Truth

- The GitHub Issue, its labels, comments, linked branch, and Draft PR are the remote source of truth.
- The repository currently uses Issue labels for workflow state. A GitHub Projects Status field is optional and must only mirror the Issue state if a Project is added later.
- Local harness records mirror remote state; they do not reserve work by themselves.
- The primary checkout may be dirty or behind. Never pull, reset, or overwrite it to synchronize the harness. Fetch `origin/main` and create or refresh an external Issue worktree instead.

## Labels

Exactly one workflow status should be active on an open executable Issue.

| Label | Meaning | May a new implementation tool claim it? |
| --- | --- | --- |
| `status:ready` | Scope and acceptance criteria are ready; no active writer owns it | Yes |
| `status:in-progress` | One implementation owner has claimed the Issue | No |
| `status:blocked` | The documented blocker prevents meaningful progress | No |
| `status:needs-review` | Implementation is frozen for independent review | Review only; no implementation edits |

Use one implementation-owner label while work is claimed:

- `agent:codex`
- `agent:gemini`
- `agent:claude-code`

`agent-loop` identifies Issues that follow the Issue/branch/worktree/Draft PR contract. Role labels such as `agent:coder` or `agent:reviewer` describe a role and do not replace the tool-owner label.

GitHub assignees can only be GitHub users or installed bot accounts with repository access. Do not invent an assignee named Codex, Gemini, or Claude Code. When no tool-specific bot account exists, use the authenticated human owner as assignee if approved, and record the actual coding tool with `agent:*` plus the claim comment.

## Claim Protocol

Only claim an Issue whose current state is `OPEN` and whose workflow label is `status:ready`.

1. Read the Issue body, all comments, linked PRs, assignees, and labels.
2. Fetch the base branch and record the exact `origin/main` commit. Do not synchronize by modifying a dirty primary checkout.
3. Confirm there is no active `status:in-progress` or `status:blocked` label and no newer claim comment.
4. Change `status:ready` to `status:in-progress`, add exactly one `agent:*` owner label, and add the approved GitHub assignee when applicable.
5. Immediately add a start comment containing owner tool, branch, external worktree, base commit, allowed files, acceptance criteria, and verification plan.
6. Refresh the Issue. If a concurrent claim produced more than one owner label or claim comment, stop before editing. The parent Integrator resolves the race; the first complete claim by GitHub timestamp normally wins.
7. Create or reuse only the Issue branch and its isolated worktree. Never write from the dirty primary checkout.

Example claim comment:

```text
Claimed by: Claude Code
Status: status:in-progress
Branch: agent/123-short-slug
Worktree: external Issue worktree
Base: origin/main@<commit>
Allowlist: module-a/** and its module docs/tests
Verification: targeted tests, bootJar/JAR smoke, diff/marker checks
```

## Parallel Work Rules

- One executable Issue has one active implementation writer.
- Codex, Gemini, and Claude Code may implement different `status:ready` Issues at the same time.
- A large parent Issue must be split into executable sub-Issues before multiple writers work in parallel.
- Parallel writers require disjoint module/file allowlists. Shared build files, Compose manifests, and harness records remain parent Integrator owned.
- `status:in-progress` owned by another tool is not available. Ask for a documented handoff or choose a different `status:ready` Issue.
- `status:needs-review` may be reviewed by a different tool, but the reviewer stays read-only and reports findings to the implementation owner or parent Integrator.

## Handoff And Status Transitions

| From | To | Required evidence |
| --- | --- | --- |
| Unscoped | `status:ready` | Goal, non-goals, allowlist, acceptance criteria, verification, rollback, safety |
| `status:ready` | `status:in-progress` | Valid claim, one owner label, branch/worktree/base and start comment |
| `status:in-progress` | `status:blocked` | Blocker, attempts, preserved worktree state, next unblock action |
| `status:blocked` | `status:ready` | Blocker resolved and prior owner explicitly releases the Issue |
| `status:in-progress` | `status:needs-review` | Implementation frozen, targeted verification, diff and rollback summary |
| `status:needs-review` | `status:in-progress` | Review findings require changes; implementation owner resumes |
| `status:needs-review` | Closed | Reviewed PR merged and close conditions verified |

For an owner transfer:

1. The current owner posts a handoff with changed files, uncommitted/committed state, commands and results, risks, branch/worktree, and next action.
2. The parent Integrator confirms the handoff and changes the `agent:*` owner label.
3. The new owner refreshes the Issue and worktree before editing.
4. Do not delete or recreate an existing dirty worktree until its changes are inspected and preserved.

If work is abandoned without a usable change, the parent Integrator records why, removes the stale owner label and assignee, and returns the Issue to `status:ready`. A tool may not silently release another tool's claim.

## Review, PR, And Completion

- The implementation owner moves the Issue to `status:needs-review` only after targeted verification and a frozen diff.
- The independent reviewer does not commit production/test fixes. Findings go back to the implementation owner.
- The parent Integrator alone updates shared harness records, stages, commits, pushes, opens or changes PR state, merges, closes Issues, and cleans branches/worktrees.
- Draft PRs use `Refs #<issue>` until close conditions are satisfied. Use `Fixes #<issue>` only when the reviewed merge should close the Issue.
- The closed Issue state represents Done. Remove active `status:*` and current-owner labels after merge/close if they would imply ongoing ownership; history remains in comments and the PR.

## GitHub CLI Reference

Read before claiming:

```powershell
gh issue view 123 --json number,state,labels,assignees,projectItems,url --comments
git fetch origin main
git rev-parse origin/main
```

Claim a ready Issue after approval:

```powershell
gh issue edit 123 --remove-label "status:ready" --add-label "status:in-progress" --add-label "agent:gemini"
gh issue comment 123 --body "<claim contract>"
```

Move a frozen implementation to review:

```powershell
gh issue edit 123 --remove-label "status:in-progress" --add-label "status:needs-review"
gh issue comment 123 --body "<verification and review handoff>"
```

Never place credentials, private endpoints, environment files, or command output containing secrets in labels, comments, worklogs, or PR bodies.
