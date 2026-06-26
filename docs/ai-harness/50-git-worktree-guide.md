# Git Worktree Guide

## Branch Vs Worktree

- Branch is a name for a line of commits.
- Worktree is a separate folder that checks out a branch as an independent working space.
- branch는 커밋 흐름의 이름표다.
- worktree는 branch를 별도 폴더로 checkout한 독립 작업 공간이다.

초보자 설명: branch는 책갈피이고, worktree는 같은 책을 다른 책상에 펼쳐 놓는 것이다.

## What Worktree Does Not Do

- A worktree does not eliminate merge conflicts.
- It reduces workspace collisions when multiple agents work at the same time.
- Semantic conflicts still need review.
- worktree는 충돌을 없애는 도구가 아니라 병렬 작업 공간 충돌을 줄이는 도구다.

## Recommended Location

Create worktrees outside the project root when possible:

```powershell
git fetch origin
git worktree add -b agent/example-task ..\wt-example-task origin/develop
git worktree list
git worktree remove ..\wt-example-task
git worktree prune
```

If a worktree must be created inside the project, ignore these folders:

```gitignore
.worktrees/
.claude/worktrees/
```

These entries are included in the root `.gitignore`.

## Agent Pattern

1. Planner defines task boundaries.
2. Integrator creates a branch or worktree per task.
3. Coder/Test/Documentation agents work in their own spaces.
4. Integrator merges results into an integration branch.
5. Draft MR/PR is created for human review.
