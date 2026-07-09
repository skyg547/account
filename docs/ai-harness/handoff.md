# AI Harness Handoff

## Current State

- Branch: `main`
- Base branch: `main`
- Push status: `main` pushed to `origin/main`
- Current owner: Codex
- Verification: required document check, conflict marker check, trailing whitespace check, and diff check passed.

## What Changed

- Existing root guidance was preserved.
- New AI harness documents were added under `docs/ai-harness/`.
- Backups of existing harness files were created under `docs/ai-harness/_backup/2026-06-25/`.
- `.gitignore` now excludes local worktree folders.
- Beginner guide for AI agent coding with Git was added as `90-beginner-ai-agent-git-guide.md`.
- `ai-harness-upgrade-20260625` was merged into `main` by explicit user request.

## Next Recommended Steps

1. Use `docs/ai-harness/90-beginner-ai-agent-git-guide.md` as the beginner onboarding guide.
2. For the next AI task, start from updated `main` and create a new task branch.
3. Keep using Draft PR/MR for non-trivial follow-up work.

## Known Risks

- The repository has `.claude/settings.local.json`, but its content was not inspected to avoid local/private setting exposure.
- Slash-style branch prefixes failed locally, so the branch uses a slash-free name.

## 2026-07-03 Handoff - GitHub Issue Agent Loop Harness

- Branch: `agent/github-issue-agent-loop-harness`
- Worktree: `C:\tmp\account-gh-issue-harness`
- Base branch: `origin/main`
- Status: Review ready, not committed or pushed yet.
- Changed scope: GitHub Issue Agent Loop guidance, Issue Form template, Draft PR template, and harness document links.
- Verification: conflict marker search passed, `git diff --check` passed with CRLF warnings only, `gh --version` returned 2.96.0.
- GitHub CLI: installed at `C:\Program Files\GitHub CLI\gh.exe`; current shell PATH may need a new terminal session to find `gh` without the full path.
- GitHub auth: not logged in. Run `gh auth login` before creating issues or PRs.
- Git note: worktree ownership requires `git -c safe.directory=C:/tmp/account-gh-issue-harness -C C:/tmp/account-gh-issue-harness ...` unless safe.directory is configured or the worktree is recreated with normal ownership.
- Next step: review the templates, authenticate `gh`, then create the first parent issue for module-by-module inspection.

## Issue Loop Handoff Template

Use this template before stopping or transferring an issue-driven task.

```md
## YYYY-MM-DD Handoff - GH-<issue-number> <task-name>

- Current state:
- Issue:
- Branch:
- Worktree:
- Draft PR:
- Base branch:
- Owner agent:

### Done
-

### Not Done
-

### Next Steps
-

### Files To Inspect
-

### Commands Already Run
-

### Last Judgment
-

### Risks
-

### Prompt For Next Agent
-
```

## 2026-07-07 Handoff - GH-1 Issue Branch Worktree PR Harness Hardening

- Current state: Review ready, not committed, not pushed, no Draft PR yet.
- Issue: #1 https://github.com/skyg547/account/issues/1
- Branch: `agent/github-issue-agent-loop-harness`
- Worktree: `C:\tmp\account-gh-issue-harness`
- Draft PR: not created yet
- Base branch: `origin/main`
- Owner agent: Codex

### Done
- Issue #1 was fetched and reviewed.
- Compliance audit was added.
- Issue/Branch/Worktree/PR traceability model was added.
- Default GitHub PR template was added.
- Issue Form and dedicated PR template were hardened.
- Local worklog/status/handoff/integration templates now include Issue, Branch, Worktree, and PR fields.

### Not Done
- Branch was not committed or pushed.
- Draft PR was not created.
- Issue #1 was not closed.
- Repository labels were not created or applied.

### Next Steps
- Review diff.
- Commit with `GH-1: harden issue based agent loop harness` if accepted.
- Push `agent/github-issue-agent-loop-harness`.
- Create a Draft PR with `Refs #1`.
- Close Issue #1 only after merge and human confirmation.

### Risks
- Worktree still may require `git -c safe.directory=C:/tmp/account-gh-issue-harness` because it was created under elevated permissions.
- The main workspace has unrelated dirty changes and should not be mixed with this harness branch.

## 2026-07-08 Handoff - GH-1 Close And Runbook Update

- Current state: Issue #1 closed; PR #2 merged; follow-up harness update in progress.
- Issue: #1 https://github.com/skyg547/account/issues/1
- Merged PR: #2 https://github.com/skyg547/account/pull/2
- Branch: `agent/gh-1-close-harness-update`
- Worktree: `C:\tmp\account-gh-1-close-update`
- Base branch: `origin/main`
- Owner agent: Codex

### Done
- Confirmed PR #2 was mergeable and clean.
- Marked PR #2 ready from Draft.
- Merged PR #2 into `main` through GitHub PR merge.
- Closed Issue #1 manually with a comment.
- Added PR merge and issue close runbook to `85-github-issue-agent-loop.md`.

### Not Done
- This follow-up branch is not committed, pushed, or merged yet.
- Merged branch/worktree cleanup was not performed.

### Next Steps
- Run final verification.
- Commit this follow-up update.
- Push and create a small PR, or merge after user approval.

### Risks
- Do not delete `agent/github-issue-agent-loop-harness` while `C:\tmp\account-gh-issue-harness` is still checked out on that branch.
