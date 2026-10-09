# payable process flow

## API 입구

`payable:api`가 HTTP Controller, 요청 DTO, Bean Validation, 응답 DTO를 소유한다. `payable:core`는 HTTP를 모르고 `PurchaseInvoiceCommand`, `PaymentRunCommand` 같은 유즈케이스 입력값과 업무 서비스를 소유한다.

| 컨트롤러 | 메서드 | 경로 | 역할 |
| --- | --- | --- | --- |
| `PurchaseController` | `GET` | `/api/purchase/invoices`, `/api/payable/invoices` | 전체 또는 선택한 `status`의 매입 인보이스 목록을 조회한다. |
| `PurchaseController` | `GET` | `/api/purchase/invoices/{id}`, `/api/payable/invoices/{id}` | ID로 매입 인보이스 한 건을 조회한다. |
| `PurchaseController` | `POST` | `/api/purchase/invoices`, `/api/payable/invoices` | 매입 인보이스를 등록하고 채무 및 매입 인식 전표를 만든다. |
| `PurchaseController` | `POST` | `/api/purchase/payables/update-status/{asOfDate}`, `/api/payable/payables/update-status/{asOfDate}` | 기준일 이전 미지급 채무를 연체 상태로 갱신한다. |
| `PaymentController` | `POST` | `/api/payments/run` | 지급 기준일에 도래한 채무를 모아 지급 런과 지급 후보를 만든다. |
| `PaymentController` | `POST` | `/api/payments/execute` | 개별 지급을 실행하고 성공 시 채무 잔액을 차감한다. |
| `PaymentController` | `POST` | `/api/payments/advance` | 공급업체 선급금을 기록하고 전표 초안을 만든다. |
| `PaymentController` | `POST` | `/api/payments/offset-payable` | 선급금 잔액으로 채무를 상계한다. |

## 매입 인보이스 등록 흐름

```mermaid
flowchart TD
    A[POST /api/purchase/invoices] --> B[PurchaseController]
    B --> C[PurchaseInvoiceRequest Bean Validation]
    C --> D[PurchaseInvoiceCommand]
    D --> E[PurchaseUseCase]
    E --> F[PurchaseService]
    F --> G[MasterDataQueryPort로 공급업체 검증]
    F --> H[PurchaseInvoicePersistencePort로 중복 확인 및 저장]
    F --> I[Payable 생성]
    I --> J[PayablePersistencePort 저장]
    F --> K[PayableAccountMappingPort로 계정과목 결정]
    K --> L[JournalPostingPort로 매입 인식 전표 초안 생성]
    L --> M[PurchaseInvoiceResponse 반환]
```

핵심 정합성 포인트:

- API DTO는 HTTP 입력 모양과 Bean Validation을 담당한다.
- core command는 API와 Batch가 공통으로 쓰는 업무 입력값이며, 날짜/금액/actor 정합성을 한 번 더 확인한다.
- 같은 공급업체의 같은 인보이스 번호는 중복 등록하지 않는다.
- 공급업체 코드는 `master-data`에서 검증한다.
- 매입 인식 전표에 필요한 비용, 매입부가세, 매입채무 계정이 존재해야 한다.
- 채무는 인보이스 번호와 공급업체 코드로 원천 문서를 추적한다.

## 매입 인보이스 조회 흐름

`PurchaseController`는 기존 `/api/purchase`와 프런트엔드가 사용하는 `/api/payable`을 같은 유즈케이스에 연결합니다. Gateway도 두 prefix를 `payable-api`로 전달하며, 두 경로는 같은 `PurchaseInvoiceResponse`를 반환합니다.

```http
GET /api/payable/invoices
GET /api/payable/invoices?status=RECEIVED
GET /api/purchase/invoices/10
```

- `status`를 생략하거나 공백으로 보내면 전체 목록을 조회합니다.
- `status`는 대소문자를 구분하지 않고 `RECEIVED`, `APPROVED`, `PAID`, `PARTIAL_PAID`, `OVERDUE`, `CANCELLED` 중 하나를 사용합니다. 다른 값은 `400 INVALID_REQUEST`로 거부됩니다.
- 존재하지 않는 ID는 빈 응답 객체로 바꾸지 않고 `404 RESOURCE_NOT_FOUND`로 처리합니다.
- 조회는 읽기 전용이며 인보이스, 채무, 전표 상태를 변경하지 않습니다.

## 지급 런과 지급 실행 흐름

```mermaid
flowchart TD
    A[POST /api/payments/run] --> B[PaymentRunRequest]
    B --> C[PaymentRunCommand]
    C --> D[PaymentService.initiatePaymentRun]
    D --> E[기준일 도래 Payable 조회]
    E --> F{DB 조건부 claim 1행 성공?}
    F -->|0행, 다른 런이 선점| H
    F -->|1행| G0[채무 최신 잔액 다시 조회]
    G0 --> G1[Payment 생성]
    G1 --> G[Payment.payableId에 정확한 채무 ID 저장]
    G --> H[PaymentRun PROCESSING]
    H --> I[PaymentRunResponse 반환]

    J[POST /api/payments/execute] --> K[ExecutePaymentRequest]
    K --> L[ExecutePaymentCommand]
    L --> M[PaymentService.executePayment]
    M --> N{이미 COMPLETED인가?}
    N -->|예| O[기존 결과 반환]
    N -->|아니오| P[PAYMENT:{paymentId} 멱등 키 생성]
    P --> Q[PaymentExecutionPort 호출]
    Q -->|실패| R[Payment FAILED, 채무 미차감]
    Q -->|성공| S[Payable.applyPayment]
    S --> T[Payment COMPLETED]
    T --> U[JournalPostingPort로 지급 전표 초안 생성]
    U --> V[PaymentResponse 반환]
```

지급 실행은 `PAYMENT:{paymentId}` 멱등 키를 사용한다. 장애 후 같은 지급을 재시도해도 로컬 어댑터는 같은 참조번호를 반환한다. 운영에서는 이 포트 자리에 은행 API 어댑터를 연결하되, 같은 멱등 키 규칙을 유지해야 한다.

지급 후보를 만들기 전 `PayablePersistencePort.claimForPayment`가 채무 ID, 지급 가능 상태(`OPEN`, `APPROVED`, `UNPAID`, `PARTIAL_PAID`, `OVERDUE`), 양수 잔액을 한 DB UPDATE에서 검사하고 상태를 `IN_PAYMENT`로 바꾼다. 갱신 행 수가 1일 때만 최신 채무를 다시 읽어 그 잔액으로 `Payment`를 만든다. 0이면 다른 런이 먼저 선점했거나 상태가 바뀐 것이므로 후보를 만들지 않는다. claim과 후보 삽입은 같은 서비스 트랜잭션에서 커밋 또는 롤백된다. 따라서 실패한 트랜잭션은 claim을 남기지 않고 다시 시도할 수 있다. 초보자 관점에서는 같은 빚에 붙이는 '지급 중' 표식을 DB가 한 사람에게만 허용하는 셈이다.

## 선급금과 상계 흐름

```mermaid
flowchart TD
    A[POST /api/payments/advance] --> B[AdvancePaymentCommand]
    B --> C[AdvancePayment 저장]
    C --> D[선급금 전표 초안 생성]
    D --> E[AdvancePaymentResponse 반환]

    F[POST /api/payments/offset-payable] --> G[OffsetPayableCommand]
    G --> H[Payable 행 잠금 후 조회]
    H --> H1{IN_PAYMENT인가?}
    H1 -->|예| H2[상계 거부, 잔액 유지]
    H1 -->|아니오| I[AdvancePayment 조회]
    H --> J{두 vendorCode가 비어 있지 않고 같은가?}
    I --> J
    J -->|아니오| X[업무 예외: 변경 없이 거부]
    J -->|예| K[Payable.applyOffset]
    K --> L[AdvancePayment.applyOffset]
    L --> M[잔액과 상태 저장]
    M --> N[상계 전표 초안 생성]
    N --> O[PayableResponse 반환]
```

상계는 채무와 선급금 양쪽 잔액을 동시에 줄이는 업무다. 한쪽만 바뀌면 거래처 잔액이 틀어지므로 `PaymentService`가 두 도메인 객체를 함께 조회하고 같은 트랜잭션에서 처리한다. 채무 행 잠금은 지급 런의 조건부 claim과 상계를 직렬화한다. 이미 `IN_PAYMENT`인 채무의 상계는 잔액이나 전표를 변경하기 전에 거부한다. 상계가 먼저 완료되면 뒤따른 지급 런은 변경된 잔액으로 claim하고, 지급 런이 먼저 완료되면 상계가 거부된다.

또한 **두 객체의 `vendorCode`는 비어 있지 않고 같아야 한다.** 서비스는 잔액·상태를 처음 변경하기 전에 이 소유권 불변식을 확인한다. 코드가 다르거나 어느 한쪽의 코드가 `null` 또는 공백이면 `IllegalArgumentException`으로 거부하며 두 객체 저장 및 `JournalPostingPort` 호출을 하지 않는다. 상계 전표의 차변·대변 공급업체 코드는 검증된 채무 코드를 사용한다.

예를 들어 채무 ID 100(잔액 500.00)과 같은 공급업체 선급금 ID 200(잔액 200.00)이 있을 때 다음 본문으로 요청한다.

```json
{"payableId":100,"advancePaymentId":200,"offsetAmount":100.00}
```

채무 잔액은 400.00(`PARTIAL_PAID`), 선급금 잔액은 100.00(`ACTIVE`)이 되고 차변·대변에 각각 100.00인 전표 초안을 요청한다. 두 잔액이 모두 500.00일 때 500.00을 상계하면 채무는 `PAID`, 선급금은 `OFFSET`이 되며 양쪽 잔액은 0이다. 다른 공급업체 선급금 ID로 같은 요청을 보내면 두 잔액·상태가 그대로 남고 전표 초안도 생성하지 않는다. 요청 금액은 양쪽 잔액을 넘을 수 없다.

## 전표 유형과 차대변

| 업무 | 전표 유형 | 차변 | 대변 |
| --- | --- | --- | --- |
| 매입 인식 | `PURCHASE_RECOGNITION` | 비용, 매입부가세 | 매입채무 |
| 지급 실행 | `PAYMENT_EXECUTION` | 매입채무 | 현금/예금 |
| 선급금 지급 | `ADVANCE_PAYMENT` | 선급금 | 현금/예금 |
| 선급금 상계 | `AP_ADVANCE_OFFSET` | 매입채무 | 선급금 |

## 경계 정리 결과

- `payable:core`는 더 이상 HTTP Controller, 요청 DTO, Bean Validation, `spring-boot-starter-web`을 소유하지 않는다.
- `payable:api`가 요청 DTO와 응답 DTO를 소유하고, core 유즈케이스에는 command만 전달한다.
- `payable:batch`도 지급런 실행 시 `PaymentRunCommand`를 사용하므로 API와 Batch가 같은 core 업무 흐름을 공유한다.
- 도메인 객체는 응답 DTO로 변환되어 외부 JSON 계약에 직접 노출되지 않는다.
