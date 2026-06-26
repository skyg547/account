# Rebase And Merge Policy

## Basic Principles

- AI must not rebase or force push `main`, `master`, or `develop`.
- 기준 브랜치 `main`, `master`, `develop`은 AI가 직접 rebase하거나 force push하지 않는다.
- Rebase only personal/task branches such as `agent/*`, `ai/*`, `feature/*`, or `fix/*`.
- rebase는 `agent/*`, `ai/*`, `feature/*`, `fix/*` 같은 개인/작업 브랜치에서만 수행한다.
- Be careful rebasing already shared branches.
- 이미 공유된 브랜치는 rebase에 주의한다.
- Create a backup branch before rebase.
- rebase 전 백업 브랜치를 만든다.
- Do not auto-resolve conflicts without review.
- conflict 발생 시 자동 해결하지 말고 `conflict-log.md`에 기록한다.
- Record conflicts in `conflict-log.md`.
- Files with possible semantic conflicts require Integrator Agent or human final judgment.
- Prefer `--force-with-lease`, not `--force`.
- Consider `git rerere` only when repeated conflicts are common. Review automatic resolutions.

## Rebase Example

```powershell
git fetch origin
git checkout agent/my-task
git branch agent/my-task-backup
git rebase origin/develop
```

## Conflict Flow

```powershell
git status
```

Then:

1. Open conflicted files.
2. Resolve conflict markers.
3. Update `docs/ai-harness/conflict-log.md`.
4. Add files.
5. Continue rebase.

```powershell
git add <file>
git rebase --continue
```

Abort if needed:

```powershell
git rebase --abort
```

Push updated task branch:

```powershell
git push origin agent/my-task --force-with-lease
```

## Merge Integration Example

```powershell
git fetch origin
git checkout ai/integration-my-feature
git merge --no-ff agent/controller-part
git merge --no-ff agent/service-part
git merge --no-ff agent/test-part
```

초보자 설명: rebase는 내 작업을 최신 기준 위로 다시 쌓는 것이고, merge는 두 작업 흐름을 합치는 것이다. 공유 브랜치에서 무리하게 rebase하면 다른 사람 기록이 꼬일 수 있다.
