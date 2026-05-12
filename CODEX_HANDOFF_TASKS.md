# 📝 Codex 작업 지시서 (Handoff Tasks)

> **문서 목적:** 최근 진행된 리뷰 문서(`CLAUDE_WORKLOG.md`, `CODEX_WORKLOG.md`, `GEMINI_MODULE_REVIEW.md`, `MODULE_REVIEW_2026-05-06.md`, `WORKLOG.md`)를 전수 조사하여, 아직 해결되지 않은 아키텍처 위반, 버그, 하드코딩 이슈, 추가 고도화 과제를 하나도 빠짐없이 종합한 문서입니다. Codex는 본 리스트를 기준으로 작업을 진행해 주세요.

---

## 1. 🚨 Critical / High (우선 해결 과제)

### [Reconciliation (대사)]
- [x] **대량 데이터 성능 최적화:** `buildTargetSnapshot`에서 모든 전표를 루프 돌며 합산하는 방식은 대량 처리 시 성능 병목을 유발합니다. `JournalQueryPort`에 기간별 합계(Sum)를 DB 단에서 직접 집계해 반환하는 메서드를 추가하여 교체하세요. -> *(완료: `JournalDetailAggregateSummary`와 `JournalQueryPort.getJournalDetailAggregate` 추가, `journal-ledger` DB 집계 어댑터 구현, `ReconciliationService` 대상 집계 전환 및 테스트 보강)*
- [x] **외부 데이터 연동 미흡:** `ReconManagerService`의 `SOURCE`/`INTERFACE` 단계가 설정값(`matchingRulesJson`)에 의존하고 있습니다. 실제 외부 원천/인터페이스 시스템(Mock 어댑터 포함)을 통해 데이터를 가져와 집계하도록 전환하세요. -> *(완료: `ExternalReconSnapshotPort`/`ExternalReconSnapshotRequest` 계약 추가, `RECON_EXTERNAL_STAGE_RECORD` 스테이징 집계 어댑터 구현, `ReconManagerService` SOURCE/INTERFACE 포트 호출 전환 및 테스트 보강)*

### [Loan (대출)]
- [ ] **E2E 회귀 테스트 부재 및 전표 수렴 완결성:** 대출 전표가 원장(`POSTED`)까지 수렴하는 과정을 증명하는 E2E 검증 테스트가 부족합니다. 또한 `Loan`과 `LoanContract` 병행 모델을 통합하고, 코드 내 하드코딩된 계정코드를 제거하세요.
  - 진행: `LoanService`/`InterestAccrualService` 자동 전표가 생성 후 `approveJournalEntry`와 `postJournalEntry`까지 호출하도록 보강했고, 대출 회계 계정코드는 `LoanAccountingProperties` 설정으로 분리했습니다. `LoanServiceTest`, `InterestAccrualServiceTest`로 POSTED 수렴 호출과 설정 계정 사용을 검증했습니다.
  - 남음: `Loan`/`LoanContract` 병행 모델 통합과 실제 `journal-ledger` 모듈까지 포함한 통합 E2E 테스트는 아직 미완료입니다.

### [Master-Data (기준 정보)]
- [ ] **SCD2 완전 적용 미흡:** `Product` 및 `Department` 모듈에서 데이터 변경 시 기존 행을 직접 덮어쓰고 있어 SCD2(이력 관리) 정책을 위반하고 있습니다. 신규 버전을 생성하고 이전 버전의 `validTo`를 닫는 진정한 의미의 SCD2 로직으로 전면 수정하세요.

### [Governance (권한/감사)]
- [ ] **승인 연계 시 마스터 데이터 정보 유실:** `master-data` 변경요청에 대해 `governance` 모듈이 승인 시, 기존의 `effectiveDate`와 `requestedVersion`을 무시하고 `LocalDate.now()`와 고정 버전(1)으로 덮어써 이력이 훼손되는 문제를 수정하세요.

### [Expenditure-Resolution (지출 결의)]
- [ ] **마스터 조회 실패 은닉(NPE 방어):** 전표 생성 시 부서/계정/거래처 미존재를 `orElse(null)`로 강제 허용하여 불완전한 전표가 저장될 위험이 있습니다. 마스터 조회 실패 시 명시적으로 예외 처리(Exception)하도록 변경하세요.

---

## 2. ⚠️ Medium (아키텍처 및 품질 개선 과제)

### [Common & Closing]
- [ ] **하드코딩 더미 계정/금액 제거:** `closing` 모듈의 자동 분개 및 평가 배치에 남아있는 더미 계정(`999998`, `999999`)과 하드코딩된 금액들을 실제 룰 엔진이나 설정 정보 기반으로 동적 할당하도록 리팩토링하세요.

### [Reporting (보고서)]
- [ ] **목업(Mock) 연동 제거:** `LedgerClientAdapter`에 하드코딩된 원장 잔액(`101`, `102`)과 고정 보고서명(`ASSET_CASH`)을 걷어내고, 실제 `journal-ledger` 연동 데이터를 읽어 재무제표를 생성하도록 구현하세요.

### [Receivable & Governance (헥사고날 위반)]
- [ ] **[Receivable] 웹 어댑터 도메인 노출:** `CollectionController` 및 `SalesController`에서 응답 시 도메인 엔티티를 직접 노출하지 말고 DTO로 전환하세요.
- [ ] **[Governance] 도메인 엔티티 및 Map 노출:** `AuditController`에서 도메인 엔티티 반환 및 `Map<String, String>` 입력 구조를 전용 DTO로 변경하세요.
- [ ] **[Governance] TracingService 포트 우회:** `TracingService`가 `AuditLogRepository`에 직접 의존하여 계층을 무너뜨린 것을 Port를 통하도록 구조를 수정하세요.

### [Reconciliation (대사 심화)]
- [ ] **복합 매칭 조건 확장:** `AutomatedMatchingEngine`의 매칭 로직에 금액/일자 외에도 설명문구 유사도, 전표번호, 계좌번호 등 복합 조건을 지원하도록 고도화하세요.
- [ ] **계정 산정 정책 도메인화:** 업무별 차/대 계정 산정을 설정(`criteriaJson`)에만 의존하지 않고, 별도의 도메인 정책(Policy) 객체로 명확히 분리하세요.

### [Contracts & Journal-Ledger]
- [ ] **[Contracts] LedgerQueryPort 조회 기능 확장:** 현재 GL(총계정원장) 잔액 조회만 지원하는 구조에 SL(보조원장)/거래처/부서 단위 조회 기능을 추가하세요.
- [ ] **[Journal-Ledger] 주석 정합성:** `JournalRuleEngine`의 주석("스켈레톤/빈 DRAFT 반환")이 실제 로직(평가 및 라인 생성)과 다르므로 정확히 갱신하세요.

### [Expenditure-Resolution]
- [x] **API 응답 DTO 필드 회귀:** `ExpenditureResolutionDto`에서 이름 필드(`departmentName` 등)가 항상 null로 반환되는 문제를 조회 어댑터 등을 활용하여 해결하세요. -> *(해결 완료: `ExpenditureResolutionDtoAssembler`에서 `MasterDataQueryPort`를 활용하여 동적으로 이름 필드를 채우는 로직 구현 확인)*

---

## 3. 🎨 프론트엔드 및 시스템 고도화 과제 (Next Steps)

- [ ] **[Frontend] 마스터 데이터 SCD2 타임라인 UI 구현:** `master-data`의 기준 정보 변경 이력을 한눈에 볼 수 있는 Audit Log 타임라인 화면을 프론트엔드에 구축하세요.
- [ ] **[Backend] 전표 및 룰 엔진 검증 로직 강화:** 전표 생성 시 하드코딩된 차대일치, 계정유효성, 마감잠금 확인 로직을 독립적인 검증 필터/엔진으로 고도화하세요.
