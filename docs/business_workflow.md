# 🏆 전사 통합 비즈니스 프로세스 및 워크플로우 (Business Workflow)

이 문서는 데이터 발생부터 최종 재무제표 산출, 그리고 대손충당금(IFRS9) 분석까지 이어지는 전사 통합 비즈니스 파이프라인과 주요 도메인별 상세 워크플로우를 정의합니다.

---

## 1. 전사 통합 업무 파이프라인 (E2E)

시스템 전체의 데이터 흐름을 한눈에 파악할 수 있는 파이프라인입니다.

```mermaid
flowchart TD
    subgraph "Phase 1: 기반 시스템 (Foundation)"
        MD[(Master Data)] -- "기준정보 제공 (SCD2 이력관리)" --> SL
        MD -- "ID 기반 검증" --> JL
    end

    subgraph "Phase 2: 업무 서브레저 (Subledgers)"
        SL[Subledger Modules]
        EX[지출 expenditure] --> SL
        LN[여신 loan] --> SL
        AR[매출채권 receivable] --> SL
        AP[매입채무 payable] --> SL
        AS[자산/리스 asset-lease] --> SL
    end

    subgraph "Phase 3: 회계 엔진 (Core Accounting)"
        SL -- "1. 전표 생성 요청" --> JL_USE[JournalUseCase]
        JL_USE --> RE[자동 분개 엔진 Rule Engine]
        RE --> DRAFT[DRAFT 전표]
        DRAFT --> APP[전표 승인 APPROVED]
        APP -- "2. 전기 실행" --> POST[PostingService]
        POST --> GL_SL[(GL / SL 원장)]
    end

    subgraph "Phase 4: 결산 및 대사 (Closing & Recon)"
        GL_SL -- "3. 잔액 대조" --> RC[대사 엔진 reconciliation]
        RC -- "차이 발견 시" --> ADJ[조정 전표 생성]
        ADJ --> JL_USE

        CL[결산 통제 closing] -- "4. 마감 잠금" --> JL_USE
    end

    subgraph "Phase 5: 대손충당금(IFRS9) 분석 및 마트 (Risk & Mart)"
        GL_SL -- "5. 원장 데이터 적재" --> MART[(Account Mart ODS / CDM)]
        LN -- "여신 기초 데이터" --> MART
        
        MART -- "6. 통합 데이터 제공" --> ECL_ENG[IFRS9 ECL 엔진]
        ECL_ENG -- "7. 대손충당금 산출" --> ECL_SUM[Allowance Summary]
        
        ECL_SUM -- "결산 자동 분개" --> CL
    end

    subgraph "Phase 6: 보고 및 역추적 (Reporting)"
        GL_SL -- "8. 최종 잔액 집계" --> RP[보고서 엔진 reporting]
        RP --> FS[재무제표 & 주석 마트]
        FS --> DT[드릴스루 Drill-through]
        DT -- "9. 전표 상세 역추적" --> GL_SL
    end

    style MD fill:#f9f,stroke:#333,stroke-width:2px
    style GL_SL fill:#bbf,stroke:#333,stroke-width:2px
    style MART fill:#ffb,stroke:#333,stroke-width:2px
    style FS fill:#dfd,stroke:#333,stroke-width:4px
```

---

## 2. 도메인별 핵심 워크플로우

### 2.1 전표 처리 및 전기 (Journal-to-Ledger)
모든 경제적 사건은 복식부기 원리에 따라 전표로 기록됩니다.

```mermaid
graph TD
    A[Economic Event / Transaction] --> B{Manual or Auto?}
    B -- Manual --> C[Create DRAFT Journal Entry]
    B -- Auto/Rule --> D[Auto-generate DRAFT Journal Entry via JournalRule]
    
    C --> E[Submit for Approval]
    D --> E
    
    E --> F{Approval Decision}
    F -- Approved --> G[Journal Entry: APPROVED]
    F -- Rejected --> H[Journal Entry: REJECTED]
    
    G --> I[Finalize / Posting Execution]
    I --> J[Journal Entry: POSTED]
    
    J --> K[Generate GlEntry per Line]
    J --> L[Update GlBalance per Account/Period]
    
    K --> M[General Ledger Updated]
    L --> M
```

### 2.2 마스터 데이터 승인 및 이력 관리
기준정보의 변경은 엄격한 승인 절차와 SCD2 이력 관리를 따릅니다.

```mermaid
graph LR
    SUB["AccountSubject / BP"] --> REQ["Request Change / Creation"]
    REQ --> APR["Approval Workflow (MasterApproval)"]
    APR -->|Approved| COMMIT["Commit to Master Data (SCD2 Version Created)"]
    APR -->|Rejected| CANCEL["Request Cancelled"]
```

### 2.3 다통화 변환 프로세스
외화 거래 발생 시 회계 일자의 환율을 적용하여 장부 통화 금액을 자동 산출합니다.

```mermaid
sequenceDiagram
    participant EV as Transaction Event
    participant JS as Journal Service
    participant ER as Exchange Rate Repo
    participant JD as Journal Detail
    
    EV->>JS: Transaction Amount (Foreign)
    JS->>ER: Fetch Rate for Accounting Date
    ER-->>JS: Exchange Rate (e.g. USD/KRW)
    JS->>JS: Calculate Base Amount (Functional Currency)
    JS->>JD: Store Amount (Foreign) & Base Amount (Functional)
```

---

## 3. 주요 비즈니스 시나리오 (BPMN)

### 3.1 P2P/AP (지출 및 지급)
- **과정**: 지출 결의 -> 인보이스 검증 -> 지급 승인 -> 지급 실행 -> 전표 생성
- **통제**: 예산 잔액 체크 및 이중 승인(Maker-Checker) 적용.

### 3.2 O2C/AR (매출 및 수금)
- **과정**: 매출 청구 -> 수금(가상계좌 등) -> 자동 매칭(Matching Engine) -> 반제 처리
- **통제**: 미매칭 건에 대한 별도 큐(Queue) 관리 및 수동 조정 프로세스.

### 3.3 대출 회계 (Loans & EIR)
- **과정**: 대출 실행 -> 부대비용 발생 -> EIR 기반 상각 스케줄 생성 -> 매월 이자 수익 및 비용 상각 배치
- **통제**: 중도 상환 시 상각 스케줄 재계산 및 멱등성 보장.

### 3.4 대손충당금(IFRS9) 산출 E2E 프로세스 (Frontend to DB)
재무 담당자가 프론트엔드 대시보드에서 대손충당금 산출을 실행하고, 최종 결과를 조회하기까지의 전체 아키텍처 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    actor User as 재무 담당자
    participant UI as Frontend (Next.js)
    participant GW as API Gateway
    participant ECL_API as ECL Service (API)
    participant BATCH as Spring Batch (allowanceEclJob)
    participant MART as Account Mart (ODS/CDM)
    participant DB as PostgreSQL DB
    
    rect rgb(240, 248, 255)
        Note right of User: 1. 산출 실행 요청 (Batch Trigger)
        User->>UI: 대손충당금 산출 버튼 클릭
        UI->>GW: POST /api/v1/ecl/run
        GW->>ECL_API: API 라우팅
        ECL_API->>BATCH: JobLauncher.run(allowanceEclJob)
    end
    
    rect rgb(255, 240, 245)
        Note right of BATCH: 2. 데이터 적재 및 정합성 검증 (Phase 1)
        BATCH->>MART: 여신/고객 기초 데이터 스냅샷 동기화 요청
        MART-->>BATCH: ODS/CDM Data 반환
        BATCH->>DB: allowance_exposure_snapshots 테이블 적재
        BATCH->>BATCH: Data Quality (DQ) 룰 검증
    end
    
    rect rgb(255, 250, 205)
        Note right of BATCH: 3. IFRS9 대손충당금 모델 산출 (Phase 2)
        BATCH->>BATCH: Stage 판정 (연체일수 기반 1,2,3 단계)
        BATCH->>BATCH: 부도율(PD), 부도시손실률(LGD), 부도시노출액(EAD) 계산
        BATCH->>BATCH: 거시경제 미래전망 가중평균 ECL 산출
        BATCH->>DB: cr_risk_results 테이블에 최종 산출 내역 저장
    end
    
    rect rgb(240, 255, 240)
        Note right of BATCH: 4. 회계 결산 집계 (Phase 3)
        BATCH->>DB: INSERT INTO allowance_summary SELECT ...
        Note right of DB: 산출 결과를 회계 계정과 매핑하여 요약본 생성
        BATCH-->>ECL_API: Batch Status (COMPLETED) 반환
        ECL_API-->>GW: 200 OK (JobId)
        GW-->>UI: 실행 성공 응답
        UI-->>User: 산출 완료 알림
    end
    
    rect rgb(245, 245, 255)
        Note right of User: 5. 대손충당금 대시보드 조회
        User->>UI: 결과 대시보드 페이지 진입
        UI->>GW: GET /api/v1/ecl/summary
        GW->>ECL_API: API 라우팅
        ECL_API->>DB: SELECT * FROM allowance_summary
        DB-->>ECL_API: 집계 데이터 반환
        ECL_API-->>GW: JSON Response
        GW-->>UI: JSON Response
        UI->>UI: Recharts 기반 시각화 렌더링
        UI-->>User: IFRS9 분석 대시보드 제공
    end
```

---

## 4. 운영 및 예외 정책

- **오류큐 (Error Queue)**: 자동 분개나 인터페이스 실패 건을 격리하여 운영자가 재처리할 수 있도록 지원합니다.
- **재처리 (Reprocessing)**: 원인 수정 후 동일 거래를 재수신하거나 수동 재실행할 때 중복 전표가 발생하지 않도록 멱등성을 보장합니다.
- **마감 잠금 (Closing Lock)**: 결산 완료 후 과거 일자로의 소급 기표를 원천적으로 차단합니다.

---
**최종 수정일**: 2026-05-29
