# [ecl] 완료 결과가 0건인 재실행이 기존 충당금 summary를 삭제함

- **Issue ID:** RISK_ECL_02
- **Priority:** P1
- **Module:** ecl
- **Area / category:** core / bug
- **Labels:** module:ecl, area:core, type:bug, priority:p1, agent-loop, status:ready, spec-driven

## Code reference
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/application/service/allowance/AllowanceSummaryService.java:31
- ecl/ecl-core/src/main/java/com/ho/account/ecl/core/application/service/allowance/AllowanceSummaryService.java:33
- ecl/ecl-batch/src/main/java/com/ho/account/ecl/batch/job/tasklet/AllowanceSummaryTasklet.java:43
- ecl/ecl-core/src/test/java/com/ho/account/ecl/core/application/service/allowance/AllowanceSummaryServiceTest.java:43
- closing/core/src/main/java/com/ho/account/closing/application/service/EclProvisionService.java:65

## Problem statement
`rebuildAllowanceSummary`는 해당 기준일의 완료된 ECL 결과가 0건이면 기존 `allowance_summary`를 삭제하고 성공 결과를 반환한다. 0건은 실제 포트폴리오가 비어 있다는 확정 신호가 아니라 입력 미적재, 전 단계의 무산출 또는 실패한 재실행에서도 발생할 수 있다. 이 경로에는 기존 확정 summary 보존이나 명시적인 0건 완료 표시가 없다.

## Reproduction / evidence
기준일 D에 정상 `allowance_summary`가 있는 상태에서 완료 결과가 없도록 만든 뒤 `standaloneAllowanceSummaryJob` 또는 `allowanceSummaryStep`을 실행하면, `AllowanceSummaryService.java:31-35`가 기준일 D의 summary를 삭제하고 정상 반환한다. `AllowanceSummaryTasklet.java:43-54`도 0건을 오류로 취급하지 않는다. 기존 테스트는 이 삭제를 기대 동작으로 고정한다(`AllowanceSummaryServiceTest.java:43-52`). Closing은 같은 날짜의 summary가 비어 있으면 예외로 분개를 중단한다(`EclProvisionService.java:65-70`).

## Financial / architectural impact
이전 월말의 검증된 목표 충당금 자료가 재실행 한 번으로 사라지고, closing의 대손충당금 분개가 중단된다. 이미 사용한 ECL 평가 입력의 추적성과 재처리 복구 기준도 잃는다. 이 결함은 0원 분개를 만드는 것이 아니라 기존 확정 자료를 삭제해 후속 분개를 막는 것으로 한정된다.

## Proposed solution
완료 결과 0건이면 `AllowanceSummaryService`가 예외를 던져 해당 Step을 실패 처리하고 기존 summary를 보존한다. 진짜 0건 포트폴리오를 허용해야 한다면 별도의 명시적 완료 marker 계약을 정의한 후에만 삭제·0원 처리를 허용한다. **정확한 수정 allowlist (3파일):** `ecl/ecl-core/src/main/java/com/ho/account/ecl/core/application/service/allowance/AllowanceSummaryService.java`, `ecl/ecl-core/src/test/java/com/ho/account/ecl/core/application/service/allowance/AllowanceSummaryServiceTest.java`, `ecl/ecl-batch/src/test/java/com/ho/account/ecl/batch/AllowanceEclBatchIntegrationTest.java`.

## Acceptance criteria
- [ ] 완료 ECL 결과가 0건이면 summary 재생성 Step이 실패하고 같은 기준일의 기존 summary가 보존된다.
- [ ] 완료 결과와 계정 매핑이 유효하면 같은 기준일 summary를 단일 트랜잭션에서 교체하고 중복 적재하지 않는다.
- [ ] closing이 읽는 기준일 summary를 사용한 회귀에서 실패한 재실행 뒤에도 이전 확정 결과가 유지된다.

## Test gap and verification limits
현재 core 테스트는 0건 삭제를 성공으로 단언하고, Batch 통합 테스트는 결과가 생성되는 정상 경로만 검증한다. 감사 중 위 RISK_ECL_01에 적은 대상 core 테스트 명령은 통과했다. 수정 후 `./gradlew :ecl:ecl-core:test :ecl:ecl-batch:test :closing:core:test --offline --no-daemon --console=plain --max-workers=1`로 실패 Step·트랜잭션 롤백·closing 소비를 검증한다. 실제 DB 장애 중단 후 재시작은 이번 감사에서 실행하지 않았다.
