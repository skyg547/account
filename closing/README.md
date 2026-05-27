# 🔒 Closing Service (결산 마감 관리)

`closing` 모듈은 회계 기간의 종료를 통제하고, 장부를 봉인하여 과거 데이터의 임의 수정을 방지하는 시스템의 '안전 자물쇠' 역할을 합니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**결산 마감은 '가계부 한 달 치를 확정하고 테이프로 봉인하는 것'과 같습니다.**
1. **결산(Closing):** 한 달 동안의 모든 거래를 모아 "이달의 이익은 얼마!"라고 최종 확정 짓는 작업입니다.
2. **태스크(Task):** 마감 전 반드시 체크해야 할 리스트입니다. (예: "통장 잔액 확인", "법인카드 정산")
3. **잠금(Lock):** 마감이 끝난 달에 누군가 몰래 과거 날짜로 전표를 넣지 못하도록 해당 기간을 잠그는 것입니다.
4. **재오픈(Reopen):** 정말 중요한 실수로 인해 잠긴 기간을 수정해야 할 때, 높은 책임자의 승인을 받아 임시로 자물쇠를 푸는 절차입니다.

---

## 2. 🔄 프로세스 흐름 (Process Flow)

### 📌 결산 캘린더 및 마감 파이프라인
마감 시작부터 태스크 검증, 최종 기간 잠금까지의 헥사고날 아키텍처 흐름입니다.

```mermaid
sequenceDiagram
    participant User as 사용자/배치
    participant Service as ClosingService
    participant Task as ClosingTask
    participant Journal as Journal-Ledger
    participant DB as Persistence Adapter

    User->>Service: 마감 프로세스 시작
    Service->>Task: 필수 체크리스트 검증
    Task-->>Service: 검증 완료
    
    rect rgb(240, 240, 240)
        Note over Service, Journal: 기간 잠금 및 연동
        Service->>DB: Period Lock 생성 (SCD2 이력 저장)
        Service->>Journal: 해당 기간 전표 생성 차단 활성화
    end
    
    Service-->>User: 마감 확정 완료
```

### 📌 타 모듈과의 연동 (Status Port)
전표 모듈은 전표를 끊기 전, 이 포트를 통해 해당 날짜가 마감되었는지 확인합니다.

```mermaid
flowchart LR
    A[Journal Service] -- "Is date closed?" --> B{AccountingPeriodStatusPort}
    B -- "Status Check" --> C[Closing Service]
    C -- "OPEN / CLOSED" --> A
```

---

## 3. 📊 데이터 모델 (Schema)

회계 기간의 상태와 잠금 이력을 완벽히 추적합니다.

```mermaid
erDiagram
    FISCAL_PERIODS ||--o{ PERIOD_LOCKS : "locked_by"
    CLOSING_CALENDARS ||--o{ CLOSING_TASKS : "manages"

    FISCAL_PERIODS {
        uuid id PK
        String fiscal_year "연도"
        String period_month "월"
        String status "OPEN, CLOSED"
        Boolean is_current "SCD2"
    }
    
    PERIOD_LOCKS {
        uuid id PK
        uuid fiscal_period_id FK "ID 기반 참조"
        String lock_type "GENERAL, PARTIAL"
        LocalDateTime locked_at "잠금 일시"
    }
```

---

## 4. 🐳 실행 및 설정 방법

**실행 명령:**
```bash
docker-compose up -d closing
```

**설정 주의사항:**
- 자동 평가/충당 분개 계정은 `account.closing.accounting.*` 설정을 통해 동적으로 할당됩니다.
- ECL 충당 배치는 `allowance_summary`에 확정된 IFRS 9 산출 결과가 있을 때만 목표 충당금을 읽어 보충/환입 전표를 생성합니다. 더 이상 `대출채권 잔액 * 1%` 고정률로 목표 충당금을 계산하지 않습니다.
- FX/ECL 배치 전표번호는 기준일, 배치 ID, 계정 식별자를 조합해 20자 이내로 결정적으로 생성됩니다. 배치 ID 파라미터가 없으면 기준일(`yyyyMMdd`)을 기본값으로 사용해 같은 기준일 재실행이 중복 전표번호로 감지될 수 있게 합니다.
- 전표 모듈 연동을 위해 `AccountingPeriodStatusPort` 구현체가 정상적으로 노출되어야 합니다.
