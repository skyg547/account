# DB DDL Current State

이 문서는 `docs/db` 하위 DDL 파일을 현재 Java 도메인 엔티티 기준으로 분류한 결과다.
현재 파일은 `docs/db/current`, `docs/db/legacy`로 물리적으로 분리되어 있다.

## 분류 기준

- `현행`: 현재 엔티티와 테이블명/구조 방향이 대부분 일치하며 우선 참조 가능한 DDL
- `부분 일치`: 핵심 테이블은 맞지만 누락되거나 병렬 모델이 있어 그대로 기준으로 쓰기 어려운 DDL
- `레거시`: 과거 스키마이거나 현재 엔티티와 키 전략/테이블 구조가 달라 참고용으로만 봐야 하는 DDL

## 파일별 상태

| 파일 | 상태 | 기준 엔티티/비고 |
| --- | --- | --- |
| `current/fixed_asset_schema.sql` | 현행 | `asset.FixedAsset`와 일치 |
| `current/ifrs16_lease_schema.sql` | 현행 | `asset.LeaseContract`, `RightOfUseAsset`, `LeaseLiability`, `LeasePaymentSchedule`와 일치 |
| `current/closing_schema.sql` | 부분 일치 | `closing` 주요 엔티티와 대체로 맞지만 `DailyClosingStatus` 테이블이 없음 |
| `current/o2c_ar_schema.sql` | 부분 일치 | `income.SalesInvoice`, `Receivable`, `Collection`, `MatchingRule`, `UnmatchedCollection`와 맞음. 다만 `income.ArInvoice`, `ArPayment`는 반영 안 됨 |
| `current/p2p_ap_schema.sql` | 부분 일치 | `expenditure.PurchaseInvoice`, `Payable`, `Payment`, `PaymentRun`, `AdvancePayment`와 맞음. 다만 `ap_invoices`, `ap_payments`, `Budget`, `ExpenditureDetail`, `ExpenditureResolution`는 반영 안 됨 |
| `current/loan_accounting_schema.sql` | 부분 일치 | `loan.Loan`, `LoanDisbursal`, `DeferredItem`, `EIRAmortizationSchedule`, `LoanEvent`, `RecalculationRun`와 맞음. 하지만 `LoanContract` 계열과는 별도 모델 |
| `current/reconciliation_schema_part.sql` | 부분 일치 | `RECONCILIATION_RESULTS`, `RECONCILIATION_VARIANCES`, `BANK_STATEMENTS` 중심의 구형/병렬 모델 |
| `current/recon_fixed.sql` | 부분 일치 | `reconciliation_schema_part.sql`의 한글 주석 보정본. 현행 lower-case 대사 엔티티 전체를 반영하지는 않음 |
| `legacy/report_schema.sql` | 레거시 | 보고 엔티티와 일부 개념은 겹치지만 `RPT_SNAPSHOT_HEADER` 등 실제 컬럼 구조가 다름 |
| `legacy/audit_security_schema.sql` | 레거시 | `security`/`audit` 엔티티와 PK, 조인 테이블, 테이블명이 다름 |
| `legacy/schema.sql` | 레거시 | 초기 통합 스키마. 현재 JPA 엔티티와 네이밍/키 전략이 다름 |
| `legacy/schema_fixed.sql` | 레거시 | `schema.sql` 보정본이지만 현재 엔티티 기준은 아님 |
| `legacy/schema_new.sql` | 레거시 | 다른 세대의 통합 스키마. 현재 모듈형 엔티티 기준과 혼재 |
| `legacy/table_spec.md` | 레거시 | 상위 통합 스키마 문서. 현재 JPA 테이블 정의와 직접 일치하지 않음 |

## 현재 기준으로 먼저 볼 파일

모듈 작업 시 아래 순서로 참조하는 편이 안전하다.

1. Java 엔티티
2. 이 문서(`docs/db/README.md`)
3. 해당 모듈의 현행 또는 부분 일치 DDL
4. 레거시 DDL

## 모듈별 메모

### 1. 자산/리스

- `fixed_asset_schema.sql`, `ifrs16_lease_schema.sql`는 현재 엔티티 기준 현행으로 봐도 무방하다.

### 2. 결산

- `closing_schema.sql`는 `daily_closing_status`를 포함하며, 실행 migration은 `closing/core/src/main/resources/db/closing-migration/V50__daily_eod_lifecycle.sql`이다.
- 월·연 기간 상태는 `ClosingCalendar`와 Master Data `FiscalPeriod`를 사용하며 별도 `ClosingPeriod` 병렬 모델은 제거했다.

### 3. O2C / AR

- 현행 서비스 흐름에 필요한 주요 테이블은 들어 있다.
- 하지만 `ar_invoices`, `ar_payments`처럼 병렬/확장 모델이 코드에 있어 DDL이 엔티티 전체 집합을 대표하지는 않는다.

### 4. P2P / AP

- 핵심 AP 플로우 테이블은 들어 있다.
- 예산/지출결의/보조 AP 테이블은 별도 정리가 필요하다.

### 5. 대출회계

- 현재 저장소에는 `Loan` 중심 모델과 `LoanContract` 중심 모델이 동시에 있다.
- `loan_accounting_schema.sql`는 `Loan` 중심 모델 기준이다.
- `LoanContract` 계열을 유지할지 정리할지 결정하기 전까지는 부분 일치로 본다.

### 6. 대사

- 현재 엔티티는 `reconciliation_units`, `reconciliation_runs`, `reconciliation_rules`, `reconciliation_differences`, `difference_reason_codes`까지 확장되어 있다.
- 반면 `reconciliation_schema_part.sql`와 `recon_fixed.sql`는 `RECONCILIATION_RESULTS`, `RECONCILIATION_VARIANCES`, `BANK_STATEMENTS` 중심이다.
- 따라서 대사 DDL은 별도 현행본 재정리가 필요하다.

### 7. 보안/감사

- `audit_security_schema.sql`는 숫자 PK 중심 테이블과 매핑 테이블을 정의한다.
- 현재 엔티티는 `SYSTEM_USERS`, `SYSTEM_ROLES`, `SYSTEM_FUNCTIONS`, `ROLE_FUNC_PERMISSIONS`와 별도 `audit.SystemRole`을 함께 사용한다.
- 이 영역은 현행 DDL을 다시 설계해야 한다.

### 8. 재무보고

- 현재 보고 엔티티는 `BASE_DATE`, `VERSION` 중심 스냅샷 구조다.
- `report_schema.sql`는 `fiscal_year`, `fiscal_period`, `snapshot_date_time` 중심이라 그대로 맞지 않는다.

## 권장 원칙

- 신규 작업 시 `docs/db/*.sql` 전체를 현행으로 가정하지 않는다.
- 모듈별로 현행 DDL을 분리하려면 장기적으로 `docs/db/current`와 `docs/db/legacy` 구조로 나누는 것이 적절하다.
- 정합성 작업은 `security/audit`, `loan`, `reconciliation`, `report` 순서로 우선 정리하는 편이 효과적이다.

## 엔티티 대응표

### 1. `current/fixed_asset_schema.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `fixed_assets` | `asset.FixedAsset` | 일치 |

### 2. `current/ifrs16_lease_schema.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `lease_contracts` | `asset.LeaseContract` | 일치 |
| `right_of_use_assets` | `asset.RightOfUseAsset` | 일치 |
| `lease_liabilities` | `asset.LeaseLiability` | 일치 |
| `lease_payment_schedules` | `asset.LeasePaymentSchedule` | 일치 |

### 3. `current/closing_schema.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `closing_calendars` | `closing.ClosingCalendar` | 대체로 일치 |
| `closing_tasks` | `closing.ClosingTask` | 대체로 일치 |
| `closing_gates` | `closing.ClosingGate` | 대체로 일치 |
| `period_locks` | `closing.PeriodLock` | 대체로 일치 |
| `reopen_approvals` | `closing.ReopenApproval` | 대체로 일치 |
| `valuation_batches` | `closing.ValuationBatch` | 대체로 일치 |
| `provision_batches` | `closing.ProvisionBatch` | 대체로 일치 |
| `closing_adjustments` | `closing.ClosingAdjustment` | 대체로 일치 |
| `daily_closing_status` | `closing.DailyClosingStatus` | V50 migration과 일치 |

### 4. `current/o2c_ar_schema.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `sales_invoices` | `income.SalesInvoice` | 일치 |
| `receivables` | `income.Receivable` | 일치 |
| `collections` | `income.Collection` | 일치 |
| `matching_rules` | `income.MatchingRule` | 일치 |
| `unmatched_collections` | `income.UnmatchedCollection` | 일치 |
| 없음 | `income.ArInvoice` | 미반영 |
| 없음 | `income.ArPayment` | 미반영 |

### 5. `current/p2p_ap_schema.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `purchase_invoices` | `expenditure.PurchaseInvoice` | 일치 |
| `payables` | `expenditure.Payable` | 일치 |
| `payments` | `expenditure.Payment` | 일치 |
| `payment_runs` | `expenditure.PaymentRun` | 일치 |
| `advance_payments` | `expenditure.AdvancePayment` | 일치 |
| 없음 | `expenditure.Invoice` (`ap_invoices`) | 미반영 |
| 없음 | `expenditure.APPayment` (`ap_payments`) | 미반영 |
| 없음 | `expenditure.Budget` | 미반영 |
| 없음 | `expenditure.ExpenditureDetail` | 미반영 |
| 없음 | `expenditure.ExpenditureResolution` | 미반영 |

### 6. `current/loan_accounting_schema.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `loans` | `loan.Loan` | 일치 |
| `loan_disbursals` | `loan.LoanDisbursal` | 일치 |
| `deferred_item_types` | `loan.DeferredItemType` | 일치 |
| `deferred_items` | `loan.DeferredItem` | 일치 |
| `eir_amortization_schedules` | `loan.EIRAmortizationSchedule` | 일치 |
| `loan_events` | `loan.LoanEvent` | 일치 |
| `recalculation_runs` | `loan.RecalculationRun` | 일치 |
| 없음 | `loan.LoanContract` | 병렬 모델 |
| 없음 | `loan.LoanAmortizationScheduleEntry` | 병렬 모델 |
| 없음 | `loan.LoanAccrualLog` | 병렬 모델 |

### 7. `current/reconciliation_schema_part.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `RECONCILIATION_RESULTS` | `reconciliation.ReconciliationResult` | 부분 일치 |
| `RECONCILIATION_VARIANCES` | `reconciliation.ReconciliationVariance` | 부분 일치 |
| `BANK_STATEMENTS` | `reconciliation.BankStatement` | 부분 일치 |
| 없음 | `reconciliation.ReconciliationUnit` (`reconciliation_units`) | 미반영 |
| 없음 | `reconciliation.ReconciliationRun` (`reconciliation_runs`) | 미반영 |
| 없음 | `reconciliation.ReconciliationRule` (`reconciliation_rules`) | 미반영 |
| 없음 | `reconciliation.ReconciliationDifference` (`reconciliation_differences`) | 미반영 |
| 없음 | `reconciliation.DifferenceReasonCode` (`difference_reason_codes`) | 미반영 |
| 없음 | `reconciliation.ReconUnitDefinition` | 미반영 |
| 없음 | `reconciliation.ReconStageResult` | 미반영 |

### 8. `current/recon_fixed.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `RECONCILIATION_RESULTS` | `reconciliation.ReconciliationResult` | 부분 일치 |
| `RECONCILIATION_VARIANCES` | `reconciliation.ReconciliationVariance` | 부분 일치 |
| `BANK_STATEMENTS` | `reconciliation.BankStatement` | 부분 일치 |

### 9. `legacy/report_schema.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `RPT_SNAPSHOT_HEADER` | `report.ReportSnapshotHeader` | 컬럼 구조 불일치 |
| `RPT_SNAPSHOT_DETAIL` | `report.ReportSnapshotDetail` | 부분 개념 일치 |
| `RPT_LINE_MAPPING` | `report.ReportLineMapping` | 부분 개념 일치 |
| `REGULATORY_SUBMISSION` | `report.RegulatorySubmission` | 부분 개념 일치 |
| `DISCLOSURE_MART` | `report.DisclosureMart` | 부분 개념 일치 |

### 10. `legacy/audit_security_schema.sql`

| DDL 테이블 | 대응 엔티티 | 상태 |
| --- | --- | --- |
| `system_users` | `security.SystemUser` | PK/컬럼 구조 불일치 |
| `security_roles` | `security.SecurityRole` | PK/테이블명 불일치 |
| `system_functions` | `security.SystemFunction` | 부분 개념 일치 |
| `role_func_permissions` | `security.RoleFuncPermission` | 부분 개념 일치 |
| `user_role_mappings` | `security.SystemUser`-`SecurityRole` 매핑 | 조인 테이블명/키 불일치 |
| `audit_logs` | `audit.AuditLog` | 테이블명/컬럼 구조 불일치 |
| 없음 | `audit.SystemRole` (`SYSTEM_ROLE`) | 병렬 모델 |
| 없음 | `audit.MasterApproval` (`MASTER_APPROVAL`) | 미반영 |

### 11. `legacy/schema.sql`, `legacy/schema_fixed.sql`, `legacy/schema_new.sql`, `legacy/table_spec.md`

이 파일들은 공통적으로 아래 이유로 현재 엔티티 기준과 직접 매핑되지 않는다.

| 레거시 정의 | 현재 엔티티 기준 문제 |
| --- | --- |
| `ACCOUNT_SUBJECT`, `DEPARTMENT`, `BUSINESS_PARTNERS` 같은 대문자/구형 네이밍 | 현재는 `account_subjects`, `departments`, `business_partners` 중심 |
| 코드 PK 중심 모델 | 현재는 일부 도메인에서 identity PK + 비즈니스 코드 병행 |
| Oracle 스타일 통합 스키마 | 현재는 모듈별 JPA 엔티티와 부분 DDL 혼재 |
| 단일 전사 스키마 문서 | 현재는 도메인별 구현 속도가 달라 모듈별 현행성이 다름 |
