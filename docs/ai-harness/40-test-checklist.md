# Test Checklist

## Before Testing

- Confirm current branch is not `main`, `master`, or `develop`.
- Confirm no conflict markers:

```powershell
rg -n "<<<<<<<|=======|>>>>>>>" .
```

- Confirm changed modules and docs are known:

```powershell
git status --short
git diff --stat
```

## Java And Gradle

- Narrow compile for changed modules:

```powershell
.\gradlew :module:compileJava --console=plain --max-workers=1
```

- Narrow tests:

```powershell
.\gradlew :module:test --console=plain --max-workers=1
```

- Full build when contracts, build scripts, shared modules, API contracts, or batch behavior changed:

```powershell
.\gradlew build --console=plain --max-workers=1
```

## Continuous Integration

- CI runs `:module:test` only for the modules touched by the diff. See `45-ci-module-validation.md`.
- Changing `shared-kernel`, `contracts`, or the build scripts makes CI run every module.
- CI does not run `bootJar`, container policy checks, or migration verification. Those remain local gates in this checklist.

초보자 설명: CI가 통과했다고 모든 검증이 끝난 것은 아니다. CI는 바뀐 모듈의 테스트만 돌리므로, 아래 배치/API 항목은 여전히 직접 확인해야 한다.

## Batch

- Confirm Spring Batch Job names and parameters.
- Run context smoke with job disabled when checking wiring only.
- Run actual Job smoke with explicit `spring.batch.job.name` when verifying execution.
- Record restartability, idempotency, and data prerequisites.

## API

- Confirm controller mappings.
- Confirm DTO validation and error response behavior.
- Run bootRun smoke only when needed and with local infrastructure disabled.

## Documentation

- Update module README or docs when behavior, commands, parameters, or architecture changed.
- Update harness logs:
  - `worklog.md`
  - `agent-status.md`
  - `handoff.md`
  - `conflict-log.md` if applicable

## Rollback Note

Each change summary must state which files to revert or which commit to revert. 초보자 설명: rollback은 "문제가 생겼을 때 되돌리는 방법"이다.

