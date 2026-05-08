# receivable beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`receivable`은 매출채권을 만들고, 입금이 들어오면 그 돈을 어떤 채권에 붙일지 관리하는 모듈이다.

## 2. 초보자가 먼저 이해해야 할 개념

### 2.1 매출 인보이스

- 고객에게 청구한 문서다.

### 2.2 매출채권

- 아직 받지 못한 돈이다.

### 2.3 수납

- 실제로 회사 계좌에 들어온 돈이다.

### 2.4 매칭

- 들어온 돈을 특정 채권에 연결하는 과정이다.

### 2.5 미매칭 수납

- 돈은 들어왔지만 아직 어떤 채권인지 확정하지 못한 상태다.

## 3. 실제 시나리오

### 3.1 매출 인식

1. 매출 인보이스를 발행한다.
2. 같은 금액으로 매출채권을 만든다.
3. 매출 인식 전표를 만든다.

### 3.2 입금 수신

1. 고객 돈이 들어온다.
2. 수납 레코드를 만든다.
3. 현금 증가/AR Clearing 증가 전표를 만든다.
4. 자동 매칭을 시도한다.

### 3.3 자동 매칭 성공

1. 채권 잔액을 줄인다.
2. 인보이스 상태도 갱신한다.
3. AR Clearing을 없애고 매출채권을 줄이는 전표를 만든다.

### 3.4 자동 매칭 실패

1. 미매칭 수납 목록에 넣는다.
2. 운영자가 나중에 수동 매칭한다.

## 4. 주요 API

- `POST /api/sales/invoices`
- `POST /api/sales/receivables/update-status/{asOfDate}`
- `POST /api/collections`
- `POST /api/collections/{collectionId}/auto-match`
- `GET /api/collections/unmatched`
- `POST /api/collections/manual-match`
- `POST /api/collections/matching-rules`
- `GET /api/collections/matching-rules`

## 5. 처음 읽는 코드 순서

1. `receivable/src/main/java/com/ho/account/income/service/SalesService.java`
2. `receivable/src/main/java/com/ho/account/income/service/CollectionService.java`
3. `receivable/src/main/java/com/ho/account/income/domain/SalesInvoice.java`
4. `receivable/src/main/java/com/ho/account/income/domain/Receivable.java`
5. `receivable/src/main/java/com/ho/account/income/domain/Collection.java`
6. `receivable/src/main/java/com/ho/account/income/domain/MatchingRule.java`
7. `receivable/src/main/java/com/ho/account/income/domain/UnmatchedCollection.java`
8. `receivable/src/main/java/com/ho/account/receivable/adapter/out/source/ReceivableSourceDocumentProvider.java`

## 6. 자주 헷갈리는 지점

### 6.1 수납 인식과 채권 상계는 분리돼 있다

- 돈이 들어오면 먼저 수납 전표를 만든다.
- 나중에 채권과 매칭되면서 별도 전표가 또 생긴다.

### 6.2 자동 매칭은 완벽하지 않다

- 규칙 우선순위와 기준에 따라 결과가 달라진다.
- 실패하면 미매칭 큐로 보내는 것이 정상 동작이다.

### 6.3 부분 매칭이 가능하다

- 일부만 매칭되면 나머지 금액은 새 미매칭 수납으로 남긴다.

### 6.4 계정코드 하드코딩이 있다

- `11100`, `10100`, `21100`, `40100`, `22100` 같은 기본 계정 코드가 직접 들어 있다.

## 7. 체크리스트

- 고객 코드가 맞는가
- 수납 참조번호가 있는가
- 자동 매칭 규칙 우선순위가 적절한가
- 채권 잔액이 음수가 되지 않는가
- 부분 매칭 후 잔여 수납이 제대로 분리됐는가
- 생성된 전표와 원천 문서 연결이 일관적인가
