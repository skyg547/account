# Business Workflow - Accounting System

This document visualizes the core workflows using Mermaid diagrams.

## 1. Journal-to-Ledger Workflow (전표 처리 및 전기)

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

## 2. Master Data Approval Workflow (마스터 데이터 및 승인)

```mermaid
graph LR
    SUB["AccountSubject / BP"] --> REQ["Request Change / Creation"]
    REQ --> APR["Approval Workflow (MasterApproval)"]
    APR -->|Approved| COMMIT["Commit to Master Data (SCD2 Version Created)"]
    APR -->|Rejected| CANCEL["Request Cancelled"]
```

## 3. Multi-Currency Conversion Flow (다통화 처리)

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
