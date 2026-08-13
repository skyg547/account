# CI Module Validation

## Purpose

This document describes how GitHub Actions decides which Gradle modules to test on a pull request or a push to `main`.

초보자 설명: CI는 코드를 올릴 때마다 자동으로 테스트를 돌려주는 로봇이다. 이 저장소는 모듈이 60개가 넘어서 매번 전부 돌리면 너무 오래 걸리므로, "이번에 건드린 모듈만" 골라서 돌린다.

## Background

Until `module-validation.yml` was added, `.github/workflows/` contained only `reporting-validation.yml`, which runs:

```bash
./gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain
```

That covered one module. The other 20+ Gradle modules had no automated gate, so a defect merged into them was never "passing CI" — it was never checked at all. Treat any pre-existing claim that a change "passed CI" as evidence only about `reporting`.

## Workflow Layout

`module-validation.yml` runs three jobs.

| Job | Responsibility |
| --- | --- |
| `detect` | Read the diff, resolve which Gradle projects to test, emit a matrix |
| `test` | Run `:<project>:test` for each resolved module in parallel |
| `result` | Collapse the matrix outcome into one fixed check name |

초보자 설명: `detect`는 "뭘 테스트할지 정하는" 단계, `test`는 "실제로 돌리는" 단계, `result`는 "결과를 하나로 모으는" 단계다.

## Module Resolution

`detect` does not hardcode a module list. It reads the `include` list in `settings.gradle` and matches the first colon-separated segment of each Gradle project path against the changed top-level directories.

```
loan/core/src/main/java/X.java   →  loan   →  :loan:core:test :loan:api:test :loan:batch:test
account-mart/mart-api/…          →  account-mart  →  :account-mart:mart-core:test …
gateway/src/main/…               →  gateway →  :gateway:test
docs/README.md                   →  docs   →  (no Gradle project — skipped)
```

This handles the repository's three naming shapes without special cases:

- regular subprojects (`loan:core`, `tax:api`)
- irregular subproject prefixes (`account-mart:mart-core`, `ecl:ecl-core`)
- flat single-project modules (`gateway`, `discovery`, `config-server`)

Adding a module requires updating `settings.gradle` only. The workflow needs no change.

## Full-Run Triggers

Selective execution is abandoned and every module runs when any of these change:

- `settings.gradle`, root `build.gradle`
- `gradlew`, `gradlew.bat`, `gradle/**`
- `shared-kernel/**`, `contracts/**`
- `.github/workflows/**`
- the comparison base cannot be determined (new branch push, forced push)
- `workflow_dispatch` manual run

초보자 설명: `shared-kernel`과 `contracts`는 모든 모듈이 가져다 쓰는 공용 부품이다. 여기를 고치면 어느 모듈이 깨질지 알 수 없으므로 전부 검사한다. 기준을 못 찾을 때도 전체를 도는데, 빠뜨리는 것보다 느린 편이 안전하기 때문이다.

## Branch Protection

`test` job names vary per run because the matrix depends on what changed. A branch protection rule cannot require a check whose name is not stable, so require **`Module Validation Result`** instead. It fails when any matrix entry failed or was cancelled.

## Non-Gradle Paths

`frontend/` is a Node project and is not covered by this matrix. `docs/` and other non-module paths produce no matrix entries, and the `test` job is skipped entirely rather than run empty.

## Local Equivalent

Before pushing, run the same narrow scope locally. See `40-test-checklist.md` for the full checklist.

```powershell
.\gradlew :module:test --console=plain --max-workers=1
```

## Known Follow-Ups

- `reporting-validation.yml` is now redundant with this workflow and double-runs the reporting tests. Removing it is a separate decision.
- `frontend/` has no CI job.
- The workflow runs `test` only. It does not run `bootJar`, container policy checks, or migration verification, all of which remain manual gates described in `40-test-checklist.md`.
