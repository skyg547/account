# [expenditure-resolution] 전표 ID가 없는 응답으로 결의서가 APPROVED 처리됨

- **Issue ID:** FISCAL_OPS_03
- **Priority:** P1
- **Module:** expenditure-resolution
- **Area / category:** core / financial
- **Labels:** module:expenditure-resolution, area:core, type:financial, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- expenditure-resolution/core/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java:193
- expenditure-resolution/core/src/main/java/com/ho/account/expenditure/domain/ExpenditureResolution.java:136
- expenditure-resolution/core/src/main/java/com/ho/account/expenditure/resolution/infrastructure/adapter/HttpExpenditureJournalAdapter.java:75
- expenditure-resolution/core/src/main/resources/db/expenditure-resolution-migration/V1__expenditure_resolution_baseline.sql:12
- expenditure-resolution/core/src/test/java/com/ho/account/expenditure/application/service/ExpenditureResolutionServiceTest.java:136

## Problem statement
`approveResolution()`은 전표 생성 응답의 `journalEntryId`를 검사하지 않고 `resolution.approve()`에 전달한다. 도메인 `approve()`도 ID를 검증하지 않으며, 결의서 테이블의 `journal_entry_id`는 nullable이다. 따라서 원격 포트가 성공 응답으로 `journalEntryId=null`을 반환하면 결의서는 `APPROVED` 상태로 저장되지만 연결된 전표가 없다. `JournalPostingResult` 계약 레코드에는 필드 검증이 없고, HTTP 어댑터는 응답 객체 자체가 null인지 여부만 확인한다. `createDraftEntry`가 `DRAFT` 상태를 반환하는 사실만으로 전표 승인 정책을 추정하지 않는다.

## Reproduction / evidence
`ExpenditureResolutionService.approveResolution()`의 응답 처리(`197-212`)에 `new JournalPostingResult(null, "SLIP-X", "DRAFT")`를 반환하는 포트를 넣으면 `ExpenditureResolution.approve(null)`이 `status=APPROVED`, `journalEntryId=null`을 설정한다(`ExpenditureResolution.java:136-143`). 마이그레이션은 이 컬럼에 NOT NULL 또는 승인 상태와 연계한 CHECK를 두지 않았다(`V1__expenditure_resolution_baseline.sql:9-21`). 현재 서비스 테스트는 양수 ID `100L` 성공만 검증한다(`ExpenditureResolutionServiceTest.java:137-150`). HTTP 어댑터 역시 `result == null`만 검사하고 필드 값은 그대로 반환한다(`HttpExpenditureJournalAdapter.java:75-88`).

## Financial / architectural impact
승인 완료로 표시된 결의서가 원장 전표를 추적할 수 없는 상태가 된다. 후속 AP 지급·감사 조회가 승인 상태를 신뢰하면 전표 누락을 놓칠 수 있고, 같은 결의를 재승인할 수도 없어 수동 복구가 필요해진다.

## Proposed solution
정확한 수정 allowlist (5파일): `expenditure-resolution/core/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java`, `expenditure-resolution/core/src/main/java/com/ho/account/expenditure/resolution/infrastructure/adapter/HttpExpenditureJournalAdapter.java`, `expenditure-resolution/core/src/test/java/com/ho/account/expenditure/application/service/ExpenditureResolutionServiceTest.java`, `expenditure-resolution/core/src/test/java/com/ho/account/expenditure/resolution/infrastructure/adapter/HttpExpenditureJournalAdapterTest.java`, `expenditure-resolution/docs/process-flow.md`. 서비스가 외부 포트 결과의 양수 전표 ID와 기대 가능한 생성 상태를 확인한 후에만 도메인 승인·저장을 실행한다. HTTP 어댑터도 200 응답의 필수 필드가 비거나 잘못되면 명시적 연계 실패로 거부한다. 같은 검증을 local·mock 포트에서도 우회할 수 없도록 core 경계에 남긴다.

## Acceptance criteria
- [ ] null·0·음수 전표 ID 또는 필수 상태가 없는 응답에서 결의서는 `REQUESTED`를 유지하고 저장되지 않는다.
- [ ] 유효한 전표 ID·생성 상태 응답은 기존 결의 승인 및 전표 ID 연결 동작을 유지한다.
- [ ] 서비스·HTTP 어댑터 테스트가 malformed 200 응답과 정상 응답을 각각 검증한다.

## Test gap and verification limits
기존 테스트는 정상 양수 ID만 반환하며 200 응답의 부분 누락, null ID, 잘못된 상태를 검사하지 않는다. 검증 명령: `./gradlew :expenditure-resolution:core:test --offline --no-daemon --console=plain`. 실제 원격 Journal 장애·재시도 원자성은 별도 통합 환경에서 확인해야 하며, 이 이슈는 응답 검증과 거짓 승인 방지 범위로 한정한다.
