# [expenditure-resolution] 예산 행이 없으면 지출결의가 한도 검사 없이 생성됨

- **Issue ID:** FISCAL_OPS_02
- **Priority:** P1
- **Module:** expenditure-resolution
- **Area / category:** core / financial
- **Labels:** module:expenditure-resolution, area:core, type:financial, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- expenditure-resolution/core/src/main/java/com/ho/account/expenditure/application/service/BudgetService.java:40
- expenditure-resolution/core/src/main/java/com/ho/account/expenditure/application/service/BudgetService.java:85
- expenditure-resolution/core/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:101
- expenditure-resolution/core/src/test/java/com/ho/account/expenditure/application/service/BudgetServiceTest.java:72

## Problem statement
지출결의의 `BudgetService.useBudget()`은 연월·부서·계정의 예산 행이 없으면 곧바로 반환한다. `checkBudgetAvailability()`도 같은 경우 정상으로 판단한다. 지출결의 생성·수정은 `useBudget()` 호출 이후 결의서를 저장하므로, 해당 예산이 미설정된 항목은 한도 통제를 전혀 받지 않는다. 이는 별도 `budget` MSA의 집행 잠금 경로가 아니라 지출결의 모듈 내부 `budgets` 테이블 경로의 문제다.

## Reproduction / evidence
`BudgetPersistencePort.findByYearMonthAndDepartmentCodeAndAccountCode()`가 `Optional.empty()`를 반환하도록 하면 `useBudget()`은 저장·예외 없이 종료한다(`BudgetService.java:41-49`). 이어 `ExpenditureResolutionService.createResolution()`은 결의 번호를 만들고 결의서를 저장한다(`ExpenditureResolutionService.java:103-113`). 기존 `BudgetServiceTest.java:72-86`은 미설정 예산에서 `useBudget`과 가용성 확인이 예외 없이 성공해야 한다고 명시적으로 검증한다. 따라서 테스트 누락만이 아니라 현재 코드·테스트가 fail-open 정책을 고정한다.

## Financial / architectural impact
예산 배정 누락, 잘못된 연월·부서·계정 조합, 마스터 데이터와 예산 데이터의 불일치가 무제한 지출결의 생성으로 이어진다. 해당 결의는 이후 승인·전표 생성 경로로 진행할 수 있으므로 예산 한도 통제가 무력화된다.

## Proposed solution
정확한 수정 allowlist (4파일): `expenditure-resolution/core/src/main/java/com/ho/account/expenditure/application/service/BudgetService.java`, `expenditure-resolution/core/src/test/java/com/ho/account/expenditure/application/service/BudgetServiceTest.java`, `expenditure-resolution/core/src/test/java/com/ho/account/expenditure/application/service/ExpenditureResolutionBudgetIntegrationTest.java`, `expenditure-resolution/docs/process-flow.md`. `useBudget`과 사전 가용성 검사에서 미설정 예산을 명시적 업무 예외로 거부한다. 반려·수정의 예산 복원 경로는 이미 차감된 기존 결의만 대상으로 검토해 누락 행을 조용히 무시하지 않도록 함께 확인한다. 초과·정상 경로를 유지하면서 미설정 행의 결의 저장을 막는다.

## Acceptance criteria
- [ ] 미설정 예산 조합의 결의 생성·증액 수정은 실패하고 결의서가 저장되지 않는다.
- [ ] 유효 예산의 한도 내 차감과 한도 초과 거부, 반려 시 복원 동작이 유지된다.
- [ ] 테스트가 `Optional.empty()` 경로를 실패로 검증하고 기능 문서가 미설정 예산 정책을 설명한다.

## Test gap and verification limits
현재 테스트는 미설정 예산의 무검사 성공을 기대하므로 정책 수정 시 테스트를 함께 바꿔야 한다. 검증 명령: `./gradlew :expenditure-resolution:core:test --offline --no-daemon --console=plain`. 병렬 결의 생성의 행 잠금/원자성은 이 이슈의 4파일 범위와 별개로 추가 검증이 필요하다.
