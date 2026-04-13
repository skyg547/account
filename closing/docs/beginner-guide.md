# closing beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`closing`은 월마감, 분기마감, 연마감 같은 기간 종료를 통제하는 운영 모듈이다.

## 2. 초보자가 먼저 이해해야 할 개념

### 2.1 FiscalPeriod

- 실제 회계기간 기준이다.
- 닫혔는지 열렸는지를 다른 모듈이 참조한다.

### 2.2 ClosingCalendar

- 해당 기간의 결산 진행판이다.
- "지금 이 기간 마감이 어디까지 왔는가?"를 보여준다.

### 2.3 ClosingTask

- 마감 전에 해야 하는 체크리스트다.
- 예: 대사 완료, 평가 실행, 보고서 점검

### 2.4 ClosingGate

- 단순 태스크보다 더 강한 통과 조건이다.
- 모든 필수 태스크가 끝나야 게이트를 넘는 식으로 쓸 수 있다.

### 2.5 PeriodLock

- 마감된 기간에 더 이상 거래를 넣지 못하게 막는 잠금이다.

### 2.6 ReopenApproval

- 닫힌 기간을 다시 열어야 할 때 남기는 승인 절차다.

## 3. 실제 운영 시나리오

### 3.1 월마감 시작

1. 해당 `FiscalPeriod`가 존재한다.
2. `ClosingCalendar`를 만든다.
3. 태스크와 게이트를 등록한다.
4. 캘린더 상태를 `IN_PROGRESS`로 바꾼다.

### 3.2 마감 작업 진행

1. 담당자가 태스크를 하나씩 완료한다.
2. 필요한 경우 평가 배치와 충당 배치를 실행한다.
3. 배치 결과로 결산용 전표가 만들어질 수 있다.
4. 필요한 수동 조정 전표를 `ClosingAdjustment`로 기록한다.

### 3.3 마감 완료 판정

1. `determineClosingStatus`를 호출한다.
2. 필수 태스크와 게이트를 검사한다.
3. 조건이 맞으면 `FiscalPeriod`와 `ClosingCalendar`가 닫힌다.

### 3.4 재오픈

1. 닫힌 기간에 수정 필요가 생긴다.
2. 재오픈 승인 요청을 만든다.
3. 승인되면 잠금이 해제되고 기간이 다시 열린다.

## 4. 주요 API

- `POST /api/closing/calendars`
- `GET /api/closing/calendars/{id}`
- `GET /api/closing/calendars/by-period`
- `PUT /api/closing/calendars/{id}/status`
- `POST /api/closing/tasks`
- `PUT /api/closing/tasks/{id}/status`
- `POST /api/closing/gates`
- `PUT /api/closing/gates/{id}/check`
- `POST /api/closing/period-locks`
- `DELETE /api/closing/period-locks/{fiscalPeriodId}`
- `POST /api/closing/reopen-approvals`
- `PUT /api/closing/reopen-approvals/{id}/status`
- `POST /api/closing/valuation-batches/run`
- `POST /api/closing/provision-batches/run`
- `POST /api/closing/adjustments`
- `POST /api/closing/calendars/determine-status`

## 5. 처음 읽는 코드 순서

1. `closing/src/main/java/com/ho/account/closing/service/ClosingService.java`
2. `closing/src/main/java/com/ho/account/closing/web/ClosingController.java`
3. `closing/src/main/java/com/ho/account/closing/domain/ClosingCalendar.java`
4. `closing/src/main/java/com/ho/account/closing/domain/ClosingTask.java`
5. `closing/src/main/java/com/ho/account/closing/domain/ClosingGate.java`
6. `closing/src/main/java/com/ho/account/closing/domain/PeriodLock.java`
7. `closing/src/main/java/com/ho/account/closing/domain/ReopenApproval.java`
8. `closing/src/main/java/com/ho/account/common/adapter/ClosingStatusAdapter.java`

## 6. 자주 헷갈리는 지점

### 6.1 마감 캘린더 상태와 실제 기간 상태는 다르다

- 운영 화면용 상태는 `ClosingCalendar`
- 다른 모듈이 참조하는 진짜 잠금 기준은 `FiscalPeriod.closingStatus`

### 6.2 게이트 조건은 아직 완전 자동이 아니다

- JSON 필드는 있지만 현재 서비스는 임시 통과 처리 성격이 있다.

### 6.3 배치 전표는 현재 임시 구현이다

- 더미 계정을 써서 `ADJUSTMENT` 전표를 만든다.
- 따라서 운영형 완성 로직으로 보기보다 스켈레톤으로 봐야 한다.

### 6.4 이 모듈은 다른 모듈과 강하게 연결된다

- `journal-ledger`는 이 모듈을 통해 마감 여부를 확인한다.
- 즉, 여기 상태가 잘못되면 전표 생성/수정/전기 통제도 틀어진다.

## 7. 체크리스트

- 해당 기간의 `FiscalPeriod`가 있는가
- 필수 태스크가 모두 완료됐는가
- 게이트가 모두 통과됐는가
- 잠금이 필요한가
- 재오픈 승인 이력이 남았는가
- 결산 조정 전표가 해당 기간과 타입 조건을 만족하는가
