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
- The parent Integrator updates harness logs (not a substitute for module documentation):
  - `worklog.md`
  - `agent-status.md`
  - `handoff.md`
  - `conflict-log.md` if applicable

## Quality Completion Gate

- 구현 완료와 독립 리뷰에서 [코드·문서 품질 계약](42-code-documentation-quality.md)의 Q1–Q4 완료표 확인은 필수다. 변경 파일·테스트와 설명을 대조해 코드 품질, 흐름, 모듈/초보자 문서, 비자명 로직의 의도 주석을 검토한다.
- 각 항목은 PASS/FAIL/N/A 및 파일·테스트 근거로 보고한다. 문서 전용 변경의 코드 주석은 구체적 사유가 있을 때 N/A 가능하며 독립 리뷰어가 이를 확인한다.
- 테스트 미실행은 PASS가 아니다. 미실행 사유·위험·다음 검증 게이트를 남기고, FAIL 또는 근거 누락이 있으면 완료 처리하지 않는다. 읽기 전용 리뷰어는 원래 작성자에게 수정 요청을 반환한다.
- 이 품질 확인은 기존 검증·Ready·merge 승인 게이트를 대체하지 않는다. 아래 로컬 구조 검사는 CI 미연결 상태이며 연결은 #672에서 별도 추적한다.

```powershell
node --test tools/ci/harness-quality-contract.test.cjs
```

## Rollback Note

Each change summary must state which files to revert or which commit to revert. 초보자 설명: rollback은 "문제가 생겼을 때 되돌리는 방법"이다.

