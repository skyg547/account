# ⚖️ Reconciliation Service (대사 및 차이 관리)

`reconciliation` 모듈은 두 개 이상의 장부(예: 은행 내역 vs 회계 장부)를 비교하여 금액이 일치하는지 확인하고, 발생하는 차이를 추적하여 해소하는 프로세스를 담당합니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**대사(Reconciliation)는 '가계부와 내 통장 잔액이 맞는지 맞춰보는 일'입니다.**
1. **대사 단위:** "A은행 대사", "카드 매출 대사" 처럼 비교할 묶음을 말합니다.
2. **자동 매칭:** 사람이 일일이 대조하지 않고, 시스템이 금액과 날짜를 보고 "이건 이거네!" 하고 자동으로 짝을 맞추는 기능입니다.
3. **차이(Difference):** 대조해봤는데 어느 한쪽이 비거나 금액이 다른 경우입니다. (예: 수수료 500원 차이)
4. **조정 분개:** 차이가 났을 때 이를 장부에 반영해서 맞춰주기 위해 시스템이 자동으로 끊어주는 추가 전표입니다.

---

## 2. 🔄 프로세스 흐름 (Process Flow)

### 📌 대사 실행 및 차이 처리 라이프사이클
대사 실행부터 엔진의 매칭, 그리고 최종 조정 전표 생성까지의 흐름입니다.

```mermaid
flowchart TD
    A[대사 실행 요청] --> B[원천/대상 데이터 수집]
    B --> C{자동 매칭 엔진}
    C -->|금액/일자 일치| D[SUCCESS 상태 저장]
    C -->|차이 발생| E[Reconciliation Difference 생성]
    
    E --> F{조정 가능 여부 판단}
    F -->|자동 조정| G[조정 전표 생성 Port 호출]
    G --> H[전표 ID를 차이에 저장]
    F -->|수동 조치| I[담당자 할당 및 분석]
```

### 📌 자동 매칭 엔진 (Match Options)
단순 금액 외에도 전표번호, 적요 유사도 등을 조합하여 정교하게 매칭합니다.

```mermaid
flowchart LR
    A[Bank Statement] -- "Match Engine" --> B[Journal Details]
    subgraph Options
    C[금액 허용오차]
    D[일자 허용범위]
    E[전표번호 포함여부]
    F[적요 유사도]
    end
    C & D & E & F --> A
```

---

## 3. 📊 데이터 모델 (Schema)

대사 설정, 실행 이력, 그리고 발견된 차이를 체계적으로 관리합니다.

```mermaid
erDiagram
    RECONCILIATION_UNIT ||--o{ RECONCILIATION_RUN : "executes"
    RECONCILIATION_RUN ||--o{ RECONCILIATION_DIFFERENCE : "detects"
    
    RECONCILIATION_UNIT {
        Long id PK
        String name "대사 명칭"
        String criteria_json "비교 조건"
        Boolean is_current "SCD2"
    }
    
    RECONCILIATION_DIFFERENCE {
        Long id PK
        BigDecimal difference_amount "차이 금액"
        String status "OPEN, RESOLVED"
        Long adjustment_journal_entry_id "조정 전표 ID"
    }
```

---

## 4. 🐳 실행 및 연동 방법

**실행 명령:**
```bash
docker-compose up -d reconciliation
```

**연동 주의사항:**
- 대사 대상 데이터 조회 시 `JournalQueryPort`를 사용합니다.
- 조정 분개 생성 시 `ReconciliationAdjustmentPolicy`가 적용된 `JournalPostingPort`를 통해 처리됩니다.
