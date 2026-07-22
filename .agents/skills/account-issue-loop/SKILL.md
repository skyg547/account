---
name: account-issue-loop
description: Run account repository work through a GitHub Issue, traceable agent branch, isolated worktree, verification, Draft PR, merge gate, and handoff. Use when a task has or should have a GitHub Issue, when the current checkout is dirty, when multiple agents work in parallel, or when the user asks for branch/worktree/PR based execution.
---

# Account Issue Loop

## Establish The Contract

1. Read `AGENTS.md`, `docs/ai-harness/10-rules.md`, and `docs/ai-harness/85-github-issue-agent-loop.md`.
2. Read the selected Issue and comments when an Issue exists.
3. Record goal, non-goals, base branch, allowed files, acceptance criteria, verification, rollback, and PR expectation.
4. Create an Issue only when the task is an executable change that benefits from durable tracking. Do not create one for a simple explanation or read-only status request.

## Isolate The Work

1. Require one non-base branch per executable Issue.
2. Name Issue branches `agent/<issue-number>-<short-slug>`.
3. Use an external worktree when the current checkout is dirty, parallel agents will write, or the user requests isolation.
4. Base the branch on the fetched remote base branch. Never push directly to `main`, `master`, or `develop`.
5. Record Issue, branch, worktree, and base identifiers before editing.

## Execute The Loop

1. Summarize the requirement and inspect module documentation.
2. Split read-only exploration/review from write tasks.
3. Give every writer an explicit file allowlist. Never assign the same file to concurrent writers.
4. Implement the smallest coherent change.
5. Run targeted verification first, then expand for shared contracts or build configuration.
6. Run `$account-review-handoff` before declaring review readiness.
7. Let the parent Integrator update shared harness logs once after collecting agent results.

## Apply External Gates

- Treat push, Draft PR creation, Ready transition, merge, Issue close, remote branch deletion, and worktree removal as explicit workflow gates.
- Use `Refs #<issue>` while a PR is draft or does not yet satisfy close conditions.
- Use `Fixes #<issue>` only when merge should close the Issue.
- Merge through the PR path. Do not bypass review with a direct base-branch push.
- Remove branch/worktree resources only after merge verification and confirmation that no follow-up uses them.

## Return

Report the Issue, branch, worktree, PR, changed files, verification, rollback, risks, and next owner. Read [the detailed runbook](../../../docs/ai-harness/85-github-issue-agent-loop.md) when executing GitHub CLI commands.
