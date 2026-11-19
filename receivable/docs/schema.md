# receivable schema

## 핵심 ERD

```mermaid
erDiagram
    SALES_INVOICES ||--o| RECEIVABLES : creates
    COLLECTIONS ||--o{ COLLECTION_ALLOCATIONS : allocated_by
    RECEIVABLES ||--o{ COLLECTION_ALLOCATIONS : settled_by
    COLLECTIONS ||--o| UNMATCHED_COLLECTIONS : queues

    SALES_INVOICES {
        Long id PK
        String invoice_no
        String customer_code
        LocalDate issue_date
        LocalDate due_date
        BigDecimal net_amount
        BigDecimal tax_amount
        BigDecimal total_amount
        String status
        String created_by
    }

    RECEIVABLES {
        Long id PK
        Long sales_invoice_id
        String customer_code
        BigDecimal original_amount
        BigDecimal outstanding_amount
        LocalDate due_date
        String status
    }

    COLLECTIONS {
        Long id PK
        LocalDate collection_date
        String customer_code
        BigDecimal amount
        BigDecimal matched_amount
        String bank_account
        String virtual_account
        String reference_no
        String status
    }

    COLLECTION_ALLOCATIONS {
        Long id PK
        Long collection_id
        Long receivable_id
        BigDecimal matched_amount
        BigDecimal residual_collection_amount
        BigDecimal residual_receivable_amount
    }

    UNMATCHED_COLLECTIONS {
        Long id PK
        Long collection_id
        String reason
        String status
        String resolved_by
        LocalDateTime resolved_at
    }
```

## 상태 전이

### Receivable

| 상태 | 의미 | 주요 진입 조건 |
| --- | --- | --- |
| `OPEN` | 아직 수납되지 않은 채권 | 인보이스 등록 후 채권 생성 |
| `PARTIAL_PAID` | 일부 수납 후 잔액 존재 | `Receivable.applyCollection` |
| `PAID` | 잔액이 0인 채권 | 수납 후 잔액 0 |
| `OVERDUE` | 수금기일이 지난 미수 채권 | `updateReceivableStatus(asOfDate)` |

### Collection

| 상태 | 의미 | 주요 진입 조건 |
| --- | --- | --- |
| `RECEIVED` | 입금만 기록되고 아직 채권과 매칭되지 않음 | 수납 등록 |
| `PARTIAL_MATCHED` | 입금액 중 일부만 채권에 배분됨 | `Collection.applyAllocation` 후 미배분액 존재 |
| `MATCHED` | 입금액 전액이 채권에 배분됨 | 미배분액 0 |
| `UNMATCHED` | 자동 매칭 실패로 수동 확인 필요 | 후보 없음 또는 중복 후보 |
| `CANCELLED` | 취소 처리 확장 지점 | 운영 취소 프로세스 |

### SalesInvoice

| 상태 | 의미 |
| --- | --- |
| `ISSUED` | 고객에게 발행됨 |
| `PARTIAL_PAID` | 연결 채권이 일부 수납됨 |
| `PAID` | 연결 채권이 모두 수납됨 |
| `OVERDUE` | 수금기일 경과 |
| `CANCELLED` | 취소됨 |

## 계정 매핑 설정

`ConfiguredReceivableAccountMappingAdapter`는 아래 Spring 설정값을 사용한다. 설정이 없으면 기본값을 쓴다.

| 설정 키 | 기본값 | 사용 업무 |
| --- | --- | --- |
| `account.receivable.account-mapping.accounts-receivable-account-code` | `11100` | 매출 인식 매출채권 차변, 반제 대변 |
| `account.receivable.account-mapping.revenue-account-code` | `40100` | 매출 인식 매출 대변 |
| `account.receivable.account-mapping.output-vat-account-code` | `22100` | 매출부가세 대변 |
| `account.receivable.account-mapping.cash-account-code` | `10100` | 수납 인식 현금/예금 차변 |
| `account.receivable.account-mapping.ar-clearing-account-code` | `21100` | 수납 인식 대변, 반제 차변 |

각 계정 코드는 `MasterDataQueryPort.findAccountSubject`로 존재 여부를 확인한다. 테스트 더블이나 호스트 앱의 master-data 초기 데이터가 이 계정들을 제공해야 전표 생성이 성공한다.

## 정합성 체크 포인트

- 금액 필드는 `BigDecimal`이며 도메인 메서드에서 0 이하 금액과 잔액 초과 금액을 막는다.
- 자동 매칭은 단일 후보일 때만 수행한다.
- 부분 매칭은 `Collection.matchedAmount`와 `CollectionAllocation`에 모두 남긴다.
- `CollectionAllocation.operationId`는 수동/자동 매칭 사건을 식별한다. `(collection_id, operation_id)`가 유일하므로 동일 수납의 중복 작업은 두 배분으로 저장할 수 없다.
- 수납 인식과 채권 반제는 분리된 전표 유형으로 남아 현금 입금과 채권 감소를 추적할 수 있다.
- `customerCode`는 외부 customer 엔티티 직접 참조가 아니라 master-data 코드 참조다.

## PostgreSQL 기준선

전용 Flyway V1은 6개 소유 테이블을 생성한다. `numeric(19,2)` 금액, 인보이스 합계, 인보이스당 채권 1개, 고객 귀속 복합 FK, 상태-잔액/매칭 금액, 배분 라인리지 및 배치 조회 인덱스를 강제한다. V2는 기존 배분에 `LEGACY:<id>`를 채운 뒤 `operation_id` 필수값과 유일 제약을 추가한다. history 없는 비어 있지 않은 DB에는 자동 baseline하지 않는다.

## 원천 생성 receipt (V3)

`receivable_source_creation_locks`는 0~63의 미리 생성된 잠금 행을 가진다. `(source_type, business_key)`의 Java 문자열 hash로 stripe를 고르며 금액/digest는 잠금 키가 아니다. 첫 요청도 원격 응답과 로컬 commit까지 잠금을 유지하므로 같은 요청은 한 소스만 만든다. 서로 다른 키의 stripe 충돌은 처리량만 줄이며 identity를 합치지 않는다.

`receivable_source_creations`의 PK는 `(source_type VARCHAR(50), business_key VARCHAR(100))`이고 `request_fingerprint VARCHAR(64)`, `source_id BIGINT`가 완료된 요청을 연결한다. source_type은 `SALES_INVOICE_REF`/`COLLECTION_REF`; source_id는 서로 다른 테이블을 참조하므로 polymorphic FK 대신 유스케이스가 소스 존재와 같은 트랜잭션 커밋을 보장한다. 조회 누락은 실패한다. receipt는 runtime UPDATE/DELETE 대상이 아니다. 요청 수에 비례하여 증가하며 보존/archival은 별도 승인 정책이 필요하다.
