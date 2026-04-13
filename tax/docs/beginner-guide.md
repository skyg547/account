# tax beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`tax`는 세금계산서를 저장하고, 다른 모듈이 매입 증빙인지 검증할 수 있게 하는 모듈이다.

## 2. 초보자가 먼저 이해해야 할 개념

### 2.1 세금계산서 타입

- `SALES`: 매출 세금계산서
- `PURCHASE`: 매입 세금계산서

이 구분이 중요하다. 지출/AP 쪽은 `PURCHASE`만 써야 한다.

### 2.2 공급가액, 세액, 합계금액

- `supplyAmount`
- `taxAmount`
- `totalAmount`

기본 규칙:

- `supplyAmount + taxAmount = totalAmount`

### 2.3 거래처

- 세금계산서는 거래 상대방과 연결된다.
- 거래처 코드는 `master-data`의 `BusinessPartner`를 참조한다.

## 3. 주요 API

- `POST /api/ap/invoices`
- `GET /api/ap/invoices/{id}`
- `GET /api/ap/invoices/issue-id/{issueId}`
- `GET /api/ap/invoices?startDate=...&endDate=...`
- `PUT /api/ap/invoices/{id}`
- `DELETE /api/ap/invoices/{id}`

## 4. 처음 읽는 코드 순서

1. `tax/src/main/java/com/ho/account/tax/domain/TaxInvoice.java`
2. `tax/src/main/java/com/ho/account/tax/service/APInvoiceService.java`
3. `tax/src/main/java/com/ho/account/tax/web/APInvoiceController.java`
4. `tax/src/main/java/com/ho/account/tax/adapter/TaxInvoiceQueryAdapter.java`
5. `tax/src/main/java/com/ho/account/tax/service/TaxService.java`

## 5. 자주 헷갈리는 지점

### 5.1 `TaxService`와 `APInvoiceService`가 둘 다 있다

- `TaxService`는 일반 세금계산서 저장/기간조회에 가깝다.
- `APInvoiceService`는 매입 세금계산서 전용 흐름이다.

### 5.2 외부 모듈은 전체 엔티티를 다 보지 않는다

- 계약 포트인 `TaxInvoiceQueryPort`로 최소 정보만 본다.
- 현재는 `id`, `issueId`, `type` 정도만 전달한다.

### 5.3 이 모듈의 핵심 검증은 단순하지만 중요하다

- 금액 합계 검증이 틀리면 증빙 자체가 잘못된 것이다.
- 타입 검증이 틀리면 지출/AP 흐름이 잘못 연결된다.

## 6. 체크리스트

- 타입이 맞는가
- 거래처가 존재하는가
- 공급가액 + 세액 = 합계금액인가
- 매입 흐름에서 `PURCHASE`만 사용하고 있는가
