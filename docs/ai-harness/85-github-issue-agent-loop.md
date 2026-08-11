# GitHub Issue Agent Loop

## Purpose

This document defines how to manage AI coding work with GitHub Issues, agent branches, worktrees, Draft PRs, and the local AI harness records.

초보자 설명: GitHub Issue는 "AI에게 맡길 작업 지시서"이고, branch/worktree는 "안전한 작업 공간"이며, Draft PR은 "사람 리뷰 전 검수 문"이다.

Codex, Gemini, and Claude Code ownership, claim races, handoffs, and status transitions are defined in [86-multi-tool-issue-ownership.md](86-multi-tool-issue-ownership.md).

## Branch Or Worktree

Use both concepts together:

- A branch is required for every AI task.
- A worktree is optional, but recommended when isolation matters.

Use a normal branch in the current checkout when:

- the current working tree is clean;
- only one agent is working;
- the change is small and short-lived;
- no unrelated local work is in progress.

Use a separate worktree when:

- the current checkout has uncommitted work;
- two or more agents may work in parallel;
- the task is a module-by-module inspection loop;
- the Integrator Agent needs to compare or combine multiple agent branches;
- the user asks for isolated GitHub Issue based work.

Recommended pattern:

```powershell
git fetch origin
git worktree add -b agent/123-short-task C:\tmp\account-123-short-task origin/main
cd C:\tmp\account-123-short-task
```

Prefer `C:\tmp` or `..\wt-*` outside the project root. If a worktree must be created inside the project, keep `.worktrees/` ignored.

## Issue Types

Use one parent issue for a large loop and sub-issues for executable tasks.

- Parent issue: module audit, milestone, or architecture initiative.
- Sub-issue: one module, one layer, one bug, one test gap, or one documentation task.
- Dependency: mark blocking relationships when one issue must be solved first.

Keep each executable issue small enough for one branch and one Draft PR.

## Required Issue Fields

Every AI Agent task issue should contain:

- goal and non-goals;
- target module and layer (`api`, `batch`, `core`, docs, infra);
- expected agent roles;
- allowed file scope;
- branch or worktree plan;
- acceptance criteria;
- verification commands;
- rollback method;
- security notes;
- required worklog and handoff updates.

## Labels

Recommended labels:

- `agent-loop`
- `agent:codex`
- `agent:gemini`
- `agent:claude-code`
- `agent:planner`
- `agent:explorer`
- `agent:coder`
- `agent:test`
- `agent:reviewer`
- `agent:integrator`
- `area:api`
- `area:batch`
- `area:core`
- `type:bug`
- `type:refactor`
- `type:test`
- `type:docs`
- `priority:p0`
- `priority:p1`
- `priority:p2`
- `status:ready`
- `status:in-progress`
- `status:blocked`
- `status:needs-review`

The `agent:<tool>` labels identify the current implementation tool. Role labels such as `agent:coder` identify responsibility and do not reserve the Issue. Keep exactly one `status:*` workflow label and one implementation-owner label on a claimed open Issue.

If repository labels do not exist yet, create them manually or with an approved GitHub CLI/API step. The current repository uses labels as its workflow state because no GitHub Projects Status field is configured.

## Project Fields

Recommended GitHub Projects fields:

- Status: Backlog, Ready, In progress, Review, Blocked, Done.
- Module: `asset-lease`, `tax`, `reporting`, etc.
- Layer: `api`, `batch`, `core`, docs, infra.
- Agent Role: Planner, Explorer, Coder, Test, Reviewer, Integrator.
- Risk: Low, Medium, High.
- Target Branch: planned branch name.
- Verification: Not run, Targeted pass, Full pass, Blocked.

GitHub Projects is optional. If it is introduced later, its Status must mirror the Issue labels and must not become a conflicting second claim source.

## Standard Issue Loop

```text
Issue 생성
→ Planner/Explorer가 범위 분석 댓글 작성
→ agent/{issue-number}-{slug} 브랜치 생성
→ 필요 시 worktree 생성
→ Coder/Test Agent 작업
→ docs/ai-harness/worklog.md 업데이트
→ 테스트/검증
→ Reviewer Agent 리뷰
→ Integrator Agent 충돌/통합 확인
→ Draft PR 생성
→ 사람 리뷰
→ merge
→ Issue 종료
→ handoff.md 업데이트
```

## Agent Prompt Contract

Use this prompt shape when assigning an issue to Codex:

```text
GitHub Issue #123 기준으로 작업해줘.
main/master/develop 직접 push 금지.
agent/123-short-task 브랜치와 별도 worktree를 사용해줘.
요구사항 요약, 영향 범위, 변경 계획을 먼저 제시하고 진행해줘.
테스트 후 worklog, agent-status, handoff를 업데이트해줘.
Draft PR 본문까지 작성해줘.
```

For review-only work:

```text
GitHub Issue #123 변경분을 Reviewer Agent 관점으로 검토해줘.
직접 코드 수정은 하지 말고, 버그/위험/테스트 누락을 파일/라인 기준으로 보고해줘.
```

## GitHub CLI Usage

After `gh` is installed and authenticated:

```powershell
gh auth login
gh issue create --title "[AI][asset-lease] Split API/BATCH/CORE follow-up" --body-file issue-body.md --label "agent-loop,type:refactor,area:core"
gh issue list --label agent-loop --state open
gh issue view 123 --comments
gh pr create --draft --title "Fixes #123 asset lease follow-up" --body-file pr-body.md
```

Use `Refs #123` in a Draft PR while work still needs review. Use `Fixes #123` only when the PR should close the issue after merge.

## Safety Gates

- Do not read or print secrets, credentials, tokens, personal information, or production URLs.
- Do not push directly to `main`, `master`, or `develop`.
- Do not commit conflict markers.
- Do not mark an issue complete without tests or a documented reason why tests could not run.
- Do not install GitHub CLI, labels, automations, or external dependencies without user approval.

## Required Local Records

For every issue loop, update:

- `docs/ai-harness/worklog.md`
- `docs/ai-harness/agent-status.md`
- `docs/ai-harness/handoff.md`
- `docs/ai-harness/conflict-log.md` when conflicts occur
- `CODEX_WORKLOG.md` when Codex implements or verifies changes

## Issue / Branch / Worktree / PR Traceability Model

Use one trace identity for every non-trivial AI task.

| Object | Rule | Example |
| --- | --- | --- |
| Issue | Source of truth for scope and acceptance criteria | `#123` |
| Branch | One branch per executable issue | `agent/123-asset-lease-core-test` |
| Worktree | One isolated checkout per branch when needed | `C:\tmp\account-123-asset-lease-core-test` |
| Draft PR | One Draft PR per branch when work is reviewable | `Refs #123` |
| Worklog ID | Local record key | `GH-123` |

The Issue explains what should happen. The Branch contains the commits. The Worktree isolates files on disk. The Draft PR is the review gate. Local harness records mirror the state and must not become a separate source of truth.

## Naming Rules

- Branch: `agent/{issue-number}-{short-slug}` for AI implementation or integration work.
- Review-only branch: `review/{issue-number}-{short-slug}` only when review notes are committed as docs.
- Worktree: `C:\tmp\account-{issue-number}-{short-slug}` on Windows, or `../wt-{issue-number}-{short-slug}` when outside the repo root.
- Draft PR title: `[#issue-number] short summary`.
- Commit message prefix: `GH-issue-number: short summary` when the change is issue-scoped.

Keep the slug short and stable. Do not rename branches just to improve wording after review starts.

## Command Recipe

```powershell
# Read the issue first.
gh issue view 123 --comments

# Create isolated work when the current checkout is dirty or parallel work is expected.
git fetch origin
git worktree add -b agent/123-short-slug C:\tmp\account-123-short-slug origin/main
cd C:\tmp\account-123-short-slug

# Work and verify.
git status --short --branch
rg -n "^(<<<<<<<|=======|>>>>>>>)" .
git diff --check

# Commit and push only when the user asked for it or the workflow requires PR creation.
git add <files>
git commit -m "GH-123: short summary"
git push origin agent/123-short-slug

# Open a review gate.
gh pr create --draft --title "[#123] short summary" --body-file pr-body.md
```

Use `Refs #123` while the PR is draft or review-only. Use `Fixes #123` only when the PR should close the issue on merge.

## Status Sync Checkpoints

Update these records at each checkpoint:

| Checkpoint | GitHub Issue | Branch | Worktree | Draft PR | Local harness records |
| --- | --- | --- | --- | --- | --- |
| Intake | Confirm scope | Not created | Not created | Not created | `worklog.md`, `agent-status.md` planned |
| Start | Mark or comment in progress when approved | Create branch | Create if needed | Not created | Add issue/branch/worktree IDs |
| Implementation | Add blocker comments only when useful | Commit locally | Keep isolated | Not created | Update worklog progress |
| Verification | Record skipped tests or blockers | Ready to push | Clean or known dirty | Draft body prepared | Add test result and rollback |
| Review | Link PR | Push branch | Keep until merge | Draft PR open | Handoff includes PR URL |
| Merge | Issue closes only with reviewed PR | Branch merged | Remove/prune after merge | PR merged | Integration log and handoff updated |

## Issue #1 Current Application

Issue `#1` is the parent issue for AI harness setup. This branch applies it as:

- Branch: `agent/github-issue-agent-loop-harness`
- Worktree: `C:\tmp\account-gh-issue-harness`
- Draft PR target: `origin/main`
- Local audit: `docs/ai-harness/05-issue-1-compliance-audit.md`
- Close condition: only after the branch is committed, pushed, reviewed, merged, and the user confirms Issue #1 can be closed.

## PR Merge And Issue Close Runbook

Use this runbook when the user explicitly approves merge after a Draft PR is ready.

1. Check PR state and mergeability.

```powershell
gh pr view <pr-number> --repo <owner>/<repo> --json number,state,isDraft,mergeable,mergeStateStatus,statusCheckRollup,baseRefName,headRefName
```

2. If the PR is still Draft and the user asked to merge, mark it ready first.

```powershell
gh pr ready <pr-number> --repo <owner>/<repo>
```

3. Merge through GitHub, not by direct push to `main`.

```powershell
gh pr merge <pr-number> --repo <owner>/<repo> --merge --subject "Merge pull request #<pr-number> from <head-branch>" --body "GH-<issue-number>: <summary>"
```

4. Verify the merge result.

```powershell
gh pr view <pr-number> --repo <owner>/<repo> --json number,state,mergedAt,mergedBy,url
```

5. Close the issue only after the merge is verified. If the PR body used `Fixes #<issue-number>`, GitHub may close it automatically. If the PR body used `Refs #<issue-number>`, close it manually with a short comment.

```powershell
gh issue close <issue-number> --repo <owner>/<repo> --comment "Closed after PR #<pr-number> merged <summary>."
```

6. Verify the issue state.

```powershell
gh issue view <issue-number> --repo <owner>/<repo> --json number,title,state,url
```

7. Update local records.

- `docs/ai-harness/worklog.md`
- `docs/ai-harness/agent-status.md`
- `docs/ai-harness/integration-log.md`
- `docs/ai-harness/handoff.md`
- `docs/ai-harness/conflict-log.md` when conflicts occurred

8. Clean up branches and worktrees only after confirming no follow-up work is needed.

```powershell
git worktree list
git worktree remove <worktree-path>
git worktree prune
git push origin --delete <merged-branch>
```

Do not delete a branch that is still checked out by an active worktree.

## Issue #1 Closure Result

Issue `#1` was handled using the current GitHub Issue Agent Loop:

- Source issue: `#1 하네스구성`
- Work branch: `agent/github-issue-agent-loop-harness`
- Worktree: `C:\tmp\account-gh-issue-harness`
- Draft PR: `#2 [#1] Harden issue based agent loop harness`
- PR transition: Draft -> Ready for review
- Merge command type: GitHub PR merge commit via `gh pr merge --merge`
- Merge result: PR `#2` merged into `main`
- Issue closure: Issue `#1` manually closed after merge because PR used `Refs #1`
- Follow-up documentation branch: `agent/gh-1-close-harness-update`
