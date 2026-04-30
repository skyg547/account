# tax beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`tax` 모듈은 매입 세금계산서(AP Invoice)를 안전하게 저장하고, 지출/지급 모듈이 증빙의 유효성을 검증할 수 있도록 기준 데이터를 제공하는 모듈입니다.

## 2. 초보자가 먼저 이해해야 할 핵심 개념

### 2.1 세금계산서 타입 (`type`)

- `SALES`: 매출 세금계산서
- `PURCHASE`: 매입 세금계산서

이 모듈의 AP API는 매입 업무 전용이므로 `PURCHASE`만 허용합니다.

### 2.2 금액 검증 규칙 (`BigDecimal`)

- `supplyAmount`(공급가액)
- `taxAmount`(세액)
- `totalAmount`(합계금액)

도메인 규칙:

- `supplyAmount + taxAmount == totalAmount`

즉, 숫자 한 자리라도 다르면 저장이 거부됩니다.

### 2.3 거래처 참조

세금계산서는 반드시 거래처(`BusinessPartner`)와 연결됩니다.
거래처 코드는 `master-data` 모듈에서 검증합니다.

## 3. 주요 API

- `POST /api/ap/invoices`
- `GET /api/ap/invoices/{id}`
- `GET /api/ap/invoices/issue-id/{issueId}`
- `GET /api/ap/invoices?startDate=...&endDate=...`
- `PUT /api/ap/invoices/{id}`
- `DELETE /api/ap/invoices/{id}`

## 4. 처음 읽는 코드 순서 (현재 경로 기준)

1. `tax/src/main/java/com/ho/account/tax/domain/TaxInvoice.java`
2. `tax/src/main/java/com/ho/account/tax/application/port/in/TaxInvoiceUseCase.java`
3. `tax/src/main/java/com/ho/account/tax/application/service/TaxInvoiceService.java`
4. `tax/src/main/java/com/ho/account/tax/adapter/in/web/APInvoiceController.java`
5. `tax/src/main/java/com/ho/account/tax/application/port/out/TaxInvoicePersistencePort.java`
6. `tax/src/main/java/com/ho/account/tax/repository/TaxInvoiceRepository.java`

## 5. 단계별 업무 흐름을 아주 쉽게 풀면

1. 사용자가 AP 세금계산서 생성 API를 호출합니다.
2. 서비스가 `type == PURCHASE`인지 먼저 확인합니다.
3. 거래처 코드가 실제 마스터 데이터에 존재하는지 확인합니다.
4. 도메인 객체에서 금액 합계 검증을 수행합니다.
5. 검증이 통과되면 저장하고, 조회/수정/삭제도 같은 규칙으로 보호합니다.

## 6. 자주 헷갈리는 지점

### 6.1 왜 조회에서도 `PURCHASE`를 다시 확인하나요?

서비스는 AP 전용 경계를 지키기 위해 조회 결과에서도 `isPurchaseType()` 필터를 적용합니다.
즉, 잘못된 타입 데이터가 섞여 있어도 AP API는 매입 데이터만 반환합니다.

### 6.2 왜 단순 CRUD처럼 보이는데 도메인 검증이 중요한가요?

세금계산서는 다른 회계 흐름(지출결의/AP 지급)의 기준 증빙입니다.
여기서 금액/타입 검증을 놓치면 후속 모듈에서 잘못된 전표나 지급이 발생할 수 있습니다.

## 7. 초보자 체크리스트

- 요청 `type`이 `PURCHASE`인가?
- 거래처 코드가 `master-data`에 실제 존재하는가?
- `supplyAmount + taxAmount == totalAmount`를 만족하는가?
- 조회/수정/삭제 대상도 `PURCHASE`인가?
- 오류 메시지가 업무 담당자가 이해할 수 있게 충분히 구체적인가?
