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

## 4. 🧭 로컬 실행 및 연동 방법

현재 `reconciliation`은 `core/api/batch` 구조로 실행됩니다.
`core`는 command, 유즈케이스 서비스, 도메인 규칙, persistence/outbound port를 갖고, `api`는 Controller/DTO/Bean Validation을 담당합니다. `batch`는 Spring Batch Job/Step 실행 진입점이며 실제 대사 판단은 core command와 서비스를 호출합니다.

**PowerShell 검증 명령:**
```powershell
.\gradlew :reconciliation:core:test :reconciliation:api:compileJava :reconciliation:batch:compileJava --console=plain --max-workers=1
```

**H2 local 실행:**
```powershell
.\gradlew :reconciliation:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
.\gradlew :reconciliation:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

**실제 Spring Batch Job 실행:**

```powershell
.\gradlew :reconciliation:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=reconciliationDailyJob reconciliationDate=2026-06-19 runBy=LOCAL deepMode=false" --console=plain --max-workers=1
```

**IntelliJ 실행 순서:**
1. 루트 프로젝트를 Gradle 프로젝트로 연다.
2. Gradle JVM을 JDK 17로 맞춘다.
3. 테스트는 `reconciliation > core > Tasks > verification > test`를 실행한다.
4. API 서버는 Gradle task `:reconciliation:api:bootRun`, Batch 컨텍스트는 `:reconciliation:batch:bootRun`을 실행한다.
5. API 요청 DTO는 `api/dto`에서 Bean Validation 후 core command로 변환되고, core 서비스는 HTTP 타입을 직접 알지 않는다.

**연동 주의사항:**
- 대사 대상 데이터 조회 시 `JournalQueryPort`를 사용합니다.
- 조정 분개 생성 시 `ReconciliationAdjustmentPolicy`가 적용된 `JournalPostingPort`를 통해 처리됩니다.
- 표준 실행 생명주기는 `ReconciliationUnit -> ReconciliationRun -> ReconciliationDifference`입니다.
- 단계별 진단 결과는 `ReconciliationStageResult`로 표준 Run에 연결되며, 과거의 병렬 Aggregate 모델은 사용하지 않습니다.
- `reconciliationDailyJob`은 활성 대사 단위를 찾아 `RunReconciliationCommand`로 core 대사 실행 서비스에 위임하며, `deepMode=true`이면 Deep reconciliation 흐름을 사용합니다.
- `ReconciliationBatchJobRegistryConfiguration`은 Batch Job 등록 시점만 늦춰 로컬/H2 컨텍스트 경고를 줄이며 업무 로직은 포함하지 않습니다.
- 상세 문서는 [docs/README.md](./docs/README.md)에서 `beginner-guide`, `process-flow`, `schema`, `local-run` 순서로 확인합니다.
