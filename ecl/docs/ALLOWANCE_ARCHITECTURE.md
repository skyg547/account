# IFRS 9 대손충당금 아키텍처와 설계 흐름도

이 문서는 대손충당금 모듈만 먼저 서비스할 때의 구성과 내부 설계 흐름을 다이어그램으로 정리한다. 실행 절차는 [ALLOWANCE_SERVICE_RUNBOOK.md](ALLOWANCE_SERVICE_RUNBOOK.md)를 따른다.

## 단독 서비스 구성도

```mermaid
flowchart LR
    Operator[운영자 또는 스케줄러]
    ExternalEtl[외부 ETL 또는 임시 SQL]
    Mart[account-mart 선택<br/>snapshot 생성]

    subgraph AllowanceService[IFRS 9 대손충당금 서비스]
        Api[ecl-api<br/>상태 조회 / 수동 실행 / 조회 API]
        Batch[ecl-batch<br/>allowanceEclJob 실행]
        Core[ecl-core<br/>Stage / PD / EAD / LGD / ECL / Summary]
    end

    Db[(PostgreSQL<br/>ifrs9_allowance_db)]
    Closing[closing 선택<br/>대손충당금 전표]

    Operator --> Api
    Operator --> Batch
    ExternalEtl -->|allowance_exposure_snapshots 적재| Db
    Mart -->|allowance_exposure_snapshots 생성| Db
    Api -->|JobLauncher 또는 조회| Batch
    Batch --> Core
    Core -->|bulk read/write| Db
    Db -->|allowance_summary 조회| Closing
```

처음 서비스 검증에서는 `PostgreSQL`, `ecl-batch`, `ecl-api`, `allowance_exposure_snapshots` 공급만 있으면 된다. `closing`은 summary 검증 뒤 연결한다.

## 헥사고날 레이어 설계

```mermaid
flowchart TB
    subgraph Inbound[Inbound Adapter]
        Rest[Allowance REST Controller]
        Runner[JobRunner CLI]
        Event[CDM Event Consumer 선택]
    end

    subgraph BatchAdapter[Batch Adapter]
        JobConfig[AllowanceEclBatchConfig]
        Steps[Tasklet / Reader / Processor / Writer]
    end

    subgraph Application[Application Service]
        SyncSvc[AllowanceExposureSyncService]
        StageSvc[StagingService / PdCalculationService]
        EadSvc[EadBatchService / CcfCalculationService]
        LgdSvc[LgdCalculationService]
        EclSvc[미래전망 가중 ECL 서비스]
        SummarySvc[AllowanceSummaryService]
    end

    subgraph Domain[Domain Model]
        Account[CrAccount]
        Customer[CrCustomer]
        Result[AllowanceEclResult]
        Params[AllowanceModelParams]
        EadResult[EadCalculationResult]
    end

    subgraph Outbound[Outbound Port]
        SyncPort[AllowanceExposureSyncPort]
        ResultRepo[AllowanceEclResultRepository]
        SummaryPort[AllowanceSummaryBuildPort]
        MasterRepo[Model Master Repositories]
        ParamPort[AllowanceModelParameterRepository Port]
    end

    subgraph Infra[Infrastructure Adapter]
        SyncJdbc[JdbcAllowanceExposureSyncAdapter]
        ResultJpa[JpaAllowanceEclResultRepository]
        SummaryJdbc[JdbcAllowanceSummaryPersistenceAdapter]
        MasterJpa[JPA Master Repositories]
        ParamJpa[AllowanceModelParameterPersistenceAdapter]
    end

    Db[(PostgreSQL / H2)]

    Rest --> Application
    Runner --> JobConfig
    Event --> JobConfig
    JobConfig --> Steps
    Steps --> Application
    Application --> Domain
    Application --> Outbound
    Outbound --> Infra
    ParamPort --> ParamJpa
    Infra --> Db
```

설계 기준:

- `ecl-batch`는 Job/Step 흐름 제어만 담당한다.
- 산식, 상태 판단, summary 생성 검증은 `ecl-core`의 service/domain/adapter에 둔다.
- 대량 입력과 summary 생성은 JDBC bulk SQL로 처리한다.
- 금액과 비율 계산은 `BigDecimal` 중심으로 유지한다.

## 런타임 산출 흐름

```mermaid
flowchart LR
    Snapshot[allowance_exposure_snapshots<br/>기준일 입력 snapshot]
    Customer[cr_customers<br/>차주 입력]
    Account[cr_accounts<br/>계좌 입력]
    Dq[데이터 품질 검증]
    Stage[IFRS 9 Stage / PD]
    Ead[EAD 산출]
    Lgd[LGD 산출]
    Ecl[미래전망 가중 ECL]
    Complete[COMPLETED 확정]
    Result[allowance_ecl_results]
    Mapping[allowance_account_mappings]
    Summary[allowance_summary]
    Closing[closing 전표 생성]

    Snapshot -->|bulk upsert| Customer
    Snapshot -->|bulk upsert| Account
    Customer --> Dq
    Account --> Dq
    Dq --> Stage
    Stage --> Ead
    Ead --> Lgd
    Lgd --> Ecl
    Ecl --> Complete
    Complete --> Result
    Result --> Summary
    Mapping --> Summary
    Summary --> Closing
```

`allowance_summary`는 `allowance_account_mappings` 검증이 끝난 뒤 기준일 단위로 교체한다. 매핑이 없으면 기존 summary를 보존하고 실패한다.

## 실행 시퀀스

```mermaid
sequenceDiagram
    participant User as 운영자/스케줄러
    participant Runner as ecl-batch JobRunner
    participant Job as allowanceEclJob
    participant Core as ecl-core Services
    participant DB as ifrs9_allowance_db

    User->>Runner: job.name, baseDate, runId, modelVersion 전달
    Runner->>Job: JobParameters 생성 후 실행
    Job->>Core: allowanceExposureSyncStep
    Core->>DB: snapshot 조회 후 고객/계좌 upsert
    Job->>Core: dqStep, stagingManagerStep
    Core->>DB: 모델 마스터 조회 및 Stage/PD 결과 저장
    Job->>Core: eadCrmManagerStep, eclManagerStep
    Core->>DB: EAD/LGD/ECL 산출 결과 저장
    Job->>Core: allowanceEclCompletionStep
    Core->>DB: 완료 상태 확정
    Job->>Core: allowanceSummaryStep
    Core->>DB: 계정 매핑 검증 후 allowance_summary 교체
    DB-->>User: 검증 SQL 또는 API 조회
```

## 데이터 설계 흐름

```mermaid
erDiagram
    ALLOWANCE_EXPOSURE_SNAPSHOTS {
        date base_date
        string exposure_id
        string source_account_no
        string customer_code
        string product_code
        string currency_code
        decimal outstanding_amount
        decimal undrawn_amount
        string staging
    }

    CR_CUSTOMERS {
        bigint id
        string customer_code
        string customer_type
        string internal_rating
    }

    CR_ACCOUNTS {
        bigint id
        bigint customer_id
        string account_no
        string product_code
        decimal outstanding_amt
        decimal notional_amt
        string staging
    }

    ALLOWANCE_ECL_RESULTS {
        bigint id
        date base_date
        bigint account_id
        string staging
        decimal ead
        decimal pd
        decimal lgd
        decimal weighted_ecl
        string status
    }

    ALLOWANCE_ACCOUNT_MAPPINGS {
        bigint id
        string product_code
        string biz_unit_code
        string currency_code
        string allowance_account_code
    }

    ALLOWANCE_SUMMARY {
        bigint id
        date base_date
        string run_id
        string model_version
        string allowance_account_code
        decimal target_allowance_amount
    }

    ALLOWANCE_EXPOSURE_SNAPSHOTS ||--o{ CR_CUSTOMERS : "customer_code"
    CR_CUSTOMERS ||--o{ CR_ACCOUNTS : "customer_id"
    CR_ACCOUNTS ||--o{ ALLOWANCE_ECL_RESULTS : "account_id"
    ALLOWANCE_ECL_RESULTS }o--|| ALLOWANCE_ACCOUNT_MAPPINGS : "product/biz/currency"
    ALLOWANCE_ACCOUNT_MAPPINGS ||--o{ ALLOWANCE_SUMMARY : "account mapping"
```

## 배포 단계별 확대

```mermaid
flowchart TD
    P1[1단계<br/>로컬 단독 검증]
    P2[2단계<br/>DB 공유 서비스]
    P3[3단계<br/>결산 연동]
    P4[4단계<br/>운영 스케줄링]

    P1 --> P2 --> P3 --> P4

    P1 --> P1A[ecl-batch CLI + PostgreSQL<br/>최소 seed 검증]
    P2 --> P2A[ecl-api + ecl-batch<br/>같은 DB 사용]
    P3 --> P3A[allowance_summary를 closing이 조회]
    P4 --> P4A[스케줄러 또는 이벤트로 기준일 실행]
```

처음에는 1단계와 2단계를 완료한 뒤 `allowance_summary` 금액과 계정 매핑을 확인한다. 그 다음 closing 연결 여부를 결정한다.
