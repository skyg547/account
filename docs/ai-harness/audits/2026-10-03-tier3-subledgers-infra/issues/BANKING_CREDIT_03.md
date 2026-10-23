# [loan] 로컬 전표 어댑터가 승인 요청 없이 작성자 자신으로 DRAFT를 승인한다

- **Issue ID:** BANKING_CREDIT_03
- **Priority:** P1
- **Module:** loan
- **Area / category:** infrastructure / bug
- **Labels:** module:loan, area:infrastructure, type:bug, priority:p1, agent-loop, status:draft, spec-driven

## Code reference
- loan/core/src/main/java/com/ho/account/loan/infrastructure/adapter/LoanJournalAdapter.java:55
- loan/core/src/main/java/com/ho/account/loan/infrastructure/adapter/LoanJournalAdapter.java:85
- journal-ledger/core/src/main/java/com/ho/account/journalledger/domain/journal/domain/JournalEntry.java:277
- journal-ledger/core/src/main/java/com/ho/account/journalledger/domain/journal/domain/JournalEntry.java:308
- loan/core/src/main/java/com/ho/account/loan/service/LoanService.java:105
- loan/core/src/main/java/com/ho/account/loan/service/ScheduledRepaymentService.java:63
- loan/core/src/test/java/com/ho/account/loan/service/LoanJournalPostingFlowTest.java:94

## Problem statement
기본 로컬 `LoanJournalAdapter.post`는 초안을 생성한 직후 승인 요청 전이를 호출하지 않고 `approveJournalEntry`를 호출한다. 승인 actor도 초안 작성자 `command.actor()`와 동일하다. Journal 도메인은 `DRAFT → REQUESTED → APPROVED`와 작성자·승인자 분리를 강제하므로, 이 호출은 DRAFT 상태와 동일인 승인 두 조건 모두에 어긋난다.

## Reproduction / evidence
`account.loan.remote.enabled=false`의 `LoanJournalAdapter` 85–87행은 `createJournalEntry(entry)` 직후 `approveJournalEntry(saved.getId(), command.actor())`를 실행한다. `JournalEntry.requestApproval` 277–290행이 필요한데 호출 경로에 없고, `JournalEntry.approve` 308–316행은 REQUESTED가 아니면 즉시 예외를 던지며 작성자와 승인자가 같아도 거부한다. 실제 `LoanJournalPostingFlowTest` 단독 실행은 1건 실패했고, 예외는 `JournalEntry.approve:311 → LoanJournalAdapter.post:86 → LoanService.disburseLoan:106`에서 `현재 상태: DRAFT`였다. 약정 상환은 63–65행의 예약 커밋 뒤 전표 호출에서 실패해 미확정 예약을 남긴다.

## Financial / architectural impact
로컬 모놀리스 경로에서 대출 실행·이자 발생·상환 분개가 현재 Journal 승인 규칙으로는 완료되지 않는다. 약정 상환은 별도 예약 트랜잭션이 남아 수동 대사 없이는 재시도되지 않는다. 승인 단계만 건너뛰는 우회 수정을 하면 작성자·승인자 분리와 마감 전기 펜스를 훼손할 수 있다.

## Proposed solution
수정 allowlist(5파일): `loan/core/src/main/java/com/ho/account/loan/infrastructure/adapter/LoanJournalAdapter.java`, `loan/core/src/main/java/com/ho/account/loan/service/LoanAccountingProperties.java`, `loan/core/src/test/java/com/ho/account/loan/service/LoanJournalPostingFlowTest.java`, `loan/core/src/test/java/com/ho/account/loan/infrastructure/adapter/LoanJournalAdapterTest.java`(신규), `loan/docs/process-flow.md`. 로컬 어댑터에서 초안 작성자가 승인 요청을 제출한 뒤, 서버 설정에서 관리하는 별도 기계 승인 주체만 승인하도록 순서를 바로잡는다. 승인 주체 설정이 없거나 형식이 잘못됐거나 작성자와 같으면 전표·원장 쓰기를 완료한 척하지 않고 실패시킨다. 유효한 승인 주체 설정이 있어야 로컬 전기가 가능하다는 실행 전제를 문서화한다. 승인 완료 후에만 기존 PostingService를 통해 전기하여 회계기간 재검증과 감사 계보를 보존한다. 원격 HTTP 어댑터의 별도 인증·승인 흐름은 후속 범위로 명시한다.

## Acceptance criteria
- [ ] 유효한 별도 승인 주체가 서버에 설정된 로컬 대출 전표는 `DRAFT → REQUESTED → APPROVED → POSTED` 순서로 전이하고 작성자와 승인자 식별자가 다르다.
- [ ] 승인 주체 누락·동일인·마감 기간·전기 실패에서는 대출 실행 상태/전표 계보를 완료로 저장하지 않으며 약정 상환 예약의 복구 경계를 유지한다.
- [ ] 대출 실행 실제 JournalUseCase 흐름 테스트와 어댑터 실패 테스트가 통과하고, `loan/docs/process-flow.md`가 신원·승인·재시도 절차를 설명한다.

## Test gap and verification limits
첫 선택 실행의 Loan core 146건에는 `LoanJournalPostingFlowTest`가 포함되지 않았다. 이후 `./gradlew :loan:core:test --tests '*LoanJournalPostingFlowTest' --offline --no-daemon --console=plain --max-workers=2`를 실행하니 1건 실패(exit 1)했고, `JournalEntry.approve:311`의 DRAFT 상태 거부를 재현했다. 별도 승인자 부재·올바른 전이·닫힌 기간을 함께 검증하는 회귀는 아직 없다. 수정 후 검증 명령: `./gradlew :loan:core:test :loan:api:test :loan:batch:test --offline --no-daemon --console=plain`; 이 감사에서는 실제 PostgreSQL/Journal 서버, 원격 인증 경로, 분산 장애 복구를 실행하지 않았다.
