# expenditure-resolution beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`expenditure-resolution`은 지출 요청을 결의서로 만들고, 승인 후 회계 전표와 지급 흐름으로 연결하는 모듈이다.

## 2. 초보자가 먼저 이해해야 할 개념

### 2.1 지출결의서

- 회사가 이 비용을 집행해도 되는지 판단하는 문서다.
- 회계 전표의 앞단 승인 문서라고 보면 된다.

### 2.2 상세 라인

- 어떤 계정과목으로 얼마를 지출하는지 적는다.
- 거래처도 라인별로 연결할 수 있다.

### 2.3 지급 계정

- 최종 대변으로 빠지는 계정이다.
- 예: 현금, 보통예금, 미지급금 성격 계정

### 2.4 예산 통제

- 승인 전에 "이 부서가 이 계정으로 쓸 수 있는 예산이 남았는가"를 확인한다.

### 2.5 세금계산서 연결

- 연결된 세금계산서는 `PURCHASE` 타입이어야 한다.

## 3. 실제 시나리오

### 3.1 일반 비용 집행

1. 사용자가 지출결의서를 작성한다.
2. 시스템이 부서, 계정, 거래처를 확인한다.
3. 예산을 차감한다.
4. 결의서를 `DRAFT`로 저장한다.
5. 승인 요청 후 승인되면 전표를 만든다.

### 3.2 고정자산 취득

1. 지출결의 상세 중 자산 계정이 있다.
2. 승인 시 회계 전표를 만든다.
3. 동시에 자산 등록 포트를 호출한다.

### 3.3 리스 연계 집행

1. 결의서가 리스 계약과 연결돼 있다.
2. 승인 시 리스 계약 활성화 포트를 호출한다.

### 3.4 AP 지급

1. 승인된 결의서를 대상으로 지급 이력을 만든다.
2. 상태는 처음 `PENDING`
3. 이후 지급 결과에 따라 상태를 갱신한다.

## 4. 주요 API

- `POST /api/expenditures`
- `PUT /api/expenditures/{id}`
- `POST /api/expenditures/{id}/request`
- `POST /api/expenditures/{id}/approve`
- `POST /api/expenditures/{id}/reject`
- `GET /api/expenditures`
- `GET /api/expenditures/{id}`
- `POST /api/ap/payments`
- `GET /api/ap/payments/{id}`
- `GET /api/ap/payments/by-expenditure/{expenditureResolutionId}`
- `PUT /api/ap/payments/{id}`
- `PATCH /api/ap/payments/{id}/status`
- `DELETE /api/ap/payments/{id}`

## 5. 처음 읽는 코드 순서

1. `expenditure-resolution/src/main/java/com/ho/account/expenditure/service/ExpenditureService.java`
2. `expenditure-resolution/src/main/java/com/ho/account/expenditure/web/ExpenditureController.java`
3. `expenditure-resolution/src/main/java/com/ho/account/expenditure/domain/ExpenditureResolution.java`
4. `expenditure-resolution/src/main/java/com/ho/account/expenditure/domain/ExpenditureDetail.java`
5. `expenditure-resolution/src/main/java/com/ho/account/expenditure/service/BudgetService.java`
6. `expenditure-resolution/src/main/java/com/ho/account/expenditure/service/APPaymentService.java`
7. `expenditure-resolution/src/main/java/com/ho/account/common/adapter/BudgetControlAdapter.java`

## 6. 자주 헷갈리는 지점

### 6.1 이 모듈이 직접 전기까지 끝내는 것은 아니다

- 전표를 만들기는 하지만,
- 실제 회계 규칙과 상태 관리는 `journal-ledger`에 의존한다.

### 6.2 승인 시 journal 상태는 `POSTED`까지 가지 않는다

- 현재 구현은 `create -> request -> approve`까지 호출한다.
- 즉, 전기까지 자동으로 가지는 않는다.

### 6.3 예산 차감 시점이 빠르다

- 현재는 결의 생성/수정 시점에 예산 사용 처리를 한다.
- 승인 시점 차감이 아니라는 점을 알아야 한다.

### 6.4 수정 로직은 예산 재정산이 정교하지 않다

- 기존 상세를 되돌린 뒤 다시 차감하는 구조가 아니라,
- 새 상세 기준으로 다시 사용 처리한다.
- 운영 로직을 강화하려면 이 부분 보완이 필요하다.

## 7. 체크리스트

- 부서 코드가 맞는가
- 지급 계정 코드가 맞는가
- 상세 계정과 거래처가 맞는가
- 세금계산서가 `PURCHASE` 타입인가
- 예산이 충분한가
- 자산/리스 연계 대상인가
- 승인 후 생성된 전표가 연결됐는가
