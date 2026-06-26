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

