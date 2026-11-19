# Loan 데이터 모델

## 관계

```mermaid
erDiagram
    LOANS ||--o| LOAN_DISBURSALS : disburses
    LOANS ||--o{ DEFERRED_ITEMS : owns
    DEFERRED_ITEM_TYPES ||--o{ DEFERRED_ITEMS : classifies
    LOANS ||--o{ EIR_AMORTIZATION_SCHEDULES : schedules
    LOANS ||--o{ RECALCULATION_RUNS : recalculates
    LOANS ||--o{ LOAN_EVENTS : records
    RECALCULATION_RUNS ||--o{ LOAN_EVENTS : explains
    LOANS ||--o{ LOAN_ACCRUAL_LOG : accrues
```

## 테이블 역할

| 테이블 | 역할 |
| --- | --- |
| `loans` | 약정 원금, 현재 잔액, 소수 단위 이율, 거래처 ID, 통화 코드, 상태, optimistic lock version |
| `loan_disbursals` | 1회 실행 금액과 전표 계보 |
| `deferred_item_types` | EIR 현금흐름 정책과 Loan 소유 계정 코드 참조 |
| `deferred_items` | 대출별 이연 금액·잔액·기간·초기 전표 |
| `eir_amortization_schedules` | runtime이 생성하고 Batch가 읽는 단일 월별 EIR 스케줄 |
| `recalculation_runs` | 변경 전후 EIR·만기, 영향 분석, 조정 전표 |
| `loan_events` | 이벤트 유형·설명과 선택적 재계산/전표 계보 |
| `loan_accrual_log` | `(loan_id, accrual_date)`별 성공·실패·전표 결과 |
| `loan_amortization_schedule_entries` | V30에 남은 레거시 중복 테이블. runtime 코드가 사용하지 않음 |

모든 테이블이 SCD2인 것은 아닙니다. 계약 조건 변경의 업무 이력은 `recalculation_runs`와 `loan_events`가 보존하고, 동시 수정은 `loans.lock_version`이 감지합니다.

## 외부 참조 경계

| 값 | Loan 저장 형태 | 유효성 제공자 |
| --- | --- | --- |
| 차주 | `loans.business_partner_id` | Master Data 거래처 |
| 통화 | `loans.currency_code` | Master Data 통화 |
| 계정 | 설정 및 `deferred_item_types.*_account_ref` | Master Data 계정과목 |
| 전표 | 각 업무 테이블의 ID/전표번호 | Journal Ledger |

Master Data/Journaling JPA 엔티티를 Loan 엔티티 관계로 매핑하지 않습니다.

## Flyway

| 파일 | 역할 |
| --- | --- |
| `V30__init_loan_schema.sql` | 초기 통합 스키마와 현재는 레거시인 중복 스케줄 테이블 |
| `V31__loan_owned_journal_references.sql` | 실행·이연·재계산 전표번호 값 컬럼 |
| `V32__loan_accrual_journal_reference.sql` | 발생 로그·이벤트·EIR 스케줄 전표 참조 |
| `V34__loan_source_operation_identity.sql` | 이연 항목/이벤트의 operation_key + source_request_fingerprint, 대출+작업 유일 제약 |
| `V33__loan_consistency_and_reference_boundaries.sql` | lock version, 계정 코드 참조, 실행/스케줄/발생 멱등 인덱스, 상태 조회 인덱스 |

V30~V32는 배포된 checksum을 보존하고 V33 forward migration으로 보강합니다. V33 적용 전에 다음 중복을 사전 조회해야 합니다.

```sql
SELECT loan_id, COUNT(*) FROM loan_disbursals GROUP BY loan_id HAVING COUNT(*) > 1;
SELECT loan_id, schedule_date, COUNT(*) FROM eir_amortization_schedules GROUP BY loan_id, schedule_date HAVING COUNT(*) > 1;
SELECT loan_id, accrual_date, COUNT(*) FROM loan_accrual_log GROUP BY loan_id, accrual_date HAVING COUNT(*) > 1;
```

레거시 `loan_amortization_schedule_entries` 제거 완료 조건은 운영 데이터와 EIR 스케줄 대조·이관 보고서, 소비처 0건 확인, 백업/복구 리허설, 별도 forward migration입니다.

## 운영 조회 예시

```sql
SELECT loan_number, status, principal_amount, current_principal_balance,
       current_eir, maturity_date, lock_version
FROM loans
ORDER BY id DESC;
```

```sql
SELECT loan_id, schedule_date, beginning_balance, interest_income,
       principal_repayment, ending_balance
FROM eir_amortization_schedules
WHERE schedule_date = DATE '2026-04-30';
```

```sql
SELECT loan_id, accrual_date, accrued_amount, journal_entry_id,
       journal_no, status, error_message
FROM loan_accrual_log
ORDER BY accrual_date DESC, loan_id;
```


V34는 `deferred_items`와 `loan_events`에 최대 100자 작업 키와 64자 요청 지문을 추가합니다.
기존 행은 두 값 모두 NULL로 유지하며 신규 재시도 요청에만 바인딩합니다. 두 값이 함께 있거나
함께 없는지 CHECK로 검증합니다. 새 코드 배포 전에 V34를 적용하고 원격 Journal V21 receipt
스키마를 준비합니다. 마이그레이션 롤백은 기존 V30~V33을 수정하는 방식으로 하지 않습니다.
