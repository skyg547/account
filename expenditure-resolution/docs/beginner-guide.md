# expenditure-resolution beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`expenditure-resolution`은 지출 요청을 결의서로 만들고, 승인 후 전표/지급/자산/리스 흐름으로 안전하게 연결하는 모듈입니다.

## 2. 초보자가 먼저 이해해야 할 개념

### 2.1 지출결의서 (`ExpenditureResolution`)

- "이 비용을 집행해도 되는가"를 기록하는 승인 문서입니다.
- 상태는 `DRAFT -> REQUESTED -> APPROVED` 또는 `REJECTED`로 이동합니다.

### 2.2 상세 라인 (`ExpenditureDetail`)

- 어떤 계정과목으로 얼마를 지출하는지 기록합니다.
- 승인 시 각 상세 라인이 차변 분개 라인의 입력 근거가 됩니다.

### 2.3 지급 계정

- 결의 총액의 대변 계정입니다.
- 예: 보통예금, 현금, 미지급금 계정

### 2.4 예산 통제 (`BudgetService`)

- 결의 생성/수정 시 월(`yyyyMM`) + 부서 + 계정 기준으로 사용 예산을 반영합니다.
- 예산 부족 시 결의 저장이 거부됩니다.

### 2.5 세금계산서 검증

- 결의/AP 지급에 세금계산서를 연결하면 반드시 `PURCHASE` 타입이어야 합니다.
- 검증은 `TaxInvoiceQueryPort` 조회 후 타입 체크로 수행합니다.

## 3. 주요 API

### 3.1 지출결의 API

- `POST /api/expenditures`
- `PUT /api/expenditures/{id}`
- `POST /api/expenditures/{id}/request`
- `POST /api/expenditures/{id}/approve`
- `POST /api/expenditures/{id}/reject`
- `GET /api/expenditures`
- `GET /api/expenditures/{id}`

응답은 엔티티 직접 노출이 아니라 `ExpenditureResolutionDto`를 반환합니다.
(순환 참조/과다 직렬화 방지)

### 3.2 AP 지급 API

- `POST /api/ap/payments`
- `GET /api/ap/payments/{id}`
- `GET /api/ap/payments/by-expenditure/{expenditureResolutionId}`
- `PUT /api/ap/payments/{id}`
- `PATCH /api/ap/payments/{id}/status`
- `DELETE /api/ap/payments/{id}`

## 4. 처음 읽는 코드 순서 (현재 경로)

1. `expenditure-resolution/src/main/java/com/ho/account/expenditure/domain/ExpenditureResolution.java`
2. `expenditure-resolution/src/main/java/com/ho/account/expenditure/domain/ExpenditureDetail.java`
3. `expenditure-resolution/src/main/java/com/ho/account/expenditure/application/port/in/ExpenditureResolutionUseCase.java`
4. `expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/ExpenditureResolutionService.java`
5. `expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/BudgetService.java`
6. `expenditure-resolution/src/main/java/com/ho/account/expenditure/application/service/APPaymentService.java`
7. `expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/web/ExpenditureController.java`
8. `expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/web/APPaymentController.java`
9. `expenditure-resolution/src/main/java/com/ho/account/expenditure/adapter/in/contract/BudgetControlAdapter.java`

## 5. 실무 시나리오를 짧게 보면

1. 결의 생성: 부서/계정/거래처/세금계산서 검증 + 예산 반영
2. 승인 요청: 상태를 `REQUESTED`로 변경
3. 승인: 전표 생성 및 승인 처리, 자산/리스 연계, 결의 `APPROVED`
4. 지급 생성: 결의와 세금계산서 검증 후 `APPayment(PENDING)` 생성

## 6. 자주 헷갈리는 지점

### 6.1 이 모듈이 회계 전표 엔진 자체인가요?

아닙니다. 전표 생성 트리거를 담당하고, 실제 회계 처리 생명주기는 `journal-ledger` 유즈케이스에 위임합니다.

### 6.2 승인 전에 예산을 보나요, 승인 후에 보나요?

현재 구현은 결의 생성/수정 시점에 예산 사용을 반영합니다.
운영 규칙이 승인 시점 차감을 요구한다면 추가 설계가 필요합니다.

### 6.3 수정 시 예산 재정산은 완전한가요?

현재 코드는 기존 상세를 "복원"한 뒤 재차감하는 구조가 아니라, 새 상세 기준으로 다시 사용 처리합니다.
회계 정책에 따라 보강 여부를 검토해야 합니다.

## 7. 초보자 체크리스트

- 결의 상태 전이가 규칙(`DRAFT/REQUESTED/APPROVED/REJECTED`)을 지키는가?
- 세금계산서 연결 시 `PURCHASE` 타입 검증이 빠지지 않았는가?
- 금액 계산이 `BigDecimal`로 유지되는가?
- 예산 반영 시 `yearMonth + dept + account` 기준을 지키는가?
- 승인 후 생성 전표와 결의 연결(`journalEntry`)이 누락되지 않았는가?
