# Reporting Process Flow

## 재무제표 생성

```mermaid
sequenceDiagram
    participant API as Reporting API
    participant Service as ReportingService
    participant Ledger as LoadLedgerPort
    participant Mapping as LoadReportLineMappingPort
    participant Snapshot as Snapshot JPA Adapter

    API->>Service: generate(type, baseDate)
    Service->>Ledger: getAccountBalances(baseDate)
    Ledger-->>Service: Map<accountCode, BigDecimal>
    Service->>Snapshot: findFinalizedStatement(type, baseDate - 1 year)
    Snapshot-->>Service: previous FINAL statement
    Service->>Mapping: loadMappings(type, baseDate)
    Mapping-->>Service: effective SCD2 mappings
    Service->>Service: line amount aggregation
    Service->>Snapshot: saveFinalized(statement)
    Service-->>API: FINAL statement
```

## 감독보고 제출본 등록

```mermaid
sequenceDiagram
    participant API as Reporting API
    participant Service as RegulatoryReportSubmissionService
    participant Snapshot as LoadReportHistoryPort
    participant Submission as StoreRegulatoryReportSubmissionPort

    API->>Service: submit(type, baseDate, requester, correctionReason)
    Service->>Snapshot: findFinalizedStatement(type, baseDate)
    Snapshot-->>Service: FINAL statement snapshot
    Service->>Submission: nextVersion(type, baseDate)
    Submission-->>Service: version
    Service->>Service: validate lines and correction policy
    Service->>Submission: save(READY submission)
    Service-->>API: submission id/version/status
```

## 주석 마트 생성

```mermaid
sequenceDiagram
    participant API as Reporting API
    participant Service as DisclosureNoteMartService
    participant Snapshot as LoadReportHistoryPort
    participant Mart as Disclosure Note Mart Adapter

    API->>Service: generate(type, baseDate, requester)
    Service->>Snapshot: findFinalizedStatement(type, baseDate)
    Snapshot-->>Service: FINAL statement snapshot
    Service->>Service: classify note lines by maturity/rate/currency/risk
    Service->>Mart: replace(mart)
    Service-->>API: mart entries
```

## 감독보고 매핑 및 제출

```mermaid
sequenceDiagram
    participant API as Reporting API
    participant Service as RegulatoryFilingService
    participant Submission as LoadRegulatoryReportSubmissionPort
    participant Mart as LoadDisclosureNoteMartPort
    participant Mapping as LoadRegulatoryReportMappingPort
    participant Gateway as SubmitRegulatoryFilingPort
    participant Store as StoreRegulatoryFilingPort

    API->>Service: submit(type, baseDate, targetAgency, requester)
    Service->>Submission: findLatestReady(type, baseDate)
    Submission-->>Service: READY submission version
    Service->>Mart: find(type, baseDate)
    Mart-->>Service: disclosure note mart
    Service->>Mapping: loadMappings(type, baseDate)
    Mapping-->>Service: effective SCD2 mappings
    Service->>Service: map mart entries to filing lines
    Service->>Gateway: submit(package)
    Gateway-->>Service: receipt id
    Service->>Store: save(filing lines + receipt)
    Service-->>API: filing result
```

## 계층 책임

- `application.service`: 유즈케이스 흐름, 트랜잭션, 포트 협업을 담당합니다.
- `application.port.out`: 원장 조회, 매핑 조회, 스냅샷 저장 계약만 정의합니다.
- `domain.model`: 보고서, 라인, 매핑 유효성 같은 비즈니스 규칙을 보유합니다.
- `infrastructure.persistence`: JPA/Flyway 기반 DB 접근과 인메모리 데모 어댑터를 구현합니다.
