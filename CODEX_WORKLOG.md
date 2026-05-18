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

## 2026-05-12 (reconciliation 조정분개 링크 ID 참조 전환)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 직전 작업 후에도 `ReconciliationDifference.adjustmentJournalEntry`가 `JournalEntry` 엔티티 직접 연관이라 `ReconciliationService`에 `JournalEntryRepository.findById`가 남아 있었음.
- 수정 내용:
  - `ReconciliationDifference`의 `JournalEntry` `@ManyToOne`을 제거하고 `adjustment_journal_entry_id` 컬럼을 `Long adjustmentJournalEntryId`로 매핑.
  - `ReconciliationVariance`도 `ADJUSTMENT_JOURNAL_ENTRY_ID`를 `Long adjustmentJournalEntryId`로 매핑.
  - `ReconciliationService`에서 `JournalEntryRepository` 의존성을 제거.
  - 수동 조정분개 ID는 `JournalQueryPort.getJournalSummary`로 존재 확인 후 ID만 저장하도록 변경.
  - 자동 조정분개 생성은 `JournalPostingPort` 결과의 `journalEntryId`를 그대로 차이에 저장.
  - `ReconciliationDifferenceDto`, `VarianceDto`, `ReconciliationServiceTest`를 ID 참조 기준으로 갱신.
  - `reconciliation/docs`에 조정분개 링크가 전표 ID 참조임을 명시.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `AutomatedMatchingEngine`는 아직 `journal-ledger`의 `JournalDetail` 도메인 타입을 입력 모델로 사용함.
  - 조정분개 계정 산정은 설정 기반이며, 업무별 정책 객체로는 아직 분리되지 않음.

## 2026-05-12 (reconciliation 자동 매칭 계약 DTO 전환)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 조정분개 링크 ID 전환 후 `reconciliation` 메인 소스에서 남은 `journal-ledger` 도메인 직접 의존은 `AutomatedMatchingEngine`의 `JournalDetail` 입력 모델이었음.
- 수정 내용:
  - `JournalDetailSummary`에 `accountingDate` 필드를 추가.
  - `MonolithJournalQueryAdapter`가 전표 상세 요약에 회계일자를 채우도록 변경.
  - `AutomatedMatchingEngine`이 `JournalDetail` 대신 `JournalDetailSummary`를 입력받도록 변경.
  - 금액 비교는 `baseAmount` 우선, 없으면 `amount`를 사용하도록 보완.
  - `reconciliation/build.gradle`에서 `journal-ledger:core` 직접 의존성을 제거.
  - `AutomatedMatchingEngineTest`를 추가해 계약 DTO 기반 exact match와 date mismatch를 검증.
  - `reconciliation/docs`의 자동 매칭 설명을 `JournalDetailSummary` 기준으로 갱신.
- 실행 명령:
  - `.\gradlew :contracts:compileJava :journal-ledger:core:compileJava :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 원천/SOURCE 집계는 아직 외부 시스템 조회가 아니라 설정값 기반.
  - 자동 매칭 조건은 여전히 금액과 일자 정확히 일치만 지원.
  - 조정분개 계정 산정은 설정 기반이며, 업무별 정책 객체로는 아직 분리되지 않음.

## 2026-05-12 (reconciliation 자동 매칭 허용오차 옵션 추가)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - 직전 작업으로 `AutomatedMatchingEngine`의 DTO 의존성은 정리됐지만, 금액과 일자가 조금만 달라도 실패하는 운영 리스크가 남아 있었음.
- 수정 내용:
  - `AutomatedMatchingEngine.MatchOptions`를 추가해 금액 허용오차와 일자 허용일수를 입력받도록 확장.
  - 기존 `match(List<BankStatement>, List<JournalDetailSummary>)`는 `MatchOptions.exact()`로 위임해 기존 완전일치 동작을 유지.
  - 매칭 사유를 상수화하고, 완전일치와 허용오차 매칭을 각각 `EXACT_DATE_AMOUNT_MATCH`, `TOLERANCE_DATE_AMOUNT_MATCH`로 구분.
  - 금액 비교는 은행 입금 우선/출금 대체, 전표 `baseAmount` 우선/`amount` 대체 규칙을 유지하고 null 금액을 방어.
  - `AutomatedMatchingEngineTest`에 허용오차 매칭 성공, 허용오차 초과 실패, 음수 옵션 거부 테스트를 추가.
  - `reconciliation/docs`에 기본 완전일치와 `MatchOptions` 기반 허용오차 매칭 기준을 반영.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
  - `git diff --check -- reconciliation/src/main/java/com/ho/account/reconciliation/service/AutomatedMatchingEngine.java reconciliation/src/test/java/com/ho/account/reconciliation/service/AutomatedMatchingEngineTest.java reconciliation/docs/README.md reconciliation/docs/process-flow.md reconciliation/docs/beginner-guide.md reconciliation/docs/schema.md`
- 결과:
  - `BUILD SUCCESSFUL`.
  - `git diff --check`는 오류 없이 종료했고 CRLF 변환 경고만 출력됨.
- 남은 리스크:
  - `ReconciliationRule`의 tolerance 필드를 실제 `MatchOptions`로 변환해 사용하는 통합 호출 경로는 아직 없음.
  - 설명문구 유사도, 전표번호, 계좌번호 등 복합 매칭 조건은 아직 없음.
  - 원천/SOURCE 집계는 아직 외부 시스템 조회가 아니라 설정값 기반.

## 2026-05-12 (reconciliation 메인 대사 규칙 허용오차 적용)
- 사용자 요청: 계속 작업 진행.
- 기준 판단:
  - `ReconciliationRule`에는 `toleranceType/toleranceValue`가 있지만 `performReconciliation`이 규칙을 조회만 하고 집계 비교에 사용하지 않았음.
- 수정 내용:
  - `ReconciliationTolerancePolicy` 도메인 정책을 추가해 금액 허용오차 계산을 서비스 흐름에서 분리.
  - 우선순위 순서로 전달된 규칙 중 첫 활성 규칙의 허용오차를 적용.
  - `ABSOLUTE`는 금액 그대로, `PERCENTAGE`는 원천 금액 기준 비율로 허용오차를 계산.
  - `performReconciliation`이 원천/대상 금액 차이가 허용오차를 초과할 때만 `AMOUNT_MISMATCH` 차이를 만들도록 변경.
  - 허용오차 이내인 경우 차이, 사유코드, 조정분개를 생성하지 않고 matched 집계를 채우도록 보완.
  - `ReconciliationTolerancePolicyTest`와 `ReconciliationServiceTest`를 추가/갱신.
  - `reconciliation/docs`에 메인 대사 집계 비교의 허용오차 적용 기준을 문서화.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 라인 단위 `AutomatedMatchingEngine.MatchOptions`는 아직 저장된 `ReconciliationRule.ruleDefinitionJson`과 자동 연결되지 않음.
  - 설명문구 유사도, 전표번호, 계좌번호 등 복합 매칭 조건은 아직 없음.
  - 원천/SOURCE 집계는 아직 외부 시스템 조회가 아니라 설정값 기반.

## 2026-05-12 (Gemini 리뷰 후속 확인 및 reconciliation 인코딩 정리)
- 사용자 요청: 작업을 수행하고 `GEMINI_REVIEW_PROMPT.md`의 Gemini 리뷰 내용을 확인.
- 확인 내용:
  - `GEMINI_REVIEW_PROMPT.md`는 리뷰 결과가 아니라 Gemini에게 전달할 표준 리뷰 프롬프트/핸드오프 문서임.
  - 실제 Gemini 리뷰 결과는 `GEMINI_MODULE_REVIEW.md`에 있으며, `ReconciliationService.java` 인코딩 깨짐, `journal-ledger:core` 직접 의존성, 대량 집계 성능 리스크를 지적함.
- 수정 내용:
  - 현재 코드 기준 `reconciliation`의 `journal-ledger:core` 직접 의존성과 `JournalEntryRepository` 직접 참조는 이미 제거된 상태임을 확인.
  - `ReconciliationService.java`의 깨진 한글 Javadoc/주석을 ASCII 설명으로 정리.
  - 차이 설명과 조정분개 설명에 남아 있던 깨진 문자열을 ASCII 문구로 교체.
  - `GEMINI_REVIEW_PROMPT.md`, `WORKLOG.md`, `CODEX_WORKLOG.md`에 이번 Gemini 리뷰 후속 조치와 남은 리스크를 반영.
- 실행 명령:
  - `rg -n "...mojibake pattern..." reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java`
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - 깨진 인코딩 패턴 검색 결과 없음.
  - 첫 Gradle 실행은 출력 없이 제한 시간 초과.
  - 재실행 결과 `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `buildTargetSnapshot`은 아직 `JournalQueryPort`로 전표 목록과 상세를 조회해 루프 합산하므로 Gemini가 지적한 대량 집계 성능 리스크가 남아 있음.
  - `ReconManagerService` SOURCE/INTERFACE는 아직 외부 시스템 조회가 아니라 설정값 기반.
  - 다른 모듈의 더미 계정/하드코딩 리스크는 이번 범위에서 제외.

## 2026-05-12 (handoff 대사 대상 집계 성능 개선)
- 사용자 요청: `CODEX_HANDOFF_TASKS.md`의 검수 과제를 계획으로 쪼개 작업하고 완료 처리.
- 기준 판단:
  - Critical/High 첫 항목인 `ReconciliationService.buildTargetSnapshot` 대량 집계 병목은 최근 Gemini 리뷰의 남은 성능 리스크와도 일치함.
- 수정 내용:
  - `JournalDetailAggregateSummary` 계약 DTO 추가.
  - `JournalQueryPort.getJournalDetailAggregate(LocalDate, LocalDate, JournalSide)` 추가.
  - `JournalDetailRepository.summarizeByAccountingDateBetweenAndSide` JPQL 집계 쿼리 추가.
  - `MonolithJournalQueryAdapter`가 Repository 집계 결과를 `JournalDetailAggregateSummary`로 변환하도록 구현.
  - `ReconciliationService.buildTargetSnapshot`이 `getJournalSummaries`/`getJournalDetails` 루프 대신 집계 포트를 호출하도록 변경.
  - `MonolithJournalQueryAdapterTest` 추가, `ReconciliationServiceTest` 갱신.
  - `CODEX_HANDOFF_TASKS.md`의 해당 과제를 `[x]` 완료로 표시.
  - `contracts/docs/README.md`에 Journal Query 집계 흐름을 문서화.
- 실행 명령:
  - `.\gradlew :contracts:compileJava :journal-ledger:core:test :reconciliation:test --console=plain --max-workers=1`
  - `git diff --check -- CODEX_HANDOFF_TASKS.md contracts/src/main/java/com/ho/account/contracts/journal/JournalDetailAggregateSummary.java contracts/src/main/java/com/ho/account/contracts/journal/JournalQueryPort.java contracts/docs/README.md journal-ledger/core/src/main/java/com/ho/account/common/adapter/MonolithJournalQueryAdapter.java journal-ledger/core/src/main/java/com/ho/account/journalledger/domain/journal/repository/JournalDetailRepository.java journal-ledger/core/src/test/java/com/ho/account/common/adapter/MonolithJournalQueryAdapterTest.java reconciliation/src/main/java/com/ho/account/reconciliation/service/ReconciliationService.java reconciliation/src/test/java/com/ho/account/reconciliation/service/ReconciliationServiceTest.java`
- 결과:
  - `BUILD SUCCESSFUL`.
  - `git diff --check`는 오류 없이 종료했고 CRLF 변환 경고만 출력됨.
- 남은 리스크:
  - 다음 handoff 항목인 `ReconManagerService` SOURCE/INTERFACE 외부 데이터 연동 미흡은 아직 남아 있음.
  - `journal-ledger/docs`, `reconciliation/docs` 일부 파일은 작업 전부터 삭제 상태였고 복구하지 않음.

## 2026-05-12 (handoff 대사 외부 스냅샷 연동)
- 사용자 요청: `CODEX_HANDOFF_TASKS.md`의 미완료 작업을 계획으로 쪼개 단계별 진행하고 완료 처리.
- 기준 판단:
  - Critical/High에서 남은 다음 항목은 `ReconManagerService`의 `SOURCE`/`INTERFACE` 단계가 `matchingRulesJson` 고정 집계값에 의존하는 문제였음.
- 수정 내용:
  - `ExternalReconSnapshotPort`, `ExternalReconSnapshotRequest`, `ExternalReconSnapshot`를 추가해 외부 원천/인터페이스 집계 계약을 애플리케이션 포트로 분리.
  - `ExternalReconStageRecord`와 `ExternalReconStageRecordRepository`를 추가해 `RECON_EXTERNAL_STAGE_RECORD` 스테이징 데이터를 `unitId`, `stageCode`, 대사일, 상품/통화/법인 기준으로 DB 단 집계.
  - `ExternalReconStageSnapshotAdapter`를 추가해 Mock/스테이징 외부 시스템 집계를 포트 구현으로 제공.
  - `ReconManagerService`가 `SOURCE`/`INTERFACE` 스냅샷을 `matchingRulesJson`에서 직접 읽지 않고 `ExternalReconSnapshotPort.loadSnapshot`으로 가져오도록 변경.
  - `ReconManagerServiceTest`는 외부 포트 호출과 단계별 결과를 검증하도록 갱신.
  - `ExternalReconStageSnapshotAdapterTest`를 추가해 Repository 집계 매핑과 null projection 방어를 검증.
  - `CODEX_HANDOFF_TASKS.md`의 해당 과제를 `[x]` 완료로 표시.
  - `reconciliation/README.md`에 외부 스냅샷 포트와 스테이징 집계 흐름을 문서화.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `ReconciliationService`의 단순 메인 대사 원천 집계는 아직 `criteriaJson` 기반이며 이번 `ReconManagerService` 항목 범위 밖.
  - 심화 대사의 JOURNAL 단계는 아직 전표 상세 루프 집계 경로라, 필요 시 별도 포트 집계로 최적화할 수 있음.
  - `reconciliation/docs` 일부 파일은 작업 전부터 삭제 상태였고 복구하지 않음.

## 2026-05-12 (handoff loan 전표 수렴/계정 설정화)
- 사용자 요청: `CODEX_HANDOFF_TASKS.md`의 미완료 작업을 단계별 진행.
- 기준 판단:
  - Loan 항목은 `POSTED` 수렴 검증, `Loan`/`LoanContract` 모델 통합, 하드코딩 계정 제거가 묶인 큰 작업이므로, 우선 회계 영향이 큰 전표 수렴과 계정코드 설정화부터 분리 수행.
- 수정 내용:
  - `LoanAccountingProperties`를 추가해 현금, 대출채권, 이연자산, 인식수익, 미수이자, 이자수익 계정코드를 `account.loan.accounting.*` 설정으로 분리.
  - `LoanService`의 대출 실행/재계산/이연 항목 전표가 설정 계정코드를 조회하도록 변경.
  - `LoanService.createAutomatedJournalEntry`가 전표 생성 후 `approveJournalEntry`와 `postJournalEntry`를 호출하고 전기 후 전표를 재조회하도록 변경.
  - `InterestAccrualService`도 설정 계정코드를 사용하고 일일 이자 발생 전표를 생성 후 승인/전기하도록 변경.
  - `LoanServiceTest`를 갱신하고 `InterestAccrualServiceTest`를 추가해 설정 계정 사용, 전표 생성, 승인, 전기 호출 순서를 검증.
  - `CODEX_HANDOFF_TASKS.md`의 Loan 항목은 아직 `[ ]`로 유지하고 완료된 하위 작업/남은 모델 통합 및 통합 E2E 범위를 명시.
  - `loan/README.md`에 설정 키와 현재 보강 범위를 문서화.
- 실행 명령:
  - `.\gradlew :loan:core:test --console=plain --max-workers=1`
  - `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1`
- 결과:
  - 두 명령 모두 `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `Loan`/`LoanContract` 병행 모델 통합은 아직 미완료.
  - 실제 `journal-ledger` 모듈까지 포함한 E2E 테스트는 아직 없고, 현재는 `JournalUseCase` 호출 흐름 단위 테스트로 보강한 상태.
  - `loan:core`의 `journal-ledger:core` 직접 의존은 유지됨.

## 2026-05-12 (handoff master-data SCD2 보강)
- 사용자 요청: `CODEX_HANDOFF_TASKS.md`의 미완료 작업을 단계별 진행.
- 기준 판단:
  - Product/Department에는 SCD2 코드가 일부 있었지만 Product 활성 버전 조회 포트가 없고, 업데이트 시 null 필드가 기존 값을 잃거나 Department `validFrom` 누락 시 실패할 수 있었음.
- 수정 내용:
  - `ProductPersistencePort.findActiveByProductCode` 추가.
  - `ProductRepository.findActiveByProductCode(productCode, date)` JPQL 조회와 최신 이력 조회용 `findByProductCodeOrderByValidFromDesc` 추가.
  - `JpaProductPersistenceAdapter`가 활성 상품 버전 조회를 구현.
  - `ProductService` 생성/조회/수정 경로를 활성 버전 기준으로 보강하고, SCD2 자연키인 상품코드 변경을 차단.
  - `ProductService.updateProduct`가 기존 활성 버전의 `validTo`를 새 시작일 전날로 닫고, 입력이 없는 필드는 기존 값을 보존한 신규 버전을 저장하도록 변경.
  - `DepartmentService` 생성 시 기본 유효기간 정책을 적용하고, 수정 시 `validFrom` 누락 방어, 코드 변경 방어, 기존 부모/유형/값 보존을 보강.
  - `ProductServiceTest`, `DepartmentServiceTest` 추가.
  - 기존 `MasterDataBatchOrchestratorTest`의 fake product port를 새 포트 계약에 맞게 갱신.
  - `CODEX_HANDOFF_TASKS.md`의 Master-Data SCD2 항목을 `[x]` 완료로 표시.
- 실행 명령:
  - `.\gradlew :master-data:test --console=plain --max-workers=1`
- 결과:
  - 첫 실행은 fake port 컴파일 누락으로 실패.
  - fake port 갱신 후 재실행 결과 `BUILD SUCCESSFUL`.
- 남은 리스크:
  - DB 제약으로 유효기간 겹침을 강제 차단하지는 않음.
  - Department Controller에는 update/deactivate endpoint가 없어 서비스/UseCase 중심으로 검증됨.

## 2026-05-12 (handoff governance 승인 연계 정보 보존)
- 사용자 요청: `CODEX_HANDOFF_TASKS.md`의 미완료 작업을 단계별 진행.
- 기준 판단:
  - `MasterDataChangeRequestAdapter`가 governance 승인 정보를 master-data 변경요청으로 넘길 때 `effectiveDate=LocalDate.now()`와 `requestedVersion=1`을 고정으로 사용하고 있었음.
- 수정 내용:
  - `MasterApproval`에 `effectiveDate`, `requestedVersion` 필드 추가.
  - `MasterApprovalUseCase.RequestApprovalCommand`와 `AuditController.MasterApprovalRequest`에 두 필드 추가.
  - `MasterApprovalService.requestApproval`이 요청의 effectiveDate/requestedVersion을 보존하도록 변경하고, 누락 시 기존 호환 기본값을 적용.
  - `MasterDataChangeRequestAdapter`가 승인 엔티티의 effectiveDate/requestedVersion을 `MasterDataChangeRequestCommand`에 전달하도록 변경.
  - `MasterApprovalServiceTest`에 요청값 보존 테스트 추가.
  - `MasterDataChangeRequestAdapterTest`를 추가해 adapter가 master-data 변경요청 커맨드에 값을 그대로 전달하는지 검증.
  - `CODEX_HANDOFF_TASKS.md`의 Governance 승인 연계 정보 유실 항목을 `[x]` 완료로 표시.
- 실행 명령:
  - `.\gradlew :governance:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 신규 `MASTER_APPROVAL` 컬럼을 운영 DB에 반영할 migration은 아직 별도로 없음.
  - `AuditController` DTO 전환과 `TracingService` 포트 우회는 별도 handoff 항목으로 남아 있음.

## 2026-05-12 (handoff expenditure master lookup 검수)
- 사용자 요청: `CODEX_HANDOFF_TASKS.md`의 미완료 작업을 단계별 진행.
- 기준 판단:
  - `ExpenditureResolutionService.buildJournalEntry`는 이미 부서/계정/거래처/지급계정 조회에 `orElseThrow`를 사용 중이었음.
  - `orElse(null)` 잔존 검색 결과는 DTO Assembler의 응답 이름 표시용 선택 조회로, 불완전 전표 저장 리스크와는 별도였음.
- 수정 내용:
  - `ExpenditureResolutionServiceTest`에 승인 전표 생성 시 부서 누락, 상세 계정 누락, 거래처 누락이 예외를 발생시키는 테스트를 추가.
  - 각 실패 경로에서 `journalUseCase.createJournalEntry`와 `resolutionPersistencePort.save`가 호출되지 않음을 검증.
  - `CODEX_HANDOFF_TASKS.md`의 Expenditure-Resolution 마스터 조회 실패 은닉 항목을 `[x]` 완료로 표시.
- 실행 명령:
  - `.\gradlew :expenditure-resolution:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - DTO 이름 바인딩용 Assembler의 선택 조회는 조회 실패 시 null을 반환할 수 있으나, 전표 생성 정합성과는 분리됨.

## 2026-05-13 (handoff master-data 추가 SCD2 보강)
- 사용자 요청: 검수받은 내용의 실행 계획을 계속 진행하고 완료 표시.
- 기준 판단:
  - `CODEX_HANDOFF_TASKS.md`의 Critical/High 미완료 중 Master-Data 추가 SCD2 항목을 독립 작업 단위로 처리.
- 수정 내용:
  - `BusinessPartner`에 `isValid`/`terminate` 도메인 메서드를 추가하고, SCD2 이력 저장이 가능하도록 코드 유니크 제약을 제거하고 유효기간 인덱스를 추가.
  - `BusinessPartnerRepository`/`BusinessPartnerService`를 활성 버전 기준 조회/중복 체크/수정으로 보강.
  - `BusinessPartnerService.updateBusinessPartner`가 거래처 코드 변경을 차단하고, 누락 필드는 기존 활성 버전 값을 보존해 신규 버전을 생성하도록 변경.
  - `Currency`에 대리키 `id`, `isValid`, `terminate`를 추가하고, 통화코드 활성 버전 조회 구조로 변경.
  - `ExchangeRate`에서 `Currency @ManyToOne` 직접 참조를 제거하고 `fromCurrencyCode`/`toCurrencyCode` 문자열 코드 참조로 전환.
  - `BusinessPartnerServiceTest`, `CurrencyExchangeRateTest`를 추가.
  - `CODEX_HANDOFF_TASKS.md`의 Master-Data 추가 SCD2 항목을 `[x]` 완료로 표시하고 `master-data` 문서를 갱신.
- 실행 명령:
  - `.\gradlew :master-data:test --console=plain --max-workers=1`
- 결과:
  - Gradle 명령 성공.
- 남은 리스크:
  - 운영 DB용 DDL migration은 아직 없음.
  - 레거시 DB 문서의 `business_partner_code` FK 설명은 별도 문서 정리 대상.

## 2026-05-13 (handoff governance SystemUser 참조 분리)
- 사용자 요청: 검수받은 내용의 실행 계획을 계속 진행.
- 기준 판단:
  - `CODEX_HANDOFF_TASKS.md`의 Governance Critical/High 미완료 항목인 `SystemUser -> Department` 직접 참조 제거를 독립 작업 단위로 처리.
- 수정 내용:
  - `SystemUser`에서 `master-data.Department` import, `@ManyToOne`, `@JoinColumn` 직접 참조를 제거.
  - 부서 정보는 `Long departmentId` 값 참조로 저장하도록 변경.
  - `SystemUserTest`를 추가해 department ID 저장과 `department` 직접 `@ManyToOne` 필드 부재를 검증.
  - `CODEX_HANDOFF_TASKS.md`의 Governance 직접 참조 항목을 `[x]` 완료로 표시하고 `governance` 문서를 갱신.
- 실행 명령:
  - `.\gradlew :governance:test --console=plain --max-workers=1`
- 결과:
  - Gradle 명령 성공.
- 남은 리스크:
  - 운영 DB migration은 아직 없음.
  - `security` 패키지는 auth 모듈 이관 대상 legacy 영역으로 남아 있음.

## 2026-05-14 (handoff closing FiscalPeriod 참조 분리)
- 사용자 요청: Gemini handoff 이후 `TOTAL_QUALITY_REPORT.md` 로드맵 2번인 `closing` 리팩토링부터 재개.
- 기준 판단:
  - `ClosingAdjustment`뿐 아니라 `PeriodLock`, `ReopenApproval`, `ValuationBatch`, `ProvisionBatch`도 `master-data`의 `FiscalPeriod`를 `@ManyToOne`으로 직접 참조하고 있었음.
  - JPA 관계 제거만으로는 `ClosingService`가 master-data 도메인 엔티티에 계속 의존하므로, contracts 기반 기간 제어 포트로 분리하는 것이 적절하다고 판단.
- 수정 내용:
  - `contracts`에 `FiscalPeriodControlPort`, `FiscalPeriodRef` 추가.
  - `master-data`에 `MonolithFiscalPeriodControlAdapter` 추가.
  - `closing:core`에서 master-data 직접 dependency 제거.
  - `ClosingAdjustment`, `PeriodLock`, `ReopenApproval`, `ValuationBatch`, `ProvisionBatch`의 `FiscalPeriod @ManyToOne`를 `fiscalPeriodId` 값 참조로 전환.
  - `ClosingService`, Repository/Port, API DTO를 `fiscalPeriodId` 및 contracts 포트 기준으로 갱신.
  - `ClosingServiceTest`에 결산 조정이 회기 ID를 저장하는 회귀 테스트 추가.
  - `CODEX_HANDOFF_TASKS.md`, `TOTAL_QUALITY_REPORT.md`, `INSPECTION_TRACKER.md`를 갱신.
- 실행 명령:
  - `.\gradlew :contracts:compileJava :master-data:compileJava :closing:core:compileJava :closing:api:compileJava --console=plain --max-workers=1`
  - `.\gradlew :closing:core:test --console=plain --max-workers=1`
  - `.\gradlew :master-data:test --console=plain --max-workers=1`
- 결과:
  - 모두 `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `closing` 자동 평가/충당 분개의 더미 계정(`999998`, `999999`)과 하드코딩 금액은 별도 Medium 과제로 남음.
  - 운영 DB에 이미 FK 제약이 있다면 JPA 관계 제거와 별도로 제약/마이그레이션 정책 확인이 필요.

## 2026-05-14 (handoff reconciliation 인코딩 잔여 점검)
- 사용자 요청: 다음 작업 진행.
- 기준 판단:
  - `CODEX_HANDOFF_TASKS.md`의 다음 항목인 reconciliation 엔티티 파일 인코딩 복구를 처리.
  - `ReconciliationDifference.java`는 선행 handoff에서 정상화되어 있었고, 잔여 검색 결과 `ReconciliationVariance.java`의 getter/setter 주석 1건만 mojibake로 확인됨.
- 수정 내용:
  - `ReconciliationVariance.java`의 `Getter 諛?Setter` 주석을 ASCII 설명으로 정리.
  - BOM, 제어문자, 주요 mojibake 패턴을 재검색.
  - `CODEX_HANDOFF_TASKS.md`, `TOTAL_QUALITY_REPORT.md`, `INSPECTION_TRACKER.md` 갱신.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 복합 매칭 조건 확장과 계정 산정 정책 도메인화는 handoff의 별도 Medium 항목으로 남아 있음.

## 2026-05-14 (handoff reporting 목업 연동 제거)
- 사용자 요청: 다음 작업 계속 진행.
- 기준 판단:
  - `CODEX_HANDOFF_TASKS.md`의 Reporting Medium 항목인 목업 연동 제거를 처리.
  - `LedgerClientAdapter`의 하드코딩 잔액과 과거 보고서 반환은 실제 원장 연동을 가리는 목업이므로 제거 대상.
  - `reporting:core`는 `contracts`의 `LedgerQueryPort`만 알면 되므로 `journal-ledger:core` 직접 의존성은 제거하는 것이 헥사고날 경계에 맞음.
- 수정 내용:
  - `LedgerClientAdapter`가 `LedgerQueryPort.getGlBalanceSummaries(baseDate, baseDate, null, null)`를 호출해 기준일 GL 잔액 요약을 읽도록 변경.
  - 조회 결과를 계정코드별 `Map<String, BigDecimal>`로 변환하고, 중복 계정은 합산.
  - `endingBalance`가 없는 요약은 `debitAmount - creditAmount`로 보정.
  - 목업 과거 보고서(`PAST-001`, `ASSET_CASH`) 반환을 제거하고 스냅샷 저장소 미구현 시 `Optional.empty()`를 반환하도록 변경.
  - `reporting/core/build.gradle`에서 `journal-ledger:core` 직접 의존성을 제거.
  - `LedgerClientAdapterTest`를 추가해 실제 포트 호출 인자, 계정별 합산, 보정 계산, 과거 보고서 빈 결과를 검증.
  - `reporting` README/docs, `CODEX_HANDOFF_TASKS.md`, `TOTAL_QUALITY_REPORT.md`, `INSPECTION_TRACKER.md`, `WORKLOG.md` 갱신.
- 실행 명령:
  - `.\gradlew :reporting:core:test --console=plain --max-workers=1`
  - `.\gradlew :reporting:api:compileJava :reporting:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :reporting:api:test :reporting:batch:test --console=plain --max-workers=1`
- 결과:
  - 모두 `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `ReportingService`의 라인 산출은 아직 최소 구현으로, SCD2 기반 `ReportLineMapping` 저장/조회와 복수 보고 라인 산출은 다음 과제.
  - 과거 보고서 스냅샷 저장소가 없으므로 운영 비교 재무제표의 전기 금액은 별도 영속화 어댑터가 필요.

## 2026-05-14 (handoff closing 자동분개 룰 설정화)
- 사용자 요청: 다음 작업 진행.
- 기준 판단:
  - `CODEX_HANDOFF_TASKS.md`의 Common & Closing Medium 항목인 더미 계정/고정 금액 제거를 처리.
  - `ClosingService.runValuationBatch`와 `runProvisionBatch`가 자동 분개 생성 시 `999998`, `999999`, `1000`, `500`을 직접 사용하고 있었음.
- 수정 내용:
  - `ClosingAccountingProperties` 추가.
  - 평가 유형별 `valuation-rules`, 충당 유형별 `provision-rules`에서 차변 계정, 대변 계정, 금액을 읽도록 변경.
  - 설정 누락, 빈 계정, 0 이하 금액이면 자동 분개 생성 전 실패하도록 검증.
  - `ClosingService.createAutomatedJournalEntry`가 설정 룰 기반으로 `JournalLineCommand`를 생성하도록 변경.
  - `ClosingServiceTest`에 평가/충당 배치가 설정 계정/금액을 사용하는지, 룰 누락 시 배치 저장과 전표 생성이 호출되지 않는지 검증 추가.
  - `closing` README/process-flow, `CODEX_HANDOFF_TASKS.md`, `TOTAL_QUALITY_REPORT.md`, `INSPECTION_TRACKER.md`, `WORKLOG.md` 갱신.
- 실행 명령:
  - `.\gradlew :closing:core:test --console=plain --max-workers=1`
  - `.\gradlew :closing:api:compileJava :closing:batch:compileJava --console=plain --max-workers=1`
- 결과:
  - 모두 `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 운영 계정 및 금액은 환경별 설정으로 반드시 주입해야 함.
  - 금액 산출까지 도메인 룰 엔진으로 자동화하는 것은 후속 고도화 범위.

## 2026-05-15 (handoff receivable 웹 DTO 전환)
- 사용자 요청: 다음 작업 진행.
- 기준 판단:
  - `CODEX_HANDOFF_TASKS.md`의 Receivable 웹 어댑터 도메인 노출 항목을 처리.
  - 현재 코드 기준 `Receivable`의 `BusinessPartner @ManyToOne` 직접 참조는 이미 제거되어 있고, 고객은 `customerCode`와 `contracts.masterdata.BusinessPartnerRef`로 다뤄짐을 확인.
- 수정 내용:
  - `SalesController`가 `SalesInvoiceRequest`를 받고 `SalesInvoiceResponse`를 반환하도록 변경.
  - `CollectionController`가 `CollectionRequest`를 받고 `CollectionResponse`를 반환하도록 변경.
  - 수동 매칭 요청을 `Map<String,Object>` 대신 `ManualMatchingRequest`로 전환하고 `amount` 입력명은 `@JsonAlias`로 호환.
  - `SalesInvoiceRequest`, `CollectionRequest`에 도메인 변환 메서드 추가.
  - `CollectionResponse` 추가.
  - `SalesInvoice.create`에 description 전달 overload 추가.
  - `SalesControllerTest`, `CollectionControllerTest` 추가.
  - `receivable` README/process-flow, `CODEX_HANDOFF_TASKS.md`, `TOTAL_QUALITY_REPORT.md`, `INSPECTION_TRACKER.md`, `WORKLOG.md` 갱신.
- 실행 명령:
  - `.\gradlew :receivable:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 기존 수동 매칭 요청의 `amount` 필드는 호환 별칭으로 유지하지만, 신규 API 문서상 권장 필드는 `matchingAmount`.

## 2026-05-18 (handoff governance 웹/추적성 경계 정리)
- 사용자 요청: 작업 재개 전 git 동기화 우선 수행.
- 동기화:
  - `git fetch origin`
  - `git rev-list --left-right --count HEAD...origin/main`
  - 결과: `0 0`으로 로컬 `main`과 `origin/main` 커밋 차이 없음.
- 기준 판단:
  - 현재 코드 기준 `journal-ledger`의 마스터 엔티티 직접 참조 지적은 오래된 문서와 실제 코드가 일부 어긋나 있었음.
  - `governance`에는 `AuditController` DTO 전환 변경이 이미 진행 중이었고, `TracingService`의 `AuditLogRepository` 직접 의존은 실제 잔여 계층 위반으로 확인되어 같은 범위에서 마무리.
- 수정 내용:
  - `TracingService`가 `AuditLogRepository` 대신 `AuditLogPersistencePort`를 주입받아 `findByTargetEntityAndTargetId`를 호출하도록 변경.
  - `TracingServiceTest` 추가.
  - `CODEX_HANDOFF_TASKS.md`의 Governance DTO 전환/TracingService 포트 우회 항목을 `[x]` 완료로 표시.
  - `governance` README/docs, `TOTAL_QUALITY_REPORT.md`, `INSPECTION_TRACKER.md`, `WORKLOG.md` 갱신.
- 실행 명령:
  - `.\gradlew :governance:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `security` 패키지는 auth 모듈 이관 대상 legacy 영역으로 남아 있음.

## 2026-05-18 (handoff journal-ledger 직접 참조/주석 재확인)
- 사용자 요청: handoff 작업 계속 진행.
- 기준 판단:
  - `CODEX_HANDOFF_TASKS.md`의 Journal-Ledger 직접 참조 항목은 이미 `[x]`였지만 설명이 오래되어 실제 코드 상태를 재확인.
  - `JournalRuleEngine`의 "스켈레톤/빈 DRAFT 반환" 주석은 현재 코드에 남아 있지 않았음.
  - 대신 `MonolithJournalPostingCommand`에 코드 값을 엔티티로 변환한다는 오래된 설명이 남아 있어 정리 대상이라고 판단.
- 수정 내용:
  - `MonolithJournalPostingCommand`의 currency/account/department/businessPartner 주석을 "journal-ledger에는 코드 값만 저장하고 유효성은 외부 master-data 포트에서 검증"하는 설명으로 갱신.
  - `CODEX_HANDOFF_TASKS.md`의 Journal-Ledger 직접 참조 및 주석 정합성 항목을 실제 코드 기준으로 갱신.
  - `WORKLOG.md`에 검수/문서 결과 기록.
- 실행 명령:
  - `.\gradlew :journal-ledger:core:test --console=plain --max-workers=1`
- 결과:
  - 첫 실행은 120초 제한으로 timeout.
  - 300초 제한 재실행 결과 `BUILD SUCCESSFUL`.
- 남은 리스크:
  - `contracts`의 `LedgerQueryPort` SL/거래처/부서 단위 조회 확장은 별도 미완료 항목.

## 2026-05-18 (handoff reconciliation 복합 매칭/조정 정책 보강)
- 사용자 요청: yolo 모드로 다음 작업 계속 진행.
- 기준 판단:
  - 워킹트리에 Reconciliation 대사 심화 변경(`AutomatedMatchingEngine`, `ReconciliationAdjustmentPolicy`, `JournalDetailSummary`)이 이미 존재했지만, 품질 문서/워크로그에는 아직 보완 필요로 남아 있었음.
  - 기존 변경은 `ACCOUNT_NO_MATCH` 상수와 옵션이 있으나 실제 계좌번호 매칭 로직이 없었고, 조정 정책이 hardcoded fallback 계정을 다시 도입하고 있어 보완 필요.
- 수정 내용:
  - `JournalDetailSummary`에 `accountNo` 추가.
  - `AutomatedMatchingEngine`의 매칭 판정을 `resolveMatchReason`으로 정리하고, 옵션이 켜진 경우 SlipNo/Description/AccountNo 복합 조건을 적용하도록 보강.
  - 계좌번호는 구분자를 제거하고 대소문자를 정규화해 비교.
  - 기본 `match(...)`의 exact amount/date 매칭 사유가 복합 필드 때문에 바뀌지 않도록 회귀 테스트 추가.
  - `ReconciliationAdjustmentPolicy`에서 숨은 기본 계정 fallback을 제거하고 조정 가능한 대사 단위에 명시 계정이 없으면 실패하도록 변경.
  - `ReconciliationServiceTest`, `AutomatedMatchingEngineTest` 보강.
  - `reconciliation` README/docs, `CODEX_HANDOFF_TASKS.md`, `TOTAL_QUALITY_REPORT.md`, `INSPECTION_TRACKER.md`, `GEMINI_REVIEW_PROMPT.md`, `WORKLOG.md` 갱신.
- 실행 명령:
  - `.\gradlew :contracts:compileJava :journal-ledger:core:compileJava :reconciliation:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - 저장된 `ReconciliationRule.ruleDefinitionJson`을 `AutomatedMatchingEngine.MatchOptions`로 변환해 자동 매칭 호출에 연결하는 통합 경로는 아직 없음.

## 2026-05-18 (handoff contracts LedgerQueryPort 확장)
- 사용자 요청: yolo 모드로 다음 작업 계속 진행.
- 기준 판단:
  - `CODEX_HANDOFF_TASKS.md`의 미완료 항목 중 Contracts & Journal-Ledger의 `LedgerQueryPort` 조회 기능 확장을 처리.
  - `LedgerService`에는 이미 `getSlBalances(startDate, endDate, accountCode, businessPartnerCode, departmentCode, currencyCode)`가 있으므로 contracts 포트와 어댑터 노출을 보강하는 방식이 가장 작은 변경이라고 판단.
- 수정 내용:
  - `LedgerQueryPort.getSlBalanceSummaries` 추가.
  - `LedgerBalanceSummary`에 `businessPartnerCode`, `departmentCode` 필드 추가.
  - `MonolithLedgerQueryAdapter`에 GL/SL 매핑 메서드를 분리하고 SL 잔액 조회 구현 추가.
  - `MonolithLedgerQueryAdapterTest` 추가.
  - `CODEX_HANDOFF_TASKS.md`, `contracts/docs`, `journal-ledger/docs`, `TOTAL_QUALITY_REPORT.md`, `INSPECTION_TRACKER.md`, `GEMINI_REVIEW_PROMPT.md`, `WORKLOG.md` 갱신.
- 실행 명령:
  - `.\gradlew :contracts:compileJava :journal-ledger:core:test --console=plain --max-workers=1`
- 결과:
  - `BUILD SUCCESSFUL`.
- 남은 리스크:
  - SL 잔액 조회는 현재 `LedgerService` 내부 필터/집계 기반이며, 대량 데이터용 전용 Repository 집계 쿼리는 별도 성능 고도화 대상.
