# Beginner Guide: AI Agent Coding With Git

## Who This Is For

This guide is for beginners who want to use several AI coding agents with Git without breaking the main codebase. In this project, the usual tools are Codex CLI, Gemini CLI, Antigravity, and local/open models for non-sensitive work.

초보자 설명: AI 에이전트는 "코드를 대신 타이핑하는 사람"이 아니라, Git 브랜치 위에서 작업하고 테스트 결과와 변경 이유를 남기는 작업자라고 생각하면 된다. Git은 실수했을 때 되돌아갈 수 있게 해 주는 안전장치다.

## The Modern Pattern

The current practical pattern is not "ask one AI to change everything." The safer pattern is:

1. Define a small task.
2. Create a separate branch or worktree.
3. Assign one clear role to each agent.
4. Let the agent change only approved files.
5. Review `git diff`.
6. Run tests or document why tests could not run.
7. Have a separate read-only Reviewer inspect the frozen diff and verification.
8. Let the parent Integrator commit/push and open a Draft PR/MR within publication approval.
9. Follow frozen Draft pre-review, Draft-stage checks and the approved parent Ready gate.
10. Merge only through the separately approved parent gate after final independent review and all required checks.

초보자 설명: AI가 빠르더라도 main에 바로 넣지 않는다. AI 작업도 사람 작업처럼 브랜치, 커밋, PR, 리뷰 순서를 거친다.

## Git Words You Need

| Word | Meaning | Beginner Meaning |
| --- | --- | --- |
| repository | project history stored by Git | 프로젝트의 시간 기록 |
| branch | name for a line of commits | 작업용 복사 흐름 |
| commit | saved snapshot of changes | 저장 지점 |
| remote | GitHub/GitLab copy | 서버에 있는 저장소 |
| push | send commits to remote | 내 작업을 서버로 올림 |
| pull/fetch | receive remote updates | 서버 변경을 가져옴 |
| merge | combine branches | 작업 흐름을 합침 |
| rebase | replay commits on a new base | 내 작업을 최신 기준 위로 다시 쌓음 |
| worktree | another folder checked out from the same repo | 같은 저장소의 별도 작업 폴더 |
| PR/MR | request to review and merge changes | 리뷰 요청서 |

## Recommended Agent Roles

Use one agent for one job. Mixing roles makes reviews harder.

| Agent | Good Use | Should Avoid |
| --- | --- | --- |
| Planner Agent | requirement summary, task split, risk list | direct code edits |
| Explorer Agent | find files, call paths, docs, dependencies | direct code edits |
| Coder Agent | implement approved small changes | architecture-wide rewrites |
| Test Agent | add tests, run build, write manual check steps | hiding failed tests |
| Reviewer Agent | review bugs, security, performance, layer violations | silently changing code |
| Integrator subagent (advisory) | read-only integration analysis, conflict/PR recommendations to parent | edits, shared logs, Git/GitHub mutations |
| Parent Integrator (main agent) | collect evidence, own shared records and approved Git/GitHub changes | publication or later gates without approval |
| Documentation Agent | approved README/API docs; return record drafts to parent | shared logs, Git/GitHub changes, business logic |

The parent Integrator is the main coordinating agent, not the `integrator` subagent. Implementers, Reviewers and advisory Integrators return evidence and requests; only the parent writes shared records and changes Git/GitHub state within user or explicit workflow approval. Tool/model choice does not grant publication authority. See [30](30-agents.md), [80](80-file-ownership.md), [85](85-github-issue-agent-loop.md), [86](86-multi-tool-issue-ownership.md) and the [PR lifecycle](88-pr-review-and-merge-runbook.md).

For this repository:

- Codex CLI is the default implementation and integration agent.
- Gemini CLI is the default independent review agent.
- Antigravity can be used for planning, broad exploration, or human-supervised orchestration.
- Local/open models are only for non-sensitive local exploration and draft generation.

## Safe Git Flow For One AI Task

The parent first confirms the ready Issue, ownership, approved scope and latest base. For an Issue task, the parent can create an external worktree as follows (replace `123` and `short-task` with the assigned identity):

```powershell
git status --short --branch
git fetch origin
git worktree add -b agent/123-short-task C:\tmp\account-123-short-task origin/main
cd C:\tmp\account-123-short-task
```

Use `/tmp/account-123-short-task` on Linux. Run subsequent edits and verification inside that worktree. A dirty primary checkout must be preserved; do not pull/reset it to start the task. See [50-git-worktree-guide.md](50-git-worktree-guide.md). The parent records the exact base and confirms the claim before implementation begins.

Ask the agent to work only on the task:

```text
Read AGENTS.md and docs/ai-harness first.
Summarize the requirement.
List files you plan to change.
Make the smallest safe change.
Update tests or explain why no test is needed.
Return verification evidence, PR body draft and worklog/handoff change requests to the parent.
Do not edit shared records or mutate Git/GitHub state.
Do not read secrets.
Do not push main/master/develop.
```

Review the agent result:

```powershell
git status --short --branch
git diff
rg -n "^(<<<<<<<|=======|>>>>>>>)" .
```

Run the right verification:

```powershell
.\gradlew test --console=plain
```

For a narrow module, prefer a narrower command:

```powershell
.\gradlew :module-name:test --console=plain
```

After a separate read-only review of the frozen diff and verification, the parent alone stages/commits approved files. Push and Draft PR creation require user or explicit workflow publication approval:

```powershell
git add <files>
git commit -m "Describe the small task"
git push -u origin agent/123-short-task
```

The approved parent opens a Draft PR/MR with `Refs #123`. Without publication approval, return the local result and request to the parent. Draft approval does not authorize Ready, merge, Issue close or cleanup. Follow [20-workflow.md](20-workflow.md) and [88-pr-review-and-merge-runbook.md](88-pr-review-and-merge-runbook.md) for frozen Draft pre-review, Draft-stage checks and separate later gates; head/base or evidence changes require re-review.

## Multi-Agent Flow

Use this flow when several agents work at once:

1. Planner Agent writes the task split.
2. The parent Integrator creates one branch/worktree per task and records ownership.
3. Explorer Agent gathers file and dependency context.
4. Coder Agent changes only its assigned files.
5. Test Agent adds tests or manual verification.
6. Reviewer Agent reviews without editing code.
7. An advisory Integrator returns integration/conflict recommendations without editing.
8. The parent resolves approved integration work and alone records conflict decisions and shared logs.
9. Final checks and independent review cover the resulting integration diff.
10. The parent creates a Draft PR/MR within publication approval for human review.

초보자 설명: 여러 AI에게 같은 파일을 동시에 고치게 하면 충돌이 난다. 그래서 역할과 파일 소유권을 먼저 나눈다.

## Worktree Flow For Parallel Agents

The parent creates worktrees when two or more agents need to work at the same time, using Issue-specific branches per the preceding example:

```powershell
git fetch origin
git worktree add -b agent/123-controller-task ..\wt-controller-task origin/main
git worktree add -b agent/124-service-task ..\wt-service-task origin/main
git worktree list
```

Each worktree is a separate folder. One writer owns each assigned scope. Only after merge verification and a separate cleanup approval may the parent remove its owned, clean, no-longer-needed worktree; inspect registration/ownership and preserved changes first. Finishing implementation alone is insufficient:

```powershell
git worktree remove ..\wt-controller-task
```

Important: worktrees reduce workspace collisions, but they do not remove merge conflicts. If two agents edit the same business rule, the parent Integrator must still reconcile recommendations and record the approved decision.

## Prompt Templates

### Planner Prompt

```text
You are Planner Agent.
Do not edit code.
Read AGENTS.md and docs/ai-harness.
Summarize the request, affected modules, risk, and proposed task split.
Return a file ownership plan for each agent.
```

### Coder Prompt

```text
You are Coder Agent.
Work only on the approved scope.
Keep changes minimal.
Before editing, list files you will modify.
After editing, provide changed files, tests run, rollback method, and risks.
Return evidence and proposed worklog/handoff entries to the parent Integrator.
Do not write shared records, stage/commit/push or change Issue/PR state.
```

### Reviewer Prompt

```text
You are Reviewer Agent.
Do not edit code.
Review the diff for bugs, regressions, security issues, performance issues, missing tests, and layer violations.
Report findings by severity with file and line references.
```

### Advisory Integrator Prompt

```text
You are a read-only advisory Integrator subagent, not the parent Integrator.
Inspect assigned branch diffs and verification evidence.
Return integration risks, conflict recommendations and Draft PR notes to the parent.
Do not edit files or shared records, or mutate Git/GitHub state.
```

## What To Put In A PR/MR

Always include:

- purpose of the change
- changed files or modules
- test commands and results
- screenshots or logs when relevant
- rollback method
- known risks
- which agents worked on it
- links to worklog, handoff, and conflict-log when relevant

Draft PR/MR is useful for incomplete work because reviewers can see progress without treating it as ready to merge.

## Things AI Agents Must Not Do

- Do not read `.env`, local settings, secrets, tokens, production URLs, or personal information.
- Do not paste secrets into chat, commits, logs, PRs, or docs.
- Do not run destructive commands without explicit approval.
- Do not mix Controller, Service, DAO, Query XML, VO, DTO, domain, and infrastructure responsibilities.
- Do not commit conflict markers.
- Do not mark work complete without tests or a documented verification reason.
- Do not hide uncertainty. Write the risk down.

## Daily Routine

The parent starts with this read-only status check and remote fetch:

```powershell
git status --short --branch
git fetch origin
```

Then:

1. Read `AGENTS.md`.
2. Read `docs/ai-harness/10-rules.md`.
3. Make or switch to a task branch.
4. Ask the AI to summarize before editing.
5. Inspect the diff yourself.
6. Run tests.
7. Have an independent Reviewer return findings and resolve required fixes.
8. The parent commits and, when publication is approved, pushes the task branch.
9. The approved parent creates a Draft PR/MR with the Issue reference.
10. Follow separately approved Ready/merge/close/cleanup gates after required reviews and checks.

## Recommended Defaults For This Repository

- One task per branch.
- One agent role per task.
- Use worktrees for parallel work.
- Use Codex for implementation and integration.
- Use Gemini for independent review.
- Use Antigravity for planning, broad exploration, or supervised orchestration.
- Keep model choice role-based, not vendor-name based.
- Only the parent updates `docs/ai-harness/worklog.md`, `agent-status.md`, `handoff.md` and shared history for meaningful work.
- Only the parent updates `conflict-log.md` when conflicts happen.

## Check Three Approval Scenarios

Read the four entrypoints (`GEMINI.md`, `CLAUDE.md`, `.agent/workflows/frontend-feature-loop.md`, and this guide) against central 30/80/85/86 and the PR lifecycle. Walk through each scenario without executing publication commands:

| Request | Implementer / independent Reviewer / advisory Integrator | Parent and external changes | Shared-record writer |
| --- | --- | --- | --- |
| Implementation only | Implement approved files, verify and return evidence; separate read-only review | Parent coordinates the approved local work; no push, Issue/PR publication without a separate applicable approval | Parent only (1 designated writer) |
| Review only | Read-only findings and record-change requests; no implementation or Git/GitHub mutation | Parent receives findings; review request alone authorizes no external publication | Parent only (1 designated writer; no write required) |
| Explicit Draft PR approval | Implementer returns verified diff/PR draft; independent Reviewer returns findings; advisory Integrator returns advice | After verification/review, parent alone performs approved commit/push/`Refs #<issue>` Draft PR; Ready/merge/close/cleanup need separate gates | Parent only (1 designated writer) |

Expected result in all three: zero unauthorized publications and one designated shared-record writer, the parent. A request to “record the review” is a request for evidence and a proposed entry, not reviewer write permission. A failed check, unresolved finding, missing approval or changed head/base returns to the relevant verification/review/approval gate; never treat it as success. These are document-contract walkthroughs, not proof of enforced runtime permissions.

## Source Notes

This guide follows the repository harness plus current official Git/GitHub/OpenAI guidance checked on 2026-06-26:

- GitHub Flow: https://docs.github.com/en/get-started/using-github/github-flow
- GitHub Pull Requests and Draft Pull Requests: https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/about-pull-requests
- Git worktree: https://git-scm.com/docs/git-worktree
- Git branch: https://git-scm.com/docs/git-branch
- OpenAI Codex manual: https://developers.openai.com/codex/codex-manual.md
