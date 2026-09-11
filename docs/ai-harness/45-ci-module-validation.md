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

- `settings.gradle`, root `build.gradle`, `gradle.properties`
- `gradlew`, `gradlew.bat`, `gradle/**`
- `shared-kernel/**`, `contracts/**`
- `.github/workflows/**`
- `.gitattributes`, `.editorconfig`
- the comparison base cannot be determined (new branch push, forced push)
- `workflow_dispatch` manual run

초보자 설명: `shared-kernel`과 `contracts`는 모든 모듈이 가져다 쓰는 공용 부품이다. 여기를 고치면 어느 모듈이 깨질지 알 수 없으므로 전부 검사한다. 기준을 못 찾을 때도 전체를 도는데, 빠뜨리는 것보다 느린 편이 안전하기 때문이다.

## Branch Protection

`test` job names vary per run because the matrix depends on what changed. A branch protection rule cannot require a check whose name is not stable, so require **`Module Validation Result`** instead.

That job is fail-closed. It passes only when `detect` succeeded **and** `test` is `success` or `skipped`. Any other combination fails, including the case where `detect` itself broke and `test` never ran.

| `detect` | `test` | Result |
| --- | --- | --- |
| success | success | pass |
| success | skipped (no Gradle module changed) | pass |
| success | failure / cancelled | fail |
| failure / cancelled / skipped | any | fail |

초보자 설명: 이 검사는 "merge해도 되는가"를 결정하는 최종 관문이다. 앞 단계가 고장 나서 테스트를 아예 못 돌린 경우에도 초록불을 주면 검증 없이 코드가 들어가므로, "성공"이 아닌 모든 경우를 실패로 처리한다.

## Fail-Closed Parsing

If the `settings.gradle` parse yields zero projects — a format change, a moved file, a broken regex — `detect` exits non-zero instead of reporting "no modules changed". Without this guard a parsing break would silently skip every test and report green.

## Dependency Rate Limiting

The first full run failed `master-data` with `429 Too Many Requests` from Maven Central — 25 jobs resolved dependencies simultaneously. Two guards address this:

- `max-parallel: 6` caps concurrent jobs.
- The test step retries up to 3 times with 30s/60s backoff, but **only** when the log shows a transient resolution failure (`Too Many Requests`, `Could not resolve`, `Could not GET`, `Connection reset`, `Read timed out`). A genuine test or compile failure is reported immediately without retry.

초보자 설명: 여러 job이 한꺼번에 같은 서버에서 라이브러리를 받으면 서버가 거절한다(429). 코드는 멀쩡한데 실패하므로 동시 실행 수를 제한하고, 네트워크 탓일 때만 다시 시도한다. 진짜 버그를 세 번씩 돌리면 시간만 낭비되므로 그 경우는 바로 멈춘다.

## Non-Gradle Paths

`frontend/` is a Node project and is not covered by this matrix. `docs/` and other non-module paths produce no matrix entries, and the `test` job is skipped entirely rather than run empty.

## Harness Validation

`.github/workflows/harness-validation.yml` adds a separate, lightweight gate for repository instructions. It runs on **every pull request**, pushes to `main`, and manual dispatch, without path or draft filters. Thus `AGENTS.md`, `.agents/**`, `.codex/**`, Issue templates, `docs/ai-harness/**`, and **`tools/**` alone** all execute the checks. Running this small suite unconditionally avoids changed-path detection failures and missing required-check results.

| Job | Behavior |
| --- | --- |
| `validate` | Check existing Python/PyYAML/Node runtimes, validate TOML/YAML/role/skill/path contracts, execute Python fixtures and #662 Node PR lifecycle tests, verify nonzero test counts and no skips |
| `result` | Always run after `validate`, expose the fixed **Harness Validation Result**, pass only for `needs.validate.result == success` |

| `validate` outcome | Harness Validation Result |
| --- | --- |
| success, including executed schema and Python/Node checks | pass |
| failure / cancelled / skipped / unknown | fail |

Missing files/parsers, malformed syntax, test failures, empty test discovery and skipped tests fail the gate. `set -euo pipefail` preserves test-runner errors through `tee`; report validation also requires actual test counts. Logs contain the runtime versions, schema category counts, Python `Ran N tests`, and Node `# tests`/`# pass`/failure counters. A fully cancelled workflow may prevent the result job from finishing; absence or cancellation is never success evidence.

The new workflow has only `contents: read`, uses `pull_request` rather than `pull_request_target`, disables persisted checkout credentials and performs no installation, publication, Issue/PR mutation or secret-backed operation. Python uses the runner's existing PyYAML 6.0.1; runtime drift fails for a reviewed correction. See [fixed inputs, local commands and limitations](95-codex-skills-subagents.md#validation).

초보자 설명: Gradle 검사는 업무 프로그램을 시험하고, 하네스 검사는 AI 작업 지시서와 역할 약속을 시험한다. 하네스 파일만 바뀌어 Gradle 시험이 생략되더라도 하네스 시험의 건수와 결과는 따로 표시된다. 둘 다 필요한 변경이면 두 검사 모두 확인한다.

The Gradle workflow and its existing full-run triggers are unchanged: adding or modifying `.github/workflows/**` still selects all Gradle modules. Harness success does not replace **Module Validation Result** or certify business code. Where branch protection supports required checks, an authorized administrator can require both fixed names; creating this workflow does not change repository rules or grant merge authority.

Rollback uses a reviewed follow-up PR to revert this Issue's workflow/validator/fixtures and guide changes while preserving shared historical records. No application schema or data changes are involved. Independent review and separately approved merge remain external gates.

## Local Equivalent

Before pushing, run the same narrow scope locally. See `40-test-checklist.md` for the full checklist.

```powershell
.\gradlew :module:test --console=plain --max-workers=1
```

## Known Follow-Ups

- `reporting-validation.yml` is now redundant with this workflow and double-runs the reporting tests. Removing it is a separate decision.
- `frontend/` has no CI job.
- The workflow runs `test` only. It does not run `bootJar`, container policy checks, or migration verification, all of which remain manual gates described in `40-test-checklist.md`.
