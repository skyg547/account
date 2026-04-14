# Loan Process Flow

## 1. 전체 흐름

```mermaid
flowchart TD
    A[대출 계약 생성] --> B[대출 실행]
    B --> C[이연 항목 유형 생성]
    C --> D[이연 항목 등록]
    D --> E[EIR 상각 스케줄 생성]
    E --> F{중간 이벤트 발생?}
    F -- 아니오 --> G[일별 이자 발생 인식]
    F -- 예 --> H[중도상환/조건변경 이벤트 등록]
    H --> I[재계산 Run 생성]
    I --> J[기존 미래 스케줄 재계산 처리]
    J --> E
```

## 2. 대출 계약 생성

```mermaid
sequenceDiagram
    participant API as LoanController
    participant Service as LoanService
    participant BP as BusinessPartner
    participant Cur as Currency
    participant Repo as LoanRepository

    API->>Service: createLoan(request)
    Service->>BP: 거래처 조회
    Service->>Cur: 통화 조회
    Service->>Service: Loan 생성
    Note over Service: initialEIR = currentEIR\nstatus = ACTIVE
    Service->>Repo: 저장
```

설명:
- 대출번호, 거래처, 통화, 원금, 금리, 만기, 상환주기 등을 입력해 `Loan`을 생성합니다.
- 생성 시점에는 현재 EIR과 최초 EIR을 같은 값으로 시작합니다.

## 3. 대출 실행 분개

```mermaid
flowchart LR
    A[disburseLoan] --> B[LoanDisbursal 생성]
    B --> C[자동 분개 생성]
    C --> D[차변 131000 대출채권]
    C --> E[대변 101000 현금/예금]
    D --> F[LoanDisbursal에 분개 연결]
    E --> F
```

설명:
- 실행 시 대출금이 실제로 나간 것으로 보고 분개를 만듭니다.
- 현재 구현은 계정과목을 설정 테이블에서 찾지 않고 코드값으로 고정합니다.

## 4. 이연 항목과 EIR 상각

```mermaid
flowchart TD
    A[DeferredItemType 생성] --> B[DeferredItem 생성]
    B --> C[초기 이연 분개 생성]
    C --> D[EIR 스케줄 계산]
    D --> E[월별 상각 스케줄 저장]
    E --> F[상각 분개 생성]
    F --> G[잔여 이연금액 갱신]
```

설명:
- 대출 취급수수료 같은 금액은 즉시 비용/수익으로 처리하지 않고 이연합니다.
- 이후 매 기간 EIR 기준으로 조금씩 상각합니다.
- 현재 코드는 첫 번째 `DeferredItem` 하나를 중심으로 잔액을 갱신합니다.

## 5. 중도상환과 재계산

```mermaid
sequenceDiagram
    participant API as LoanController
    participant Service as LoanService
    participant Calc as EIRCalculator
    participant Run as RecalculationRunRepository
    participant Sch as EIRAmortizationScheduleRepository

    API->>Service: createLoanEvent(request)
    Service->>Run: RecalculationRun 저장
    Service->>Calc: 새 EIR 계산
    Service->>Sch: 재계산 기준일 이후 스케줄 표시
    Service->>Service: 새 스케줄 재생성
    Service->>Service: 필요 시 조정 분개 생성
```

설명:
- 중도상환이나 만기 변경이 생기면 기존 스케줄을 그대로 쓰지 않고 다시 계산합니다.
- 기존 미래 스케줄은 `isRecalculated = true`로 표시한 뒤 새 스케줄을 만듭니다.
- 조정 분개는 현재 `EARLY_REPAYMENT` 이벤트 중심으로 생성됩니다.

## 6. 일별 이자 발생 인식

```mermaid
flowchart TD
    A[배치 또는 서비스 호출] --> B[ACTIVE LoanContract 조회]
    B --> C[당일 로그 존재 여부 확인]
    C -->|있음| D[건너뜀]
    C -->|없음| E[해당 지급일 스케줄 조회]
    E --> F[차변 11501 미수이자]
    E --> G[대변 41101 이자수익]
    F --> H[로그 SUCCESS 저장]
    G --> H
```

설명:
- 이 흐름은 `LoanContract` 기반으로 동작합니다.
- 그래서 메인 대출 API가 쓰는 `Loan` 모델과 연결 규칙을 운영 관점에서 따로 관리해야 합니다.

## 7. 현재 구현상 주의점

- 분개 생성이 `JournalService`를 통일해서 사용하지 않습니다.
- 계정과목 선택이 하드코딩이라 환경별 계정체계 차이를 흡수하지 못합니다.
- `Loan`과 `LoanContract`가 병행되어 데이터 흐름이 한 번에 이어지지 않습니다.
- 재계산 시 첫 번째 이연 항목을 전제로 상각금액을 갱신하는 부분이 있습니다.
