# reporting beginner guide

## 1. 이 모듈을 한 문장으로 설명하면

`reporting`은 전표를 사람이 읽는 보고 숫자로 바꾸는 모듈이다.

## 2. 초보자가 먼저 이해해야 할 개념

### 2.1 보고 라인

- 보고서의 한 줄이다.
- 예: 현금, 총자산, 매출, 영업비용

### 2.2 매핑

- 어떤 계정이 어떤 보고 라인으로 들어가는지 정의하는 규칙이다.

### 2.3 스냅샷

- 특정 기준일 시점의 보고 결과를 저장한 캡처본이다.
- 나중에 다시 같은 숫자를 재현하거나 제출 증적을 남길 때 쓴다.

### 2.4 드릴스루

- 보고 숫자에서 원천 전표까지 내려가는 기능이다.

## 3. 실제 시나리오

### 3.1 재무상태표 조회

1. 기준일자를 입력한다.
2. 해당 일자에 유효한 매핑을 읽는다.
3. `POSTED` 전표만 집계한다.
4. 라인별 금액을 계산한다.

### 3.2 스냅샷 생성

1. 보고유형과 기준일자를 정한다.
2. 현재 매핑 기준으로 라인 금액을 계산한다.
3. 헤더와 디테일을 저장한다.
4. 동일 기준일에 새로 만들면 버전이 올라간다.

### 3.3 숫자 검증

1. 스냅샷을 만든다.
2. `CrossCheckService`로 대차 검증을 한다.
3. 맞지 않으면 매핑 또는 전표를 다시 본다.

### 3.4 숫자 추적

1. 특정 라인코드를 고른다.
2. 라인에 연결된 계정을 찾는다.
3. 해당 계정의 전표 상세를 조회한다.
4. 필요하면 `journal-ledger` 드릴다운으로 더 내려간다.

## 4. 처음 읽는 코드 순서

1. `reporting/core/src/main/java/com/ho/account/reporting/application/service/ReportingService.java`
2. `reporting/core/src/main/java/com/ho/account/reporting/application/port/in/GenerateStatementUseCase.java`
3. `reporting/core/src/main/java/com/ho/account/reporting/application/port/out/LoadLedgerPort.java`
4. `reporting/core/src/main/java/com/ho/account/reporting/domain/model/FinancialStatement.java`
5. `reporting/core/src/main/java/com/ho/account/reporting/domain/model/ReportLine.java`
6. `reporting/core/src/main/java/com/ho/account/reporting/infrastructure/persistence/LedgerClientAdapter.java`
7. `reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java`
8. `reporting/batch/src/main/java/com/ho/account/reporting/adapter/in/batch/ReportingBatchAdapter.java`

## 5. 자주 헷갈리는 지점

### 5.1 실시간 조회와 스냅샷은 다르다

- 실시간 조회는 지금 전표 상태를 바로 계산한다.
- 스냅샷은 계산 결과를 저장한다.

### 5.2 모든 집계가 수식 기반은 아니다

- 현재는 `SUM` 중심이다.
- `FORMULA`는 아직 제한적이다.

### 5.3 POSTED 전표만 본다

- 승인만 된 전표는 보고 숫자에 포함되지 않는다.

### 5.4 드릴스루 품질은 앞단 품질에 달려 있다

- 매핑이 틀리면 숫자가 틀린다.
- 전표 라인과 계정이 틀리면 추적도 틀린다.

## 6. 체크리스트

- 라인 매핑이 맞는가
- 기준일에 유효한 매핑인가
- POSTED 전표만 집계됐는가
- 스냅샷 버전이 올바른가
- 대차 검증이 통과하는가
