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
7. Push the branch.
8. Open a Draft PR/MR.
9. Have another agent or human review it.
10. Merge only after review and checks.

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
| Integrator Agent | merge branches, resolve conflicts, update logs | unrecorded conflict decisions |
| Documentation Agent | README, API docs, handoff, worklog | changing business logic |

For this repository:

- Codex CLI is the default implementation and integration agent.
- Gemini CLI is the default independent review agent.
- Antigravity can be used for planning, broad exploration, or human-supervised orchestration.
- Local/open models are only for non-sensitive local exploration and draft generation.

## Safe Git Flow For One AI Task

Start from a clean working tree:

```powershell
git status --short --branch
git fetch origin
git switch main
git pull --ff-only origin main
```

Create a task branch:

```powershell
git switch -c agent/my-small-task
```

Ask the agent to work only on the task:

```text
Read AGENTS.md and docs/ai-harness first.
Summarize the requirement.
List files you plan to change.
Make the smallest safe change.
Update tests or explain why no test is needed.
Update worklog and handoff.
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

Commit and push:

```powershell
git add <files>
git commit -m "Describe the small task"
git push -u origin agent/my-small-task
```

Open a Draft PR/MR and ask for review. Keep it draft while work is incomplete.

## Multi-Agent Flow

Use this flow when several agents work at once:

1. Planner Agent writes the task split.
2. Integrator Agent creates one branch or worktree per task.
3. Explorer Agent gathers file and dependency context.
4. Coder Agent changes only its assigned files.
5. Test Agent adds tests or manual verification.
6. Reviewer Agent reviews without editing code.
7. Integrator Agent merges branches into an integration branch.
8. Conflict decisions go into `conflict-log.md`.
9. Final checks run on the integration branch.
10. Draft PR/MR is created for human review.

초보자 설명: 여러 AI에게 같은 파일을 동시에 고치게 하면 충돌이 난다. 그래서 역할과 파일 소유권을 먼저 나눈다.

## Worktree Flow For Parallel Agents

Use worktrees when two or more agents need to work at the same time.

```powershell
git fetch origin
git worktree add -b agent/controller-task ..\wt-controller-task origin/main
git worktree add -b agent/service-task ..\wt-service-task origin/main
git worktree list
```

Each worktree is a separate folder. One agent works in one folder. When finished:

```powershell
git worktree remove ..\wt-controller-task
git worktree prune
```

Important: worktrees reduce workspace collisions, but they do not remove merge conflicts. If two agents edit the same business rule, a human or Integrator Agent must still decide the correct result.

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
Update docs/ai-harness/worklog.md if the change is meaningful.
```

### Reviewer Prompt

```text
You are Reviewer Agent.
Do not edit code.
Review the diff for bugs, regressions, security issues, performance issues, missing tests, and layer violations.
Report findings by severity with file and line references.
```

### Integrator Prompt

```text
You are Integrator Agent.
Fetch origin, inspect branch status, merge assigned branches, record conflicts in docs/ai-harness/conflict-log.md, run final verification, and prepare Draft PR notes.
Do not force push shared branches.
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

For beginners, repeat this checklist:

```powershell
git status --short --branch
git fetch origin
git pull --ff-only
```

Then:

1. Read `AGENTS.md`.
2. Read `docs/ai-harness/10-rules.md`.
3. Make or switch to a task branch.
4. Ask the AI to summarize before editing.
5. Inspect the diff yourself.
6. Run tests.
7. Commit small changes.
8. Push the task branch.
9. Create Draft PR/MR.
10. Merge only after review.

## Recommended Defaults For This Repository

- One task per branch.
- One agent role per task.
- Use worktrees for parallel work.
- Use Codex for implementation and integration.
- Use Gemini for independent review.
- Use Antigravity for planning, broad exploration, or supervised orchestration.
- Keep model choice role-based, not vendor-name based.
- Update `docs/ai-harness/worklog.md`, `agent-status.md`, and `handoff.md` for meaningful work.
- Update `conflict-log.md` when conflicts happen.

## Source Notes

This guide follows the repository harness plus current official Git/GitHub/OpenAI guidance checked on 2026-06-26:

- GitHub Flow: https://docs.github.com/en/get-started/using-github/github-flow
- GitHub Pull Requests and Draft Pull Requests: https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/about-pull-requests
- Git worktree: https://git-scm.com/docs/git-worktree
- Git branch: https://git-scm.com/docs/git-branch
- OpenAI Codex manual: https://developers.openai.com/codex/codex-manual.md
