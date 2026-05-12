# CODEX WORKLOG

## 2026-05-06
- 사용자 요청에 따라 구현/수정 없이 검수만 수행.
- Gemini 변경분 중심으로 diff 리뷰 및 컴파일/테스트 검증 실행.
- 확인 명령:
  - `.\gradlew :expenditure-resolution:compileJava :receivable:compileJava :tax:compileJava :journal-ledger:core:compileJava :closing:core:compileJava --console=plain`
  - `.\gradlew :receivable:compileJava :tax:compileJava :journal-ledger:core:compileJava :closing:core:compileJava --console=plain`
  - `.\gradlew :receivable:test --console=plain`
- 검수 상세 결과는 루트 `WORKLOG.md`의 `2026-05-06 (검수)` 항목에 기록.

## 2026-05-06 (재검수)
- 사용자 요청에 따라 Gemini 작업 완료 여부 재검수.
- 확인 명령:
  - `.\gradlew :expenditure-resolution:compileJava :tax:compileJava :receivable:compileJava :payable:compileJava :journal-ledger:core:compileJava :closing:core:compileJava --console=plain`
  - `.\gradlew :tax:compileJava :receivable:compileJava :payable:compileJava :journal-ledger:core:compileJava :closing:core:compileJava --console=plain`
  - `npm run build` (workdir: `frontend`)
- `WORKLOG.md`에서 완료된 리뷰 항목(해결된 컴파일 이슈) 삭제 반영, 미해결 항목만 유지.

## 2026-05-06 (추가 검수)
- 사용자 요청으로 남은 변경분(원장/자산/WORKLOG) 기준 추가 점검 수행.
- 확인 명령:
  - `.\gradlew :asset-lease:compileJava :journal-ledger:core:compileJava --console=plain`
- 결과:
  - `:asset-lease:compileJava` 성공.
  - `:journal-ledger:core:compileJava` 실패 (`PostingService` 내 `List`, `ArrayList` import 누락).
- `WORKLOG.md`에서 완료된 리뷰 조치 항목 삭제 및 미해결 이슈 중심으로 정리.

## 2026-05-06 (모듈 순차 검수)
- 사용자 요청: 모듈 순차 점검 + DDD/헥사고날 + 주석 + 재무 업무흐름 관점 검토.
- 확인 문서:
  - `receivable/README.md`, `receivable/docs/README.md`, `receivable/docs/process-flow.md`
  - `expenditure-resolution/docs/README.md`, `expenditure-resolution/docs/process-flow.md`
  - `master-data/README.md`
  - `journal-ledger/README.md`, `journal-ledger/docs/README.md`, `journal-ledger/docs/process-flow.md`
- 실행 명령:
  - `.\gradlew :master-data:compileJava --console=plain`
  - `.\gradlew :journal-ledger:core:compileJava --console=plain`
  - `.\gradlew :receivable:compileJava --console=plain`
  - `.\gradlew :expenditure-resolution:compileJava --console=plain`
  - `.\gradlew :tax:compileJava :payable:compileJava :closing:core:compileJava --console=plain`
- 산출물:
  - `MODULE_REVIEW_2026-05-06.md` 신규 생성 (라인 단위 이슈/리스크 정리)
  - `WORKLOG.md`에 검수 요약 항목 추가

## 2026-05-06 (컴파일 블로커 수정 및 재검증)
- 사용자 요청으로 검수 결과 기반 후속 조치 수행.
- 수정 내용:
  - `receivable` 소스 일괄 UTF BOM 제거.
  - `receivable` 테스트 패키지명을 `com.ho.account.receivable.domain`으로 정합화.
  - `expenditure-resolution`의 `DepartmentPersistencePort.findByCode` 호출을 `findActiveByCode`로 수정.
- 실행 명령:
  - `.\gradlew :receivable:compileJava --console=plain`
  - `.\gradlew :expenditure-resolution:compileJava --console=plain`
  - `.\gradlew :receivable:test --console=plain`
  - `.\gradlew :master-data:compileJava :journal-ledger:core:compileJava :receivable:compileJava :expenditure-resolution:compileJava :tax:compileJava :payable:compileJava :closing:core:compileJava --console=plain`
- 결과:
  - 위 명령 전체 성공.
  - `WORKLOG.md`, `MODULE_REVIEW_2026-05-06.md`에 후속 조치 결과 반영.

## 2026-05-06 (Gemini/Codex 통합 최종 검수)
- 사용자 요청: 지금까지의 Gemini/Codex 변경 전체 재검수 및 리뷰 기록.
- 확인 범위:
  - 최근 커밋 `2298c42`, `88e8672`, `08d271f`
  - 영향 모듈 `frontend(closing)`, `journal-ledger`, `receivable`, `expenditure-resolution`, `master-data`
- 실행 명령:
  - `.\gradlew :master-data:compileJava :journal-ledger:core:compileJava :receivable:compileJava :expenditure-resolution:compileJava :tax:compileJava :payable:compileJava :closing:core:compileJava --console=plain`
  - `.\gradlew :receivable:test --console=plain`
  - `npm run build` (workdir: `frontend`)
- 결과:
  - 컴파일/테스트/프론트 빌드는 모두 통과.
  - 남은 이슈는 컴파일 블로커가 아니라 설계/주석/업무흐름/성능 주장 정합성 문제로 정리.
  - `WORKLOG.md`, `MODULE_REVIEW_2026-05-06.md`에 최종 검수 의견 반영.

## 2026-05-06 (전수 검수 1차: 코어/계약 계층)
- 사용자 요청: 디렉토리 단위 단계별 전수 검수 시작.
- 확인 문서:
  - `shared-kernel/README.md`, `shared-kernel/docs/*.md`
  - `contracts/README.md`, `contracts/docs/*.md`
  - `master-data/README.md`, `master-data/docs/*.md`
  - `governance/README.md`, `governance/docs/*.md`
- 실행 명령:
  - `.\gradlew :shared-kernel:compileJava :contracts:compileJava :master-data:test :governance:test --console=plain`
- 결과:
  - 1차 범위 빌드/테스트 성공.
  - `governance -> master-data` 승인 연계에서 `effectiveDate`/`requestedVersion` 유실 확인.
  - `master-data` 여러 소스 파일의 문자열 인코딩 깨짐 확인.
  - `TracingService`의 repository 직접 의존, `AuditController`의 경계 약화 문제 확인.
  - `WORKLOG.md`, `MODULE_REVIEW_2026-05-06.md`에 1차 검수 결과 반영.

## 2026-05-06 (전수 검수 2차: 회계 엔진 계층)
- 사용자 요청: `journal-ledger`, `closing`, `reconciliation`, `reporting` 순차 검수 진행.
- 확인 문서:
  - `journal-ledger/README.md`, `journal-ledger/docs/*.md`, `journal-ledger/core/src/main/java/com/ho/account/journalledger/*/README.md`
  - `closing/docs/*.md`
  - `reconciliation/docs/*.md`
  - `reporting/README.md`, `reporting/docs/*.md`
- 실행 명령:
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:compileJava :closing:core:test :closing:api:compileJava :closing:batch:compileJava :reconciliation:compileJava :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain`
  - `.\gradlew :journal-ledger:api:test :journal-ledger:batch:compileJava :closing:core:test :closing:api:compileJava :closing:batch:compileJava :reconciliation:compileJava :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain`
  - `.\gradlew :journal-ledger:batch:compileJava :closing:core:test :closing:api:compileJava :closing:batch:compileJava :reconciliation:compileJava :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain`
  - `.\gradlew :closing:api:compileJava :closing:batch:compileJava :reconciliation:compileJava :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain`
- 결과:
  - `journal-ledger:core:test` 실패: `JournalRuleEngineTest`의 구 패키지 import 잔존.
  - `journal-ledger:api` 실패: `LedgerController`의 `departmentPersistencePort.findByCode(deptCode)` 호출 잔존.
  - `closing:core:test` 실패: `determineClosingStatus`의 메시지 계약 이탈 + null status NPE 가능성 확인.
  - `closing:api`, `closing:batch`, `reconciliation`, `reporting:*`은 각각 컴파일/테스트 성공.
  - 구조 검수에서 `journal-ledger` 전기 경로 이원화, `reconciliation`의 직접 의존/더미 데이터, `reporting`의 목업 연동 상태를 확인.
- 산출물:
  - `WORKLOG.md`에 2차 검수 요약 추가.
  - `MODULE_REVIEW_2026-05-06.md`에 상세 Findings 추가.

## 2026-05-08 (루트 문서 학습 및 작업 기준 반영)
- 사용자 요청에 따라 루트 Markdown 문서를 우선 확인하고 Codex 작업 기준에 반영.
- 확인 문서:
  - `Agents.md`
  - `WORKLOG.md` 최신 항목
  - `CODEX_WORKLOG.md`
  - `docs/README.md`
  - `BEGINNER_GUIDE.md`
  - `docs/principles_and_policies.md`
  - `docs/business_workflow.md`
  - `docs/domain-catalog.md`
- 반영 기준:
  - 작업 시작 전 대상 모듈 `README.md`, `docs/*.md` 선확인
  - 헥사고날 계층 분리, 배치 오케스트레이터 비즈니스 로직 금지, `BigDecimal` 중심 금액 처리 유지
  - 작업 종료 시 변경 내용, 검증 결과, 남은 리스크를 명확히 보고
- 코드 변경 없음.

## 2026-05-08 (전수 검수 3차: 업무 서브레저/ERP·Banking 계층)
- 사용자 요청: 3차 검수 진행.
- 확인 범위:
  - `tax`
  - `payable`
  - `receivable`
  - `expenditure-resolution`
  - `asset-lease`
  - `loan:core`, `loan:api`, `loan:batch`
  - 연관 변경분 `contracts`, `master-data`
- 확인 문서:
  - `tax/README.md`, `tax/docs/*.md`
  - `payable/README.md`, `payable/docs/*.md`
  - `receivable/README.md`, `receivable/docs/*.md`
  - `expenditure-resolution/docs/*.md`
  - `asset-lease/docs/*.md`
  - `loan/docs/*.md`, `docs/loan_accounting.md`
  - `docs/db/README.md`
- 실행 명령:
  - `.\gradlew :contracts:compileJava --console=plain`
  - `.\gradlew :tax:test --console=plain --max-workers=1`
  - `.\gradlew :payable:test --console=plain --max-workers=1`
  - `.\gradlew :receivable:compileJava --console=plain --max-workers=1`
  - `.\gradlew :expenditure-resolution:test --console=plain --max-workers=1`
  - `.\gradlew :loan:core:compileJava :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :tax:compileJava :receivable:test :asset-lease:test --console=plain --max-workers=1`
- 결과:
  - 통과: `contracts:compileJava`, `tax:compileJava`, `payable:test`, `receivable:test`, `asset-lease:test`, `loan:core/api/batch:compileJava`
  - 실패: `tax:test`, `expenditure-resolution:test`
  - 주요 이슈는 테스트 계약 미갱신, 리스 지급 결의 IFRS16 분개 정책 불완전, 드릴다운 SourceDocumentProvider 문서/구현 불일치, loan 회귀 테스트 부재로 정리.
- 산출물:
  - `WORKLOG.md`에 3차 검수 요약 추가.
  - `MODULE_REVIEW_2026-05-06.md`에 10장 상세 Findings 추가.

## 2026-05-08 (AI 협업 역할 정리 및 Gemini 리뷰 프롬프트 작성)
- 사용자 요청: Codex가 구현을 맡고 Gemini가 리뷰하도록 역할을 정리하고, Gemini에게 전달할 리뷰 방법/프롬프트를 파일로 남김.
- 수정 내용:
  - `Agents.md`에 Codex Implementation Owner Role 추가.
  - `GEMINI.md`에 Gemini 기본 역할을 독립 리뷰어로 명시.
  - `docs/GEMINI.md`에 Gemini 리뷰 역할과 출력 기준 추가.
  - `GEMINI_REVIEW_PROMPT.md` 신규 생성.
  - `WORKLOG.md`에 운영 문서 변경 이력 추가.
- 검증:
  - 문서/운영 지침 변경만 수행.
  - 코드 빌드/테스트는 실행하지 않음.

## 2026-05-08 (3차 검수 Findings 보완 구현)
- 사용자 요청: 검수된 항목을 Codex가 계속 수정하고 작업 로그를 남김.
- 수정 내용:
  - `tax` 테스트를 `TaxInvoice.create(...)` 기반으로 갱신해 setter 제거 후 도메인 계약과 일치시킴.
  - `expenditure-resolution` 테스트를 최신 생성자/`findActiveByCode`/protected 도메인 생성 정책에 맞춤.
  - `ExpenditureResolutionUseCase`에서 DTO 변환 책임을 제거하고 `ExpenditureResolutionDtoAssembler`를 웹 어댑터에 추가.
  - `LeasePaymentResolutionCommand`에 다중 차변 라인 지원을 추가하고, 기존 단일 라인 생성자는 유지.
  - `MonolithLeasePaymentResolutionAdapter`가 리스 지급 결의의 여러 차변 라인을 `ExpenditureDetail` 여러 건으로 생성하도록 변경.
  - `LeaseEntryService`가 IFRS16 리스 지급 시 스케줄의 `interestPortion`/`principalPortion`을 `93100`/`25100` 차변 라인으로 분리 전달하도록 변경.
  - `payable`/`receivable`에 `SourceDocumentProvider` 구현체와 단위 테스트를 추가.
  - 관련 문서와 `.gitignore` 예외를 갱신.
- 실행 명령:
  - `.\gradlew :tax:test :expenditure-resolution:test --console=plain --max-workers=1`
  - `.\gradlew :payable:test :receivable:test --console=plain --max-workers=1`
  - `.\gradlew :contracts:compileJava :asset-lease:test :expenditure-resolution:test --console=plain --max-workers=1 --rerun-tasks`
  - `.\gradlew :tax:test :payable:test :receivable:test :expenditure-resolution:test :asset-lease:test :loan:core:compileJava :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`
- 결과:
  - 위 명령 모두 `BUILD SUCCESSFUL`.
  - 마지막 강제 재실행에서 기존 `master-data` 소스 인코딩 진단이 출력 말미에 섞였으나 Gradle 종료 코드는 성공.
- 남은 리스크:
  - `loan`의 회귀 테스트 부재와 원장 전기 수렴 경로는 아직 미해결.
  - `master-data` 인코딩 깨짐은 별도 정리 대상.

## 2026-05-11 (운영 적용 순서 문서화)
- 사용자 요청: 모듈 실행 순서 문서와 실제 운영 적용 우선순위를 알려달라고 요청.
- 수정 내용:
  - `docs/msa-execution-and-work-plan.md`에 실제 운영 적용 순서 표와 운영 적용 게이트를 추가.
  - `docs/README.md` 문서 허브의 `msa-execution-and-work-plan.md` 설명을 보강.
  - `WORKLOG.md`에 문서화 작업 이력 추가.
- 판단:
  - 실행 순서와 운영 적용 순서를 분리해 정리.
  - 운영 적용 우선순위는 `master-data -> journal-ledger -> tax -> payable/receivable -> expenditure-resolution -> asset-lease`를 우선 후보로 명시.
  - `loan`, `closing`, `reconciliation`, `reporting`은 운영 전 추가 검증 대상으로 명시.
- 검증:
  - 문서 변경만 수행했으며 빌드/테스트는 실행하지 않음.

## 2026-05-11 (journal-ledger 전기 경로 보완)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 최근 검수의 운영 차단 이슈 중 `journal-ledger:api` 컴파일 실패와 전기 경로 불일치를 우선 처리.
- 수정 내용:
  - `LedgerController`의 `DepartmentPersistencePort.findByCode` 잔존 호출을 `findActiveByCode`로 교체.
  - `JournalEntryService.postJournalEntry`가 전표를 먼저 `POSTED`로 만들지 않고 `PostingService` 단일 경로로 위임하도록 변경.
  - `PostingService.postJournalEntry(Long, String)`를 추가하고, 도메인 `JournalEntry.post(poster)`로 상태 검증/전이/auditUser 기록을 수행한 뒤 GL/SL 엔트리와 잔액을 생성하도록 보완.
  - `JournalEntryServiceTest`, `PostingServiceTest`를 추가해 전기 위임과 GL/SL 생성 회귀를 고정.
  - `journal-ledger/docs/process-flow.md`, `journal-ledger/docs/beginner-guide.md`에서 더 이상 존재하지 않는 `/api/ledger/post/{journalEntryId}` 전기 경로 설명을 제거하고 실제 흐름과 맞춤.
- 실행 명령:
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:compileJava --console=plain --max-workers=1`
  - `.\gradlew :journal-ledger:core:test --console=plain --max-workers=1 --rerun-tasks`
  - `.\gradlew :journal-ledger:api:compileJava --console=plain --max-workers=1 --rerun-tasks`
- 결과:
  - 모두 `BUILD SUCCESSFUL`.
  - 신규 테스트 XML 기준 `JournalEntryServiceTest` 1건, `PostingServiceTest` 1건 실패/에러 0건.
- 남은 리스크:
  - 기존 `master-data` UTF-8 인코딩 진단이 Gradle 출력 말미에 계속 표시됨.
  - 작업 전부터 존재한 `contracts`, `master-data`, 문서, `GEMINI_MODULE_REVIEW.md` 등 미커밋 변경은 이번 작업 범위에서 건드리지 않음.

## 2026-05-11 (closing 마감 판정 테스트 보완)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 최근 검수에서 `closing:core` 테스트 실패와 `determineClosingStatus`의 null 상태 NPE가 운영 전 차단 이슈로 남아 있어 처리.
- 수정 내용:
  - `ClosingService.determineClosingStatus`에서 `ClosingCalendar.status`가 null이어도 이전 상태명을 `OPEN`으로 처리해 NPE를 제거.
  - `ClosingCalendar.validateReadyToClose(...)`를 추가해 필수 태스크 미완료와 게이트 미통과를 도메인 규칙으로 구분 검증.
  - 기존 `isReadyToClose(...)`도 null-safe하게 보완.
  - `ClosingServiceTest` 성공 경로에서 감사 로그 포트를 mock으로 명시.
- 실행 명령:
  - `.\gradlew :closing:core:test --console=plain --max-workers=1`
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:compileJava :closing:core:test --console=plain --max-workers=1`
- 결과:
  - 모두 `BUILD SUCCESSFUL`.
  - `ClosingServiceTest` 6건 실패/에러 0건.
- 남은 리스크:
  - `closing` 평가/충당 배치의 더미 계정 사용은 이번 범위에서 미수정.

## 2026-05-11 (master-data 인코딩 진단 정리)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 최근 검증마다 `master-data` 일부 파일의 UTF-8 인코딩 진단이 Gradle 출력 말미에 섞여 결과 해석을 방해해 먼저 정리.
- 수정 내용:
  - `MasterDataChangeRequestCommand`, `AccountSubjectPersistencePort`, `BusinessPartnerPersistencePort`, `MasterDataChangeApplier`, `ExchangeRate`의 깨진 주석을 ASCII 설명으로 교체.
  - 파일 자체를 UTF-8로 재저장해 `unmappable character` 진단 원인을 제거.
  - 소스 로직과 public API는 변경하지 않음.
- 실행 명령:
  - `.\gradlew :master-data:compileJava --console=plain --max-workers=1 --rerun-tasks`
- 결과:
  - `BUILD SUCCESSFUL`.
  - 기존 UTF-8 인코딩 진단은 재현되지 않음.
- 남은 리스크:
  - 작업 전부터 존재한 `BusinessPartnerService`, `ProductService` 미커밋 변경은 건드리지 않음.

## 2026-05-11 (loan 전표 경로 회귀 테스트 보강)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - `loan`은 DoD 완료 표시가 있으나 회귀 테스트 부재와 직접 전표 저장 경로가 남은 리스크로 기록되어 있어 우선 보완.
- 수정 내용:
  - `LoanService`의 자동 전표 생성 경로를 `JournalPersistencePort.save` 직접 호출에서 `JournalUseCase.createJournalEntry` 호출로 변경.
  - `LoanServiceTest`를 추가해 대출 실행 분개가 `LOAN_DISBURSAL` lineage, 차변 `131000`, 대변 `101000`, 동일 금액으로 생성되고 실행 이력에 전표가 연결되는지 검증.
- 실행 명령:
  - `.\gradlew :loan:core:test --console=plain --max-workers=1`
  - `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`
- 결과:
  - 모두 `BUILD SUCCESSFUL`.
  - 신규 `LoanServiceTest` 1건 실패/에러 0건.
- 남은 리스크:
  - `Loan`/`LoanContract` 병행 모델과 하드코딩 계정코드 정책은 이번 범위에서 미수정.
  - 대출 전표가 원장 `POSTED`까지 수렴하는 E2E 검증은 아직 필요.

## 2026-05-11 (reconciliation 메인 대사 더미 금액 제거)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 최근 검수에서 `reconciliation`의 메인 대사 흐름이 더미 금액으로 차이를 생성하고 `journal-ledger` 내부 저장소에 과하게 의존하는 문제가 남아 있어 우선 보완.
- 수정 내용:
  - `ReconciliationService.performReconciliation`에서 하드코딩된 `1000.00`/`950.00` 및 더미 건수를 제거.
  - `ReconciliationUnit.criteriaJson`의 `sourceAmount`, `sourceCount`를 원천 집계값으로 파싱.
  - `JournalQueryPort`로 기준일 전표 요약과 상세를 조회하고 차변 상세의 `baseAmount` 또는 `amount`를 대상 금액으로 집계.
  - 사용되지 않던 `JournalDetailRepository` 주입을 제거하고 `contracts` 의존성을 명시.
  - `reconciliation` 테스트 런타임에서 Spring Cloud 전이 의존성 버전이 결정되도록 Spring Cloud BOM을 추가.
  - `ReconciliationServiceTest`를 추가해 criteria 원천값과 전표 조회 포트 대상값으로 차이 금액/건수/사유코드가 기록되는지 검증.
  - `reconciliation/docs`의 더미 금액 설명을 현재 메인 흐름 기준으로 갱신.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - 첫 실행은 Spring Cloud 전이 의존성 버전 미해석으로 실패.
  - BOM 보강 후 재실행은 `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 원천 집계는 실제 원천 시스템 조회가 아니라 `criteriaJson` 명시값에 의존.
  - 조정분개 생성은 하드코딩 계정과 `journal-ledger` 내부 엔티티/Repository 링크가 남아 있음.
  - `ReconManagerService` 심화 흐름은 여전히 단계별 더미 금액을 반환.

## 2026-05-11 (reconciliation 조정분개 계정 설정화)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 직전 보완 후 남은 리스크 중 조정분개 계정 `121000`/`999999` 하드코딩은 회계 오분개 위험이 크므로 우선 제거.
- 수정 내용:
  - `ReconciliationService`에서 조정 가능한 사유코드의 자동 조정분개 계정을 `criteriaJson`의 `adjustmentDebitAccountCode`, `adjustmentCreditAccountCode`로만 읽도록 변경.
  - 조정 가능한 사유코드인데 계정코드 설정이 없으면 명시적 예외를 발생시키도록 보완.
  - 자동 생성되는 `GENERIC_MISMATCH` 사유코드는 `adjustable=false`로 생성해 기본값만으로 숨은 조정분개가 생기지 않도록 변경.
  - `ReconciliationServiceTest`에 기본 사유코드 비조정 경로와 설정 계정 기반 조정분개 생성 경로를 추가.
  - `reconciliation/docs`의 조정분개 계정 설명을 설정 기반으로 갱신.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 조정분개 링크는 아직 `journal-ledger` 내부 `JournalEntry` 엔티티/Repository 기반.
  - 계정코드 산정 정책은 설정화됐지만, 업무별 정책 객체나 포트로 분리되지는 않음.
  - `ReconManagerService` 심화 흐름은 여전히 단계별 더미 금액을 반환.

## 2026-05-11 (reconciliation 심화 대사 더미 금액 제거)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - `ReconManagerService`가 SOURCE/INTERFACE/JOURNAL/LEDGER 모든 단계에서 더미 금액을 반환하고 있어 심화 대사 결과가 실제 데이터와 무관하게 생성되는 문제가 남아 있었음.
- 수정 내용:
  - `contracts`에 `LedgerQueryPort`, `LedgerBalanceSummary`를 추가.
  - `journal-ledger:core`에 `MonolithLedgerQueryAdapter`를 추가해 `LedgerService.getGlBalances`를 계약 DTO로 노출.
  - `ReconManagerService`에서 더미 `fetch*Amount` 메서드를 제거.
  - SOURCE/INTERFACE는 `ReconUnitDefinition.matchingRulesJson`의 `sourceAmount/sourceCount`, `interfaceAmount/interfaceCount`를 사용하도록 변경.
  - JOURNAL은 `JournalQueryPort`의 전표 상세 차변 금액을 집계하도록 변경.
  - LEDGER는 `LedgerQueryPort`의 GL 잔액을 `ledgerAmountBasis` 기준으로 집계하도록 변경.
  - variance SLA 일수가 null이면 기본 3일을 적용해 NPE를 방지하도록 보완.
  - `ReconManagerServiceTest`를 추가해 4단계 stage 결과와 variance 생성 경로를 고정.
  - `reconciliation/docs`에 심화 대사 집계 방식과 `MATCHING_RULES_JSON` 키를 문서화.
- 실행 명령:
  - `.\gradlew :contracts:compileJava :journal-ledger:core:compileJava :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
  - `MonolithLedgerQueryAdapter`에서 deprecated API 사용 알림이 출력됐으나 빌드 실패는 아님.
- 남은 리스크:
  - SOURCE/INTERFACE는 실제 외부 시스템 조회가 아니라 `matchingRulesJson` 명시 집계값 기반.
  - 신규 `LedgerQueryPort`는 GL 잔액 조회만 제공하며 SL/거래처/부서 단위 조회는 아직 없음.
  - 조정분개 링크는 아직 `journal-ledger` 내부 엔티티/Repository 기반.

## 2026-05-12 (reconciliation 조정분개 생성 포트 경로 전환)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 조정분개 계정은 설정화됐지만, `ReconciliationService`가 여전히 `JournalEntry`/`JournalDetail`을 직접 조립하고 `JournalEntryRepository.save`로 저장해 전표 생성 경로를 우회하고 있었음.
- 수정 내용:
  - `JournalPostingAdapter`가 `JournalEntryCommand`의 `entryType`, `exchangeRate`, `createdBy`, `auditUser`, `lineageSourceType`, `lineageSourceId`, 라인 `baseAmount`를 반영하도록 보완.
  - `JournalPostingAdapterTest`를 추가해 command 필드와 차/대 라인 매핑을 검증.
  - `ReconciliationService`의 자동 조정분개 생성이 `JournalPostingPort.createDraftEntry`를 호출하도록 변경.
  - 생성된 전표 ID는 기존 `ReconciliationDifference.adjustmentJournalEntry` 링크 유지를 위해 `JournalEntryRepository.findById`로 조회해 연결.
  - `ReconciliationServiceTest`를 포트 command 검증 중심으로 갱신.
  - `reconciliation/docs`의 조정분개 생성 설명을 포트 경로 기준으로 갱신.
- 실행 명령:
  - `.\gradlew :journal-ledger:core:test :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `ReconciliationDifference.adjustmentJournalEntry`가 아직 `JournalEntry` 엔티티 직접 연관이라 링크 조회에는 `JournalEntryRepository`가 남아 있음.
  - 완전한 독립성 확보를 위해서는 조정분개 링크를 ID/계약 기반 참조로 바꾸는 후속 변경이 필요.
