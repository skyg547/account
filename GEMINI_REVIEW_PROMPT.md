# Gemini Review Prompt

이 파일은 Codex가 구현을 맡고 Gemini가 독립 리뷰를 맡는 표준 핸드오프 프롬프트다.
Gemini에게 리뷰를 요청할 때 아래 프롬프트를 그대로 전달한다.

## Role Split

- Codex: 구현, 수정, 테스트 보강, 문서/워크로그 갱신, 최종 커밋 담당.
- Gemini: 독립 코드 리뷰 담당.
- Gemini는 사용자가 명시적으로 구현을 요청하지 않는 한 코드를 수정하지 않는다.
- Gemini 리뷰 결과는 Codex가 다시 검토한 뒤 실제 수정 여부를 판단한다.

## Gemini에게 전달할 프롬프트

```text
당신은 account 저장소의 독립 코드 리뷰어입니다.
이번 리뷰에서는 코드를 직접 수정하지 말고, Codex가 작업한 변경분을 검수한 뒤 Findings 중심으로 보고하세요.

1. 먼저 아래 문서를 읽어 현재 프로젝트 규칙과 최근 작업 이력을 확인하세요.
- GEMINI.md
- docs/GEMINI.md
- docs/GEMINI_SKILL.md
- Agents.md
- WORKLOG.md 최신 항목
- CODEX_WORKLOG.md 최신 항목
- MODULE_REVIEW_2026-05-06.md 최신 항목
- docs/todo.md
- 변경 대상 모듈의 README.md 및 docs/*.md

2. 현재 리뷰 대상 범위를 확인하세요.
- git status --short --branch
- git diff --stat
- git diff
- 새 파일이 있으면 해당 파일도 확인

3. 리뷰 기준은 아래 순서로 우선순위를 둡니다.
- 컴파일/테스트 실패를 유발하는 결함
- 회계 금액/차대/상태 전이/마감/승인/라인리지 오류
- 헥사고날 아키텍처 위반: adapter, application, domain, infrastructure 경계 침범
- DDD 위반: 단순 setter 중심 변경, 도메인 규칙의 서비스 산재, 의미 없는 단순 위임
- 배치 규칙 위반: batch 모듈 내 비즈니스 if/for/math 구현
- 대용량 처리 주장과 실제 구현 불일치
- API 계약 회귀: DTO, Controller, Service, Test 불일치
- 테스트 누락 또는 기존 테스트 계약 미갱신
- 문서/WORKLOG/todo 완료 표기와 실제 코드 상태 불일치

4. 검증 명령은 가능한 한 변경 범위에 맞게 실행하세요.
- Gradle은 락 충돌을 피하기 위해 필요한 경우 --max-workers=1을 사용하세요.
- 실패하면 실패한 task, 핵심 에러, 영향 범위를 기록하세요.
- 네트워크나 로컬 환경 문제로 실행하지 못하면 이유를 명확히 적으세요.

5. 출력 형식은 반드시 아래 순서를 따르세요.

Findings:
- Severity: Critical/High/Medium/Low 중 하나
- 파일/라인: 실제 경로와 라인 번호
- 문제: 무엇이 잘못됐는지
- 영향: 운영/회계/빌드/테스트에 어떤 위험이 있는지
- 제안: Codex가 수행할 수정 방향

Verification:
- 실행한 명령
- 성공/실패 결과
- 실패 시 핵심 에러 요약

Open Questions:
- 정책 결정이 필요한 항목만 작성

Notes:
- 이미 WORKLOG 또는 MODULE_REVIEW에 기록된 알려진 이슈는 새 증거가 있을 때만 중복 보고하세요.
- 리뷰는 한국어로 작성하세요.
- 코드 수정은 하지 마세요.
```

## Current Handoff Context

- 현재 로컬 기준 커밋: `e8b3080`
- 현재 로컬 워킹트리에는 미커밋 변경이 남아 있을 수 있다.
- 최근 Codex 구현 범위:
  - `journal-ledger:api`의 `DepartmentPersistencePort.findByCode` 잔존 호출을 `findActiveByCode`로 수정.
  - `JournalEntryService.postJournalEntry`가 `PostingService` 단일 전기 경로로 위임하도록 수정.
  - `PostingService.postJournalEntry(Long, String)`가 도메인 `JournalEntry.post(poster)`로 상태 전이와 감사 사용자 기록을 처리한 뒤 GL/SL 엔트리와 잔액을 생성하도록 보완.
  - `JournalEntryServiceTest`, `PostingServiceTest` 추가.
  - `journal-ledger/docs/process-flow.md`, `journal-ledger/docs/beginner-guide.md` 전기 경로 설명 갱신.
  - `closing:core`의 `determineClosingStatus` null 상태 NPE를 제거하고 `ClosingCalendar.validateReadyToClose` 도메인 검증을 추가.
  - `ClosingServiceTest`의 마감 완료 판정 테스트 6건이 통과하도록 감사 로그 포트 mock을 보강.
  - `master-data`의 깨진 주석 5개 파일을 UTF-8/ASCII 설명으로 복구해 `unmappable character` 컴파일 진단을 제거.
  - `loan:core`의 자동 전표 생성 경로를 `JournalUseCase.createJournalEntry`로 통일하고 `LoanServiceTest`를 추가.
  - `reconciliation` 메인 대사 흐름의 하드코딩 더미 금액을 제거하고 `criteriaJson` 원천 집계값 + `JournalQueryPort` 대상 전표 집계로 전환.
  - `ReconciliationServiceTest` 추가 및 `reconciliation/docs` 구현 설명 갱신.
  - `reconciliation` 테스트 런타임의 Spring Cloud 전이 의존성 버전 해석을 위해 Spring Cloud BOM 추가.
  - `reconciliation` 조정분개 계정 `121000`/`999999` 하드코딩을 제거하고 `criteriaJson`의 `adjustmentDebitAccountCode`, `adjustmentCreditAccountCode`를 사용하도록 변경.
  - 자동 생성되는 `GENERIC_MISMATCH` 기본 사유코드는 `adjustable=false`로 생성하도록 변경.
  - `contracts`에 `LedgerQueryPort`, `LedgerBalanceSummary` 추가.
  - `journal-ledger:core`에 `MonolithLedgerQueryAdapter` 추가.
  - `ReconManagerService` 심화 대사 흐름의 더미 금액을 제거하고 SOURCE/INTERFACE 설정값, JOURNAL `JournalQueryPort`, LEDGER `LedgerQueryPort` 집계로 전환.
  - `ReconManagerServiceTest` 추가 및 `reconciliation/docs` 심화 대사 설명 갱신.
  - `JournalPostingAdapter`가 `JournalEntryCommand`의 누락 필드와 라인 `baseAmount`를 반영하도록 보완.
  - `ReconciliationService` 자동 조정분개 생성을 직접 Repository 저장에서 `JournalPostingPort.createDraftEntry` 호출로 전환.
  - `JournalPostingAdapterTest`, `ReconciliationServiceTest` 갱신.
  - `ReconciliationDifference`, `ReconciliationVariance`의 조정분개 링크를 `JournalEntry` 엔티티 직접 연관에서 전표 ID 참조로 전환.
  - `ReconciliationService`의 `JournalEntryRepository` 의존성 제거 및 수동 조정분개 ID 검증을 `JournalQueryPort` 기반으로 변경.
  - `JournalDetailSummary`에 `accountingDate` 추가.
  - `AutomatedMatchingEngine` 입력 타입을 `JournalDetail`에서 `JournalDetailSummary`로 전환하고 `reconciliation`의 `journal-ledger:core` 직접 의존성을 제거.
  - `AutomatedMatchingEngine.MatchOptions`를 추가해 기본 완전일치 동작은 유지하면서 금액 허용오차와 일자 허용일수 기반 매칭을 지원.
  - `AutomatedMatchingEngineTest`에 허용오차 매칭 성공, 허용오차 초과 실패, 음수 옵션 거부 테스트를 추가.
  - `ReconciliationTolerancePolicy` 도메인 정책을 추가해 `ReconciliationRule.toleranceType/toleranceValue`를 메인 대사 집계 비교에 적용.
  - `performReconciliation`은 원천/대상 금액 차이가 활성 규칙의 허용오차를 초과할 때만 `AMOUNT_MISMATCH` 차이를 생성.
  - `ReconciliationTolerancePolicyTest`, `ReconciliationServiceTest`를 추가/갱신해 절대/퍼센트 허용오차와 허용오차 이내 차이 미생성을 검증.
  - `GEMINI_MODULE_REVIEW.md`의 Reconciliation 지적을 재확인했고, `ReconciliationService.java`의 깨진 한글 주석/문자열을 ASCII 설명으로 정리.
- 최근 검증:
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:compileJava --console=plain --max-workers=1`
  - `.\gradlew :journal-ledger:core:test --console=plain --max-workers=1 --rerun-tasks`
  - `.\gradlew :journal-ledger:api:compileJava --console=plain --max-workers=1 --rerun-tasks`
  - `.\gradlew :closing:core:test --console=plain --max-workers=1`
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:compileJava :closing:core:test --console=plain --max-workers=1`
  - `.\gradlew :master-data:compileJava --console=plain --max-workers=1 --rerun-tasks`
  - `.\gradlew :loan:core:test --console=plain --max-workers=1`
  - `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
  - `.\gradlew :contracts:compileJava :journal-ledger:core:compileJava :reconciliation:test --console=plain --max-workers=1`
  - `.\gradlew :journal-ledger:core:test :reconciliation:test --console=plain --max-workers=1`
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
  - `.\gradlew :contracts:compileJava :journal-ledger:core:compileJava :reconciliation:test --console=plain --max-workers=1`
  - 결과는 모두 `BUILD SUCCESSFUL`.
  - `git diff --check -- reconciliation/src/main/java/com/ho/account/reconciliation/service/AutomatedMatchingEngine.java reconciliation/src/test/java/com/ho/account/reconciliation/service/AutomatedMatchingEngineTest.java reconciliation/docs/README.md reconciliation/docs/process-flow.md reconciliation/docs/beginner-guide.md reconciliation/docs/schema.md`
  - 결과는 오류 없이 종료했고 CRLF 변환 경고만 출력됨.
  - `rg -n "...mojibake pattern..." reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java`
  - 결과는 매칭 없음.
- 알려진 이슈:
  - `master-data` UTF-8 인코딩 진단은 최근 `:master-data:compileJava --rerun-tasks` 기준 재현되지 않았다.
  - `loan`은 여전히 `Loan`/`LoanContract` 병행 모델과 계정코드 하드코딩이 남아 있다.
  - `reconciliation` 메인 흐름은 더미 금액을 제거했지만, 원천 집계는 아직 `criteriaJson` 명시값 기반이다.
  - `reconciliation` 조정분개 링크는 전표 ID 참조로 전환됐고, `ReconciliationService`의 `JournalEntryRepository` 의존성은 제거됐다.
  - `reconciliation` 업무별 차/대 계정 산정 정책은 아직 별도 도메인 정책으로 분리되지 않았다.
  - `AutomatedMatchingEngine`는 `JournalDetailSummary` 계약 DTO 기반으로 전환됐고, `reconciliation`의 `journal-ledger:core` 직접 의존성은 제거됐다.
  - `performReconciliation`의 메인 집계 비교는 `ReconciliationRule.toleranceType/toleranceValue`를 사용한다.
  - `AutomatedMatchingEngine`는 `MatchOptions`로 금액/일자 허용오차를 받을 수 있지만, 저장된 `ReconciliationRule.ruleDefinitionJson`의 라인 단위 매칭 조건을 이 옵션으로 변환하는 통합 호출 경로는 아직 없다.
  - 자동 매칭은 설명문구 유사도, 전표번호, 계좌번호 등 복합 조건을 아직 사용하지 않는다.
  - `GEMINI_MODULE_REVIEW.md`가 지적한 `ReconciliationService.java` 인코딩 깨짐은 후속 조치로 정리됐다.
  - `GEMINI_MODULE_REVIEW.md`가 지적한 `reconciliation`의 `journal-ledger:core` 직접 의존성과 `JournalEntryRepository` 직접 참조는 제거됐다.
  - `GEMINI_MODULE_REVIEW.md`가 지적한 대량 집계 성능 리스크는 아직 남아 있다.
  - `ReconManagerService` SOURCE/INTERFACE 단계는 더미 금액은 제거됐지만 아직 외부 시스템 조회가 아니라 `matchingRulesJson` 명시 집계값 기반이다.
  - 신규 `LedgerQueryPort`는 GL 잔액 조회만 제공하며 SL/거래처/부서 단위 조회는 아직 없다.
  - 작업 전부터 `contracts`, `master-data`, 문서, `GEMINI_MODULE_REVIEW.md` 등 다른 미커밋 변경이 존재했다.

## Review Handoff Checklist

- 리뷰 전 `git status --short --branch`로 범위를 확인했다.
- 변경 대상 모듈 문서를 읽었다.
- `WORKLOG.md`, `CODEX_WORKLOG.md`, `MODULE_REVIEW_2026-05-06.md` 최신 항목을 확인했다.
- Findings는 요약보다 먼저 작성했다.
- 각 Finding은 파일/라인 근거를 포함했다.
- Gemini는 코드를 수정하지 않았다.
