# 🏛️ 차세대 재무 시스템 통합 아키텍처 명세서 (Enterprise Architecture Guide)

본 문서는 `account` 프로젝트의 전체적인 시스템 토폴로지, 도메인 설계 원칙, 계층 구조 및 프론트엔드/백엔드 통합 아키텍처를 정의하는 전사 마스터 문서입니다. 시스템의 지속 가능한 확장성과 금융 데이터 정합성을 위한 핵심 가이드라인을 제공합니다.

---

## 1. 시스템 컨텍스트 및 토폴로지 (System Context & Topology)

본 시스템은 은행 및 엔터프라이즈 환경의 핵심 재무 엔진으로서, 다양한 원천 거래(여신, 수신, 지출, 자산 등)를 수집하여 **총계정원장(GL)** 및 **보조원장(SL)**을 생성하고, 최종적으로 **IFRS 9 기준 대손충당금** 및 **법정 재무제표**를 산출합니다.

### 1.1 시스템 전체 토폴로지 구성도 (System Architecture Diagram)

```mermaid
flowchart TD
    subgraph ClientLayer ["1. Client & Ingress Layer (단일 진입점)"]
        Browser["사용자 브라우저 / 외부 시스템"]
        Nginx["Nginx Reverse Proxy & SSL Termination<br/>(Port 80 / 443, Zero Direct Port Exposure)"]
        Browser -->|HTTPS 443| Nginx
    end

    subgraph FrontendLayer ["2. Frontend Layer (Next.js 15 App Router)"]
        NextWeb["Next.js 15 Frontend<br/>(포트 :13000 / :3000, Standalone)"]
        BFF["HttpOnly Cookie BFF Route Handlers<br/>(/api/auth/*, Same-Origin)"]
        NextWeb --- BFF
    end

    subgraph GatewayLayer ["3. API Gateway & Control Plane"]
        Gateway["Spring Cloud Gateway<br/>(포트 :18000 / :8000, JWT/RateLimit)"]
        Eureka[("Eureka Service Discovery<br/>:8761")]
        ConfigServer["Spring Cloud Config Server<br/>:8888"]
    end

    subgraph MSALayer ["4. Backend Microservices (Spring Boot 3.4 / Hexagonal)"]
        direction TB
        subgraph FoundationDomain ["기반 & 거버넌스 도메인"]
            Auth["Auth (인증/인가)"]
            MasterData["Master Data (기준정보/SCD2)"]
            InternalAudit["Internal Audit (감사로그/RCM)"]
            Governance["Governance (권한/통제)"]
        end

        subgraph SubledgerDomain ["업무 서브레저 도메인"]
            Loan["Loan (여신/EIR상각)"]
            Deposit["Deposit (수신/이자계산)"]
            Payable["Payable (매입채무/지급)"]
            Receivable["Receivable (매출채권/수납)"]
            AssetLease["Asset & Lease (고정자산/IFRS16)"]
            Expenditure["Expenditure (지출결의/품의)"]
            Budget["Budget (예산/통제)"]
            Tax["Tax (부가세/세무)"]
        end

        subgraph AccountingDomain ["회계 엔진 & 결산 도메인"]
            JournalLedger["Journal Ledger (분개엔진/GL/SL원장)"]
            Closing["Closing (결산마감/FX평가)"]
            Reconciliation["Reconciliation (내외부 원장대사)"]
        end

        subgraph MartReportingDomain ["분석 마트 & 보고 도메인"]
            AccountMart["Account Mart (ODS/CDM 데이터마트)"]
            ECL["IFRS 9 ECL (대손충당금 평가엔진)"]
            Reporting["Reporting (재무제표/드릴스루)"]
        end
    end

    subgraph StorageLayer ["5. Storage & Messaging Layer"]
        PG[("PostgreSQL 16 (17개 독립 DB Context)<br/>auth_db, journal_db, loan_db ...")]
        Redis[("Redis 7 (:6379)<br/>토큰블랙리스트/분산락/캐시")]
        Kafka[["Kafka Event Broker<br/>도메인 이벤트 / Outbox Relay"]]
    end

    subgraph ObservabilityLayer ["6. Observability & Monitoring"]
        Prometheus[("Prometheus TSDB (:19090)<br/>15초 주기 메트릭 스크랩")]
        Grafana["Grafana (:3001)<br/>대시보드 시각화"]
        ELK{{"ELK Stack (7.17)<br/>Elasticsearch/Logstash/Kibana"}}
        Zipkin["Zipkin (:9411)<br/>분산 트레이싱"]
    end

    %% 연결 관계
    Nginx -->|Web Route| NextWeb
    Nginx -->|API Route| Gateway
    BFF -->|Internal REST| Gateway

    Gateway -.-> Eureka
    Gateway --> Auth & MasterData & SubledgerDomain & AccountingDomain & MartReportingDomain

    MSALayer -.-> ConfigServer
    MSALayer -.-> Eureka
    MSALayer --> PG
    MSALayer --> Redis
    MSALayer --> Kafka

    ObservabilityLayer -.->|Metrics/Logs/Traces| MSALayer
    Prometheus --> Grafana
```

---

### 1.2 6단계 엔드투엔드 금융 비즈니스 파이프라인 (Financial Business Pipeline)

```mermaid
flowchart TD
    subgraph P1 ["Phase 1: 기반 체계 (Foundation)"]
        MD["Master Data (SCD2 이력관리)"]
        AUTH["Auth (사용자/조직/권한 검증)"]
    end

    subgraph P2 ["Phase 2: 업무 원천 거래 (Subledgers)"]
        SUB["여신(Loan) / 수신(Deposit) / 매출(AR) / 매입(AP) / 자산리스(Asset) / 지출결의(Expenditure)"]
    end

    subgraph P3 ["Phase 3: 회계 엔진 (Core Accounting)"]
        JL["JournalUseCase (전표 생성 요청)"]
        RULE["자동 분개 룰 엔진 (Rule Engine)"]
        DRAFT["DRAFT 전표 생성 및 유효성 검증"]
        APP["전표 승인 (APPROVED)"]
        POST["전기 서비스 (PostingService)"]
        GL_SL[("총계정원장(GL) & 보조원장(SL) 적재")]
    end

    subgraph P4 ["Phase 4: 결산 및 대사 (Closing & Recon)"]
        RECON["대사 엔진 (내부 원장 vs 은행/외부 거래 대조)"]
        ADJ["차이 발생 시 조정 전표 자동 발행"]
        CLOSING["결산 통제 (월말/연말 Closing Lock 잠금)"]
    end

    subgraph P5 ["Phase 5: IFRS 9 대손충당금 & 마트 (Finance Mart)"]
        MART[("Account Mart ODS/CDM ETL 적재")]
        ECL_CALC["IFRS 9 ECL 대손 엔진 (PD x LGD x EAD)"]
        ALLOWANCE["대손충당금 결산 전표 자동 분개"]
    end

    subgraph P6 ["Phase 6: 재무제표 보고 & 역추적 (Reporting)"]
        FS["재무제표 & 주석 생성 (BS, PL, Cashflow)"]
        DRILL["드릴스루 (보고서 수치 ➔ 전표 ➔ 원천 거래 역추적)"]
    end

    MD & AUTH --> SUB
    SUB -->|원천 거래 이벤트/요청| JL
    JL --> RULE --> DRAFT --> APP --> POST --> GL_SL
    GL_SL --> RECON
    RECON -->|불일치 시| ADJ --> JL
    CLOSING -->|마감 잠금| JL
    GL_SL & SUB --> MART --> ECL_CALC --> ALLOWANCE --> CLOSING
    GL_SL --> FS --> DRILL --> GL_SL
```

---

## 2. 백엔드 설계 원칙 및 헥사고날 아키텍처

### 2.1 헥사고날 아키텍처 (Ports & Adapters)
비즈니스 로직(Core)과 외부 기술 구현(Adapters)을 완전히 격리하여 프레임워크나 인프라 변경에도 핵심 금융 규칙을 안전하게 보존합니다.

```mermaid
flowchart LR
    subgraph InboundAdapters ["외부 인바운드 어댑터 (Driving)"]
        WEB["RestController (웹 API 요청)"]
        BATCH["BatchJobRunner (새벽 청크 배치)"]
        KAFKA_IN["KafkaConsumer (타 도메인 이벤트)"]
    end

    subgraph HexagonalCore ["도메인 코어 (Domain Core - Pure Java POJO)"]
        direction TB
        IN_PORT["Inbound Port<br/>(UseCase 인터페이스)"]

        subgraph CoreLogic ["순수 비즈니스 로직 (POJO)"]
            SVC["Application Service<br/>(유스케이스 오케스트레이션)"]
            DOMAIN_ENTITY["Domain Entity<br/>(불변 식별자/비즈니스 메서드)"]
            VO["Value Object & FP Calculator<br/>(BigDecimal Money 연산)"]
        end

        OUT_PORT["Outbound Port<br/>(SPI 인터페이스: Repository/Client)"]

        IN_PORT --> SVC
        SVC --> DOMAIN_ENTITY & VO
        SVC --> OUT_PORT
    end

    subgraph OutboundAdapters ["외부 아웃바운드 어댑터 (Driven)"]
        JPA["Spring Data JPA Adapter<br/>(JPA Entity / PostgreSQL)"]
        KAFKA_OUT["Kafka Producer Adapter<br/>(Outbox Relay 이벤트 발행)"]
        REST_OUT["QueryPort Remote Adapter<br/>(contracts 기반 모듈 간 연동)"]
    end

    WEB & BATCH & KAFKA_IN -->|Port 계약 호출| IN_PORT
    OUT_PORT -->|Port 인터페이스 구현| JPA & KAFKA_OUT & REST_OUT
```

- **`domain`**: 순수 자바 객체(POJO). JPA, Spring 프레임워크 어노테이션이 전혀 없는 핵심 금융 엔티티 및 비즈니스 규칙.
- **`application.port`**: 도메인과 외부를 연결하는 인터페이스 계약. Inbound(`UseCase`)와 Outbound(`Repository`, `EventPublisher`)로 구분.
- **`application.service`**: UseCase 인터페이스 구현체. 여러 도메인 모델과 Outbound 포트를 오케스트레이션.
- **`adapter`**: 포트의 실제 기술 구현체. Web MVC Controller, Spring Data JPA Repository Adapter, Kafka Producer/Consumer 등.

### 2.2 모듈 간 분리 및 계약(Contract) 기반 연동
- **엔티티 직접 참조 금지**: 마이크로서비스 간, 그리고 동위 도메인 모듈 간 JPA Entity 직접 참조는 컴파일 타임에 엄격히 차단됩니다.
- **`contracts` 모듈 활용**: 타 모듈의 조회가 필요할 경우 `:contracts`에 정의된 `QueryPort` 및 `Ref DTO`를 통해서만 통신합니다.
- **Transactional Outbox 패턴**: 비즈니스 DB 변경과 이벤트 발행의 원자성(Atomicity)을 보장하기 위해 로컬 Outbox 테이블에 적재 후 Relay 스케줄러를 통해 Kafka로 안전하게 발행합니다.

---

## 3. 프론트엔드 아키텍처 (Next.js 15 & BFF Pattern)

프론트엔드는 현대적인 사용자 경험, 강력한 타입 안정성, 그리고 보안 강화(HttpOnly Cookie BFF)를 결합하여 설계되었습니다.

### 3.1 기술 스택
- **Framework**: Next.js 15 (App Router, Standalone Output)
- **Language**: TypeScript
- **State Management**: Zustand (Global UI 상태), TanStack Query (Server State 캐싱)
- **Styling**: Tailwind CSS & CSS Modules (KBank 스타일 Dual-Theme 지원)
- **Security**: Next.js API Route 기반 HttpOnly Session BFF

### 3.2 컴포넌트 및 BFF 라우팅 구조

```mermaid
flowchart TD
    subgraph "Next.js App Router"
        LAYOUT["app/layout.tsx (Global Shell & Dual Theme)"]
        PAGE["app/**/page.tsx (122개 경로 세그먼트)"]
        LAYOUT --> PAGE
    end

    subgraph "Components Layer"
        COMMON["components/common (UI Primitives)"]
        DOMAIN["components/domain (Business Widgets)"]
        PAGE --> COMMON
        PAGE --> DOMAIN
        DOMAIN --> COMMON
    end

    subgraph "Logic & State"
        STORE[("store/ Zustand (UI State)")]
        HOOKS["hooks/ TanStack Query"]
        PAGE --> STORE
        PAGE --> HOOKS
        DOMAIN --> HOOKS
    end

    subgraph "HttpOnly BFF Layer"
        BFF_ROUTES["app/api/auth/* (Route Handlers)"]
        PROXY["app/api/[...path] (BFF Proxy)"]
        HOOKS --> BFF_ROUTES
        PAGE --> PROXY
    end

    PROXY -.->|REST API (Internal Network)| Gateway["Spring Cloud Gateway: 18000"]
```

---

## 4. 핵심 데이터 및 금융 무결성 원칙

### 4.1 정밀도 및 통화 정책 (Financial Precision)
- **`BigDecimal` 필수 사용**: 금융 금액 및 이자율, 상각률 연산에 부동소수점(`double`, `float`) 사용은 전사적으로 엄격히 금지됩니다.
- **다통화(Multi-Currency) 처리**: 발생 통화(Transaction Currency)와 장부 통화(Book Currency: KRW)를 동시 보존하며 환율 이력을 추적합니다.

### 4.2 이력 관리 및 감사 추적 (SCD2 & Lineage)
- **SCD2 (Slowly Changing Dimension Type 2)**: 기준정보(Master Data)는 `valid_from`, `valid_to`를 통해 과거 특정 시점의 상태를 완벽히 재현할 수 있습니다.
- **데이터 라인리지(Lineage) 및 드릴스루(Drill-through)**: 최종 재무제표의 금액에서 전표(Journal), 그리고 원천 거래 전표(대출, 지출결의서 등)까지 양방향 추적이 가능합니다.

### 4.3 회계적 불변성 및 역분개 (Immutability & Reversal)
- **원장 물리 삭제 금지**: 전기(Post) 완료된 전표 및 원장 레코드는 물리적 삭제(`DELETE`)가 불가능합니다.
- **역분개 전표 발행**: 오류 발생 시 반대 부호의 역분개(Reversal) 전표를 생성하여 회계적 정합성과 감사 궤적을 100% 보존합니다.

---

## 5. 인프라, 격리 및 관측성 아키텍처

### 5.1 실행 프로파일 체계
- **`local` 프로파일**: H2 인메모리 DB를 사용하여 외부 의존성(PostgreSQL, Eureka, Kafka) 없이 단독 `./gradlew :module:api:bootRun` 및 초고속 단위 테스트 수행.
- **`dev` (external-dev / self-contained)**: Docker/Podman 기반으로 실제 17개 PostgreSQL DB 컨텍스트, Redis, 플랫폼 서비스와 결합하여 검증.
- **`prod` 프로파일**: 불변(Immutable) 컨테이너 이미지와 Vault 시크릿, 외부 공인 PostgreSQL 클러스터 기반 배포.

### 5.2 관측성 인프라 (Observability)
- **메트릭 (Prometheus & Grafana)**: Spring Boot Actuator `/actuator/prometheus` 엔드포인트를 15초 주기로 스크랩하여 TSDB 적재 및 대시보드 시각화.
- **중앙 로그 (ELK Stack)**: Logstash TCP 5000 리스너를 통해 JSON 로그를 인덱싱하고 Kibana에서 전사 로그 검색 제공.
- **분산 추적 (Zipkin)**: Micrometer Tracing을 통해 Gateway부터 마이크로서비스 간 HTTP/Kafka 요청 흐름을 Trace ID 기반으로 추적.

---

## 6. 전역 문서 참조 및 아키텍처 허브

- **[전사 문서 허브 (Repository Docs Hub)](../README.md)**
- **[도메인 카탈로그 (Domain Catalog)](./domain-catalog.md)**
- **[비즈니스 업무 시나리오 (Business Workflow)](./business_workflow.md)**
- **[헥사고날 & DDD 실무 가이드](../guides/hexagonal-ddd-architecture-guide.md)**
- **[Nginx SSL 및 단일 진입점 가이드](../guides/ssl-nginx-reverse-proxy-guide.md)**
- **[개발 환경 Compose 가이드](../guides/development-compose.md)**
- **[품질 감사 리포트](../TOTAL_QUALITY_REPORT.md)**

---
**문서 소유자**: 전사 아키텍처 팀
**최종 갱신일**: 2026-09-07 (최신 MSA/BFF/관측성 토폴로지 정합화)
