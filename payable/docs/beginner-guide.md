# payable beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`payable`은 "사야 할 것을 사고, 갚아야 할 돈을 채무로 잡고, 실제로 지급하는" 모듈이다.

## 2. 초보자가 먼저 이해해야 할 개념

### 2.1 매입 인보이스

- 공급업체가 보낸 청구서다.

### 2.2 매입채무

- 아직 지급하지 않은 금액이다.
- 인보이스를 등록하면 채무가 열린다.

### 2.3 지급 런

- 여러 건 지급을 한 번에 처리하기 위한 배치 묶음이다.

### 2.4 선급금

- 청구서보다 먼저 돈을 준 경우다.
- 나중에 채무와 상계할 수 있다.

## 3. 실제 시나리오

### 3.1 매입 인보이스 등록

1. 공급업체와 인보이스 번호를 입력한다.
2. 인보이스를 저장한다.
3. 동일 금액의 `Payable`을 생성한다.
4. 매입 인식 전표를 만든다.

### 3.2 지급 실행

1. 만기 채무를 대상으로 `PaymentRun`을 만든다.
2. 각 채무별 `Payment`가 만들어진다.
3. 실제 지급을 실행한다.
4. 채무 잔액이 줄고 지급 전표가 생긴다.

### 3.3 선급금 상계

1. 먼저 돈을 지급한 `AdvancePayment`가 있다.
2. 나중에 채무가 생긴다.
3. 둘을 상계한다.
4. 채무와 선급금 잔액이 동시에 줄어든다.

## 4. 주요 API

- `POST /api/purchase/invoices`
- `POST /api/purchase/payables/update-status/{asOfDate}`
- `POST /api/payments/run`
- `GET /api/payments/run/{paymentRunId}`
- `POST /api/payments/execute`
- `POST /api/payments/advance`
- `POST /api/payments/offset-payable`

## 5. 처음 읽는 코드 순서

1. `payable/src/main/java/com/ho/account/expenditure/service/PurchaseService.java`
2. `payable/src/main/java/com/ho/account/expenditure/service/PaymentService.java`
3. `payable/src/main/java/com/ho/account/expenditure/domain/PurchaseInvoice.java`
4. `payable/src/main/java/com/ho/account/expenditure/domain/Payable.java`
5. `payable/src/main/java/com/ho/account/expenditure/domain/Payment.java`
6. `payable/src/main/java/com/ho/account/expenditure/domain/AdvancePayment.java`
7. `payable/src/main/java/com/ho/account/expenditure/adapter/out/source/PayableSourceDocumentProvider.java`

## 6. 자주 헷갈리는 지점

### 6.1 인보이스와 채무는 다르다

- 인보이스는 증빙이다.
- 채무는 회계상 미지급 금액이다.

### 6.2 지급 실행 시 채무 선택 로직은 단순하다

- 현재는 공급업체 기준 미지급 채무 중 조건에 맞는 한 건을 찾는 단순 방식이다.
- 운영 고도화 시 매칭 규칙을 더 정교하게 봐야 한다.

### 6.3 전표가 생성돼도 POSTED까지는 가지 않는다

- `create -> request -> approve`까지만 호출한다.
- 전기까지 자동으로 마무리되지는 않는다.

### 6.4 드릴다운 source type 값은 호환된다

- 공급자는 전표 생성 쪽 `PURCHASE_INVOICE`와 기존 연계 타입 `P2P_AP`를 함께 지원한다.

## 7. 체크리스트

- 공급업체가 존재하는가
- 인보이스 번호가 중복되지 않는가
- 채무 잔액이 맞는가
- 지급 후 상태가 맞게 바뀌는가
- 선급금 잔액이 음수가 되지 않는가
- 생성된 전표와 원천 문서 연결이 일관적인가
