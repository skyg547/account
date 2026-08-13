# 📝 Codex 작업 지시서 (Handoff Tasks)

> **문서 목적:** 최근 진행된 리뷰 문서(`CLAUDE_WORKLOG.md`, `CODEX_WORKLOG.md`, `GEMINI_MODULE_REVIEW.md`, `MODULE_REVIEW_2026-05-06.md`, `WORKLOG.md`)를 전수 조사하여, 아직 해결되지 않은 아키텍처 위반, 버그, 하드코딩 이슈, 추가 고도화 과제를 하나도 빠짐없이 종합한 문서입니다. Codex는 본 리스트를 기준으로 작업을 진행해 주세요.

---

## 1. 🚨 Critical / High (우선 해결 과제)

### [Reconciliation (대사)]
- [x] **대량 데이터 성능 최적화:** `buildTargetSnapshot`에서 모든 전표를 루프 돌며 합산하는 방식은 대량 처리 시 성능 병목을 유발합니다. `JournalQueryPort`에 기간별 합계(Sum)를 DB 단에서 직접 집계해 반환하는 메서드를 추가하여 교체하세요. -> *(완료: `JournalDetailAggregateSummary`와 `JournalQueryPort.getJournalDetailAggregate` 추가, `journal-ledger` DB 집계 어댑터 구현, `ReconciliationService` 대상 집계 전환 및 테스트 보강)*
- [x] **외부 데이터 연동 미흡:** `ReconManagerService`의 `SOURCE`/`INTERFACE` 단계가 설정값(`matchingRulesJson`)에 의존하고 있습니다. 실제 외부 원천/인터페이스 시스템(Mock 어댑터 포함)을 통해 데이터를 가져와 집계하도록 전환하세요. -> *(완료: `ExternalReconSnapshotPort`/`ExternalReconSnapshotRequest` 계약 추가, `RECON_EXTERNAL_STAGE_RECORD` 스테이징 집계 어댑터 구현, `ReconManagerService` SOURCE/INTERFACE 포트 호출 전환 및 테스트 보강)*

### [Journal-Ledger & Closing (코어 위반)]
- [x] **[Journal-Ledger] 모듈 간 객체 참조(FK) 위반:** `JournalDetail` 엔티티가 `master-data`의 `AccountSubject`, `BusinessPartner`, `Department`를 `@ManyToOne`으로 직접 참조하고 있습니다. 이를 `String accountCode`, `String deptCode`, `String businessPartnerCode` 등의 ID 기반 참조로 전환하고 Port를 통해 정합성을 검증하도록 수정하세요. -> *(완료/재확인: `JournalDetail`, `JournalEntry`, `GlEntry`, `SlEntry`, `GlBalance`, `SlBalance`, `UnsettledItem`의 master-data 엔티티 직접 import/@ManyToOne은 제거되어 있고 코드 값 참조를 사용. 남은 `@ManyToOne`은 journal-ledger 내부 전표/룰/미결 관계. stale 엔티티 변환 주석도 코드 값 저장 설명으로 정리. `:journal-ledger:core:test` 성공)*
- [x] **[Closing] 모듈 간 객체 참조(FK) 위반:** `ClosingAdjustment` 엔티티가 `master-data`의 `FiscalPeriod`를 직접 참조하고 있습니다. ID 기반 참조로 수정하세요. -> *(완료: `ClosingAdjustment`, `PeriodLock`, `ReopenApproval`, `ValuationBatch`, `ProvisionBatch`의 `FiscalPeriod @ManyToOne`를 `fiscalPeriodId` 값 참조로 전환. `contracts`에 `FiscalPeriodControlPort`/`FiscalPeriodRef`를 추가하고 `master-data`의 `MonolithFiscalPeriodControlAdapter`로 구현해 `closing:core`의 master-data 직접 의존성을 제거. DTO/Repository/Service/Test 갱신 및 `:contracts:compileJava :master-data:compileJava :closing:core:compileJava :closing:api:compileJava`, `:closing:core:test`, `:master-data:test` 성공)*.

### [Loan (대출)]
- [x] **E2E 회귀 테스트 부재 및 전표 수렴 완결성:** 대출 전표가 원장(`POSTED`)까지 수렴하는 과정을 증명하는 E2E 검증 테스트가 부족합니다. 또한 `Loan`과 `LoanContract` 병행 모델을 통합하고, 코드 내 하드코딩된 계정코드를 제거하세요.
  - 진행: `LoanService`/`InterestAccrualService` 자동 전표가 생성 후 `approveJournalEntry`와 `postJournalEntry`까지 호출하도록 보강했고, 대출 회계 계정코드는 `LoanAccountingProperties` 설정으로 분리했습니다. `LoanServiceTest`, `InterestAccrualServiceTest`로 POSTED 수렴 호출과 설정 계정 사용을 검증했습니다.
  - 추가 진행: 별도 계약 엔티티/저장소/DTO 잔재를 제거하고 `Loan` 단일 모델로 일일 이자 발생, 상각 스케줄, 원천 문서 조회 경로를 통합했습니다. `LoanAccountingProperties`의 코드 기본값도 제거해 설정 누락 시 자동 전표 생성 전에 실패하도록 보강했습니다.
  - 완료: `LoanJournalPostingFlowTest`를 추가해 대출 실행이 실제 `JournalEntryService`/`PostingService` 경로를 지나 전표 `POSTED`, GL/SL 엔트리 저장 호출, 원장 잔액 갱신 호출까지 수렴함을 검증했습니다.
  - 완료: `V30__init_loan_schema.sql` Flyway migration과 `LoanFlywayMigrationTest`를 추가해 `Loan` 단일 모델 기준 스키마가 H2 MySQL 모드에서 적용되는지 검증했습니다.
  - 잔여 리스크: 현재 E2E는 서비스 통합 테스트로 JPA Repository는 mock/fake 기반입니다. 여러 모듈을 한 런타임 classpath에서 Flyway 기본 스캔할 경우 기존 `V1` migration 중복을 정리하거나 모듈별 location 설정이 필요합니다.

### [Master-Data (기준 정보)]
- [x] **SCD2 완전 적용 미흡:** `Product` 및 `Department` 모듈에서 데이터 변경 시 기존 행을 직접 덮어쓰고 있어 SCD2(이력 관리) 정책을 위반하고 있습니다. 신규 버전을 생성하고 이전 버전의 `validTo`를 닫는 진정한 의미의 SCD2 로직으로 전면 수정하세요. -> *(완료: `Product` 활성 버전 조회 포트/Repository 추가, Product/Department 업데이트 시 기존 활성 버전 종료 및 신규 버전 생성/기존값 보존/코드 변경 방어 보강, SCD2 회귀 테스트 추가)*
- [x] **추가 SCD2 및 참조 위반:** `BusinessPartner`와 `Currency` 엔티티에 `terminate()` 메서드를 추가하여 SCD2 정책을 완전히 준수하도록 수정하세요. 또한 `ExchangeRate`가 `Currency`를 `@ManyToOne`으로 직접 참조하고 있는 부분을 분리하여 ID 기반 참조 구조로 개선하세요. -> *(완료: `BusinessPartner`/`Currency`에 `isValid`/`terminate` 추가, 거래처 활성 버전 조회/중복 체크 및 SCD2 신규 버전 생성 보강, `Currency` 대리키 기반 SCD2 구조 전환, `ExchangeRate`의 `Currency @ManyToOne` 제거 및 통화코드 참조 전환, 회귀 테스트 추가)*

### [Governance (권한/감사)]
- [x] **승인 연계 시 마스터 데이터 정보 유실:** `master-data` 변경요청에 대해 `governance` 모듈이 승인 시, 기존의 `effectiveDate`와 `requestedVersion`을 무시하고 `LocalDate.now()`와 고정 버전(1)으로 덮어써 이력이 훼손되는 문제를 수정하세요. -> *(완료: `MasterApproval`/요청 커맨드/API 요청에 `effectiveDate`, `requestedVersion` 추가, `MasterDataChangeRequestAdapter`가 승인 요청 값을 보존하도록 변경, 회귀 테스트 추가)*
- [x] **모듈 간 객체 참조(FK) 위반:** `SystemUser` 엔티티가 `master-data`의 `Department`를 `@ManyToOne`으로 직접 참조하고 있습니다. 헥사고날 아키텍처 원칙에 따라 `Long departmentId`로 변경하고 ID 기반 통신으로 결합도를 낮추세요. -> *(완료: `SystemUser`의 `Department @ManyToOne` 제거, `departmentId` 값 참조 전환, 직접 참조 방지 회귀 테스트 추가)*

### [Expenditure-Resolution (지출 결의)]
- [x] **마스터 조회 실패 은닉(NPE 방어):** 전표 생성 시 부서/계정/거래처 미존재를 `orElse(null)`로 강제 허용하여 불완전한 전표가 저장될 위험이 있습니다. 마스터 조회 실패 시 명시적으로 예외 처리(Exception)하도록 변경하세요. -> *(완료: 전표 생성 경로가 `orElseThrow` 기반임을 재확인하고, 승인 전표 생성 시 부서/계정/거래처 누락이 전표 생성과 저장을 차단하는 회귀 테스트 추가)*

---

## 2. ⚠️ Medium (아키텍처 및 품질 개선 과제)

### [Common & Closing]
- [x] **하드코딩 더미 계정/금액 제거:** `closing` 모듈의 자동 분개 및 평가 배치에 남아있는 더미 계정(`999998`, `999999`)과 하드코딩된 금액들을 실제 룰 엔진이나 설정 정보 기반으로 동적 할당하도록 리팩토링하세요. -> *(완료: `ClosingAccountingProperties`를 추가해 평가/충당 유형별 차변 계정, 대변 계정, 금액을 `account.closing.accounting.*` 설정에서 읽도록 전환. 설정 누락/0 이하 금액은 자동 분개 생성 전 명시적으로 실패. `ClosingServiceTest`로 설정 룰 사용 및 누락 시 posting 미호출 검증. `:closing:core:test`, `:closing:api:compileJava`, `:closing:batch:compileJava` 성공)*.

### [Reporting (보고서)]
- [x] **목업(Mock) 연동 제거:** `LedgerClientAdapter`에 하드코딩된 원장 잔액(`101`, `102`)과 고정 보고서명(`ASSET_CASH`)을 걷어내고, 실제 `journal-ledger` 연동 데이터를 읽어 재무제표를 생성하도록 구현하세요. -> *(완료: `LedgerClientAdapter`가 `contracts`의 `LedgerQueryPort`로 기준일 GL 잔액 요약을 조회하고 계정별 잔액 맵으로 변환하도록 변경. 목업 과거 보고서(`PAST-001`/`ASSET_CASH`) 반환 제거. `reporting:core`의 `journal-ledger:core` 직접 의존성 제거 및 어댑터 회귀 테스트 추가)*.

### [Receivable & Governance (헥사고날 위반)]
- [x] **[Receivable] 모듈 간 객체 참조(FK) 위반 및 인코딩:** `Receivable` 엔티티가 `master-data`의 `BusinessPartner`를 `@ManyToOne`으로 직접 참조하여 Bounded Context 격리를 위반하고 있습니다. `String customerCode` (또는 ID)로 변경하세요. 파일 내 한글 깨짐(인코딩) 문제도 함께 해결하세요. -> *(확인 완료: 현재 `Receivable`, `SalesInvoice`, `Collection`은 고객 정보를 `customerCode` 값으로 저장하며, `BusinessPartner`는 `contracts.masterdata.BusinessPartnerRef` 조회 DTO로만 사용. 잔여 `@ManyToOne`은 같은 모듈 내부 `SalesInvoice`, `Collection` 관계만 해당)*.
- [x] **[Receivable] 웹 어댑터 도메인 노출:** `CollectionController` 및 `SalesController`에서 응답 시 도메인 엔티티를 직접 노출하지 말고 DTO로 전환하세요. -> *(완료: `SalesController`는 `SalesInvoiceRequest/Response`, `CollectionController`는 `CollectionRequest/Response`를 사용하도록 전환. 수동 매칭 요청도 `Map<String,Object>` 대신 `ManualMatchingRequest`로 변경하고, 기존 `amount` 입력명은 `@JsonAlias`로 호환. 컨트롤러 단위 테스트 추가 및 `:receivable:test` 성공)*.
- [x] **[Governance] 도메인 엔티티 및 Map 노출:** `AuditController`에서 도메인 엔티티 반환 및 `Map<String, String>` 입력 구조를 전용 DTO로 변경하세요. -> *(완료: 감사로그/역할/권한/마스터 승인 API 응답을 `*Response` DTO로 전환하고, 역할 생성/권한 부여/승인 결정 입력을 전용 request DTO로 교체. `:governance:test` 성공)*
- [x] **[Governance] TracingService 포트 우회:** `TracingService`가 `AuditLogRepository`에 직접 의존하여 계층을 무너뜨린 것을 Port를 통하도록 구조를 수정하세요. -> *(완료: `TracingService`가 `AuditLogPersistencePort`를 통해 감사로그를 조회하도록 전환하고 `TracingServiceTest`로 포트 호출 경계를 검증. `:governance:test` 성공)*

### [Reconciliation (인코딩 및 품질)]
- [x] **엔티티 파일 인코딩 복구:** `ReconciliationDifference.java` 등 일부 엔티티 파일의 한글 주석과 문자열이 심각하게 깨져 있습니다. UTF-8로 인코딩을 정리하고 가독성을 확보하세요. -> *(완료: `ReconciliationDifference`는 선행 handoff에서 정상화되어 있음을 확인했고, 잔여 mojibake 검색 결과 `ReconciliationVariance.java`의 깨진 getter/setter 주석 1건을 ASCII 설명으로 정리. BOM/제어문자/주요 mojibake 패턴 재검색 결과 없음. `.\gradlew :reconciliation:test --console=plain --max-workers=1` 성공)*.

### [Reconciliation (대사 심화)]
- [x] **복합 매칭 조건 확장:** `AutomatedMatchingEngine`의 매칭 로직에 금액/일자 외에도 설명문구 유사도, 전표번호, 계좌번호 등 복합 조건을 지원하도록 고도화하세요. -> *(완료: `JournalDetailSummary`에 전표번호/헤더·라인 적요/계좌번호 필드 추가, `AutomatedMatchingEngine`에 SlipNo/Description/AccountNo 매칭 옵션 및 로직 구현, 레거시 exact 매칭 사유 보존 테스트 포함 검증 완료)*
- [x] **계정 산정 정책 도메인화:** 업무별 차/대 계정 산정을 설정(`criteriaJson`)에만 의존하지 않고, 별도의 도메인 정책(Policy) 객체로 명확히 분리하세요. -> *(완료: `ReconciliationAdjustmentPolicy` 도메인 정책 객체 생성 및 계정 산정 검증 로직 이관, 숨은 기본 계정 fallback 제거, `ReconciliationService` 리팩토링 및 테스트 통과)*

### [Contracts & Journal-Ledger]
- [x] **[Contracts] LedgerQueryPort 조회 기능 확장:** 현재 GL(총계정원장) 잔액 조회만 지원하는 구조에 SL(보조원장)/거래처/부서 단위 조회 기능을 추가하세요. -> *(완료: `LedgerQueryPort.getSlBalanceSummaries` 추가, `LedgerBalanceSummary`에 거래처/부서 차원 필드 추가, `MonolithLedgerQueryAdapter`가 `LedgerService.getSlBalances`를 노출하도록 구현, 어댑터 테스트 추가 및 `:contracts:compileJava :journal-ledger:core:test` 성공)*
- [x] **[Journal-Ledger] 주석 정합성:** `JournalRuleEngine`의 주석("스켈레톤/빈 DRAFT 반환")이 실제 로직(평가 및 라인 생성)과 다르므로 정확히 갱신하세요. -> *(확인 완료: 현재 `JournalRuleEngine`에는 스켈레톤/빈 DRAFT 반환 주석이 남아 있지 않고, 룰 후보 조회/조건 평가/라인 생성 로직이 실제 구현되어 있음. `MonolithJournalPostingCommand`의 stale 엔티티 변환 주석은 ID 값 저장 설명으로 정리)*

### [Expenditure-Resolution]
- [x] **API 응답 DTO 필드 회귀:** `ExpenditureResolutionDto`에서 이름 필드(`departmentName` 등)가 항상 null로 반환되는 문제를 조회 어댑터 등을 활용하여 해결하세요. -> *(해결 완료: `ExpenditureResolutionDtoAssembler`에서 `MasterDataQueryPort`를 활용하여 동적으로 이름 필드를 채우는 로직 구현 확인)*

---

## 3. 🎨 프론트엔드 및 시스템 고도화 과제 (Next Steps)

- [ ] **[Frontend] 마스터 데이터 SCD2 타임라인 UI 구현:** `master-data`의 기준 정보 변경 이력을 한눈에 볼 수 있는 Audit Log 타임라인 화면을 프론트엔드에 구축하세요.
- [x] **[Backend] 전표 및 룰 엔진 검증 로직 강화:** 전표 생성 시 하드코딩된 차대일치, 계정유효성, 마감잠금 확인 로직을 독립적인 검증 필터/엔진으로 고도화하세요. -> *(완료: `JournalValidationFilter` 기반의 플러그인 아키텍처 도입. 차대일치(Balance), 마감잠금(ClosingLock), 계정유효성(AccountValidity) 필터 구현 및 `JournalValidationEngine`을 통한 통합 검증 적용 완료)*
