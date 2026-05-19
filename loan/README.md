# 🏦 Loan Service (대출 회계 관리)

`loan` 모듈은 은행/금융업의 핵심인 고객 대출의 실행, 이자 수익 계산, 상환 처리 등을 관리하는 대출 특화 서브레저입니다. 고도의 EIR(유효이자율) 계산 엔진과 SCD2 기반의 이력 추적 기능을 갖추고 있습니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

대출 업무는 '친구에게 돈을 빌려주고 매달 이자를 받는 것'과 비슷하지만, 회계적으로는 훨씬 정교합니다.
1. **대출 실행:** 고객에게 돈을 쏴주면 장부에는 "대출채권(받을 돈) / 현금" 이라고 기록합니다.
2. **이연 항목 (Deferred Item):** 대출 시 뗀 수수료는 오늘 한 번에 번 것으로 치지 않고, 대출 기간 내내 조금씩 나눠서 수익으로 잡습니다.
3. **유효이자율 (EIR):** 수수료와 원금 상환 등을 모두 고려한 '진짜 수익률'입니다. 이 이율에 따라 매일 조금씩 이자 수익을 인식합니다.
4. **SCD2 (이력 관리):** 금리가 바뀌거나 중도상환이 일어나면 과거 기록을 지우지 않고 "어제까지의 조건"과 "오늘부터의 새 조건"을 모두 남깁니다.

---

## 2. 🔄 프로세스 흐름 (Process Flow)

### 📌 대출 라이프사이클 및 수렴 흐름
대출 생성부터 원장 반영까지의 통합 흐름입니다. 모든 전표는 최종적으로 원장(`POSTED`)까지 수렴됩니다.

```mermaid
sequenceDiagram
    participant App as 외부 API/사용자
    participant Loan as LoanService
    participant EIR as EIR Calculator
    participant Journal as Journal-Ledger

    App->>Loan: 대출 실행 요청
    Loan->>EIR: 초기 EIR 및 상각 스케줄 생성
    EIR-->>Loan: 스케줄 결과 반환
    
    rect rgb(240, 240, 240)
        Note over Loan, Journal: 전표 수렴 프로세스
        Loan->>Journal: 대출 실행 전표 생성 (DRAFT)
        Loan->>Journal: 전표 승인 (APPROVED)
        Loan->>Journal: 전표 전기 (POSTED)
    end
    
    Loan->>Loan: 상태를 ACTIVE로 변경 및 전표번호 연결
    Loan-->>App: 실행 완료
```

### 📌 이벤트 발생 시 SCD2 처리 흐름
중도상환이나 금리 변경 시 기존 데이터를 마감하고 새 버전을 생성합니다.

```mermaid
flowchart TD
    A[중도상환/금리변경 이벤트] --> B{기존 데이터 존재?}
    B -- 예 --> C[is_current = false, valid_to = 현재]
    C --> D[새로운 조건으로 신규 버전 생성]
    D --> E[EIR 재계산 및 스케줄 재생성]
    E --> F[완료]
```

---

## 3. 📊 데이터 모델 (Schema)

`Loan` 단일 모델로 통합되어 있으며, 모든 주요 테이블은 SCD2 필드를 포함합니다.

```mermaid
erDiagram
    LOAN ||--o{ LOAN_AMORTIZATION_SCHEDULE_ENTRIES : "schedules"
    LOAN ||--o{ LOAN_ACCRUAL_LOG : "accruals"
    
    LOAN {
        Long id PK
        String loan_number "업무 식별자"
        String business_partner_code "ID Reference"
        BigDecimal principal_amount "원금"
        BigDecimal current_eir "유효이자율"
        Boolean is_current "SCD2"
        LocalDateTime valid_from
        LocalDateTime valid_to
    }
```

---

## 4. 🐳 실행 및 설정 방법

**필수 설정 (YAML):**
자동 전표 생성을 위해 다음 계정 코드 설정이 반드시 필요합니다.
```yaml
account.loan.accounting:
  cash-account-code: "101000"
  loan-receivable-account-code: "131000"
  accrued-interest-receivable-account-code: "11501"
```

**실행 방법:**
```bash
docker-compose up -d loan
```
