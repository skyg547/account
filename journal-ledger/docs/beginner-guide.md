# journal-ledger beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`journal-ledger`는 "거래를 회계 전표로 만들고, 승인하고, 원장에 반영하고, 나중에 다시 추적할 수 있게 남기는 모듈"이다.

## 2. 초보자가 먼저 이해해야 할 개념 5개

### 2.1 전표 (`JournalEntry`)

- 회계 처리 한 건의 헤더다.
- 예: "2026-04-13 대출 실행 전표"

### 2.2 전표 라인 (`JournalDetail`)

- 실제 차변/대변 줄이다.
- 하나의 전표에는 여러 라인이 들어간다.

### 2.3 상태 (`JournalEntryStatus`)

- `DRAFT`: 작성 중
- `REQUESTED`: 승인 요청됨
- `APPROVED`: 승인 완료
- `REJECTED`: 반려됨
- `POSTED`: 원장 반영 완료
- `REVERSED`: 역분개된 원본

### 2.4 GL / SL

- `GL`: 계정과목 중심 총계정원장
- `SL`: 거래처, 부서 등 보조 차원을 포함한 보조원장

### 2.5 lineage

- 원천 문서와 회계 결과를 연결하는 추적 키다.
- 값은 `lineageSourceType`, `lineageSourceId` 두 개다.
- 이 값이 있어야 "이 숫자가 어디서 왔는가?"를 끝까지 추적할 수 있다.

## 3. 가장 흔한 업무 시나리오

### 3.1 수기 전표

1. 사용자가 전표를 입력한다.
2. 시스템이 계정과목, 부서, 거래처, 차대 합계를 검증한다.
3. 전표가 `DRAFT`로 저장된다.
4. 승인 요청 후 `REQUESTED`가 된다.
5. 승인되면 전기 가능 상태가 된다.
6. 전기되면 GL/SL과 잔액에 반영된다.

### 3.2 자동 분개

1. 외부 도메인이 이벤트를 보낸다.
2. `JournalRule`이 맞는지 검사한다.
3. 규칙이 맞으면 전표를 생성한다.
4. 이후 흐름은 수기 전표와 동일하다.

### 3.3 미결 관리

1. 특정 계정과목이 미결 계정으로 설정돼 있다.
2. 전표 승인 시 `UnsettledItem`이 생성된다.
3. 나중에 정산 금액을 입력한다.
4. 남은 금액이 0이면 `CLEARED`가 된다.

## 4. 주요 API를 어떻게 보면 되는가

### 4.1 전표 API

- `POST /api/journals`
- `GET /api/journals?startDate=...&endDate=...`
- `GET /api/journals/{slipNo}`
- `PUT /api/journals/{id}`
- `DELETE /api/journals/{id}`
- `POST /api/journals/{id}/request`
- `POST /api/journals/{id}/approve`
- `POST /api/journals/{id}/reject`
- `POST /api/journals/{id}/post`
- `POST /api/journals/{id}/reverse`
- `POST /api/journals/from-event`

### 4.2 원장 API

- `POST /api/ledger/post/{journalEntryId}`
- `GET /api/ledger/gl-balances`
- `GET /api/ledger/sl-balances`
- `POST /api/ledger/reaggregate-balances`
- `GET /api/ledger/drill-down`

### 4.3 드릴다운 API

- `GET /api/drilldown/journal-entry/{journalEntryId}`
- `GET /api/drilldown/journal-entry/{journalEntryId}/source-document`

### 4.4 미결 API

- `GET /api/unsettled/businesspartner/{businessPartnerCode}`
- `POST /api/unsettled/{id}/settle`

## 5. 처음 분석할 때 추천하는 코드 순서

1. `journal-ledger/src/main/java/com/ho/account/journal/web/JournalController.java`
2. `journal-ledger/src/main/java/com/ho/account/journal/service/JournalService.java`
3. `journal-ledger/src/main/java/com/ho/account/journal/domain/JournalEntry.java`
4. `journal-ledger/src/main/java/com/ho/account/journal/domain/JournalDetail.java`
5. `journal-ledger/src/main/java/com/ho/account/ledger/service/PostingService.java`
6. `journal-ledger/src/main/java/com/ho/account/ledger/service/LedgerService.java`
7. `journal-ledger/src/main/java/com/ho/account/ledger/web/DrilldownController.java`
8. `journal-ledger/src/main/java/com/ho/account/unsettled/service/UnsettledService.java`

## 6. 실제로 자주 헷갈리는 지점

### 6.1 승인과 전기는 다르다

- 승인되었다고 원장 반영이 끝난 것이 아니다.
- `POSTED`가 되어야 원장 반영이 끝난다.

### 6.2 `amount`와 `baseAmount`는 다르다

- `amount`는 거래 통화 금액이다.
- `baseAmount`는 기준 통화 금액이다.
- 원장 잔액 집계는 현재 `baseAmount` 기준으로 누적된다.

### 6.3 드릴다운은 전표 번호만으로 끝나지 않는다

- 보고 숫자에서 원천 문서까지 가려면 lineage가 필요하다.
- 전표를 생성하는 외부 모듈이 이 값을 빠뜨리면 추적성이 떨어진다.

### 6.4 현재 전기 API는 두 개다

- `JournalService.postJournalEntry`
- `PostingService.postJournalEntry`

초보자 기준 정리:

- 잔액만 보면 `JournalService`도 이해 가능하다.
- GL/SL 상세와 드릴다운까지 보려면 `PostingService` 흐름까지 함께 봐야 한다.

## 7. 이 모듈을 볼 때 체크리스트

- 차변/대변 합계가 맞는가
- 마감 월 차단이 걸려 있는가
- 예산 통제가 필요한 라인인가
- 미결 계정인가
- lineage가 채워졌는가
- 전기 후 GL/SL 상세와 잔액이 모두 맞는가

## 8. 다음으로 같이 보면 좋은 모듈

- `master-data`: 계정과목, 부서, 거래처 참조
- `governance`: 감사 로깅, 통제
- `closing`: 마감 상태 조회 포트
- `reporting`: 드릴다운 소비 측
