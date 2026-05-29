# 🌐 MSA 인프라 운영 및 실행 가이드 (Infrastructure Runbook)

본 문서는 시스템의 뼈대를 구성하는 인프라 모듈(Discovery, Config, Auth, Gateway)과 미들웨어(Kafka, ELK, Prometheus 등)의 역할, 실행 순서, 트러블슈팅 방법을 통합한 마스터 운영 매뉴얼입니다.

---

## 1. 인프라 아키텍처 및 토폴로지

우리 시스템은 4개의 핵심 인프라 서비스와 모니터링/메시징 플랫폼이 유기적으로 연결되어 작동합니다.

```mermaid
flowchart TD
    subgraph "External Clients"
        UI[Frontend App / Browser]
    end

    subgraph "API Gateway Layer"
        GW[API Gateway\n(Port: 8080/8000)]
        CB[Resilience4j\n(Circuit Breaker)]
        GW -.-> CB
    end

    subgraph "Service Discovery & Config"
        EUREKA((Eureka Server\nPort: 8761))
        CONFIG[Config Server\nPort: 8888]
    end

    subgraph "Backend Microservices"
        MS1[Master Data Service]
        MS2[Journal Ledger Service]
        MS3[Closing & Reporting]
        MS4[Finance Subledgers]
    end

    subgraph "Data & Messaging"
        DB[(PostgreSQL)]
        KAFKA[[Apache Kafka\nMessage Broker]]
        REDIS[(Redis\nCache & Lock)]
    end

    subgraph "Observability (관제)"
        ZIPKIN[Zipkin\nTrace ID]
        ELK{{ELK Stack\nElasticsearch, Logstash, Kibana}}
        PROM[Prometheus & Grafana]
    end

    UI -->|HTTPS Request| GW
    
    GW -.->|Routing| MS1
    GW -.->|Routing| MS2
    GW -.->|Routing| MS3
    GW -.->|Routing| MS4
    
    MS1 <-->|Register & Fetch| EUREKA
    MS2 <-->|Register & Fetch| EUREKA
    MS3 <-->|Register & Fetch| EUREKA
    MS4 <-->|Register & Fetch| EUREKA
    
    CONFIG -.->|Push Properties| MS1
    CONFIG -.->|Push Properties| MS2
    CONFIG -.->|Push Properties| MS3
    CONFIG -.->|Push Properties| MS4

    MS1 --> DB
    MS1 -.-> KAFKA
    MS1 -.-> REDIS
    MS1 -.->|Logs & Metrics| ELK
    MS2 -.->|Traces| ZIPKIN
    MS3 -.->|Metrics| PROM
    
    style EUREKA fill:#ff9,stroke:#333,stroke-width:2px
    style GW fill:#bbf,stroke:#333,stroke-width:2px
    style KAFKA fill:#dfd,stroke:#333,stroke-width:2px
```

---

## 2. 서비스 실행 순서 (Essential Order)

서비스 간 의존성 때문에 반드시 아래 단계를 준수하여 기동해야 합니다.

### [Phase 1] 공통 인프라 (Docker)
먼저 필요한 미들웨어를 실행합니다. 각 폴더로 이동하여 `docker-compose up -d`를 실행하십시오.
1. **Kafka/Zookeeper**: 이벤트 메시지 브로커
2. **ELK Stack**: 중앙 집중 로깅 (Elasticsearch, Logstash, Kibana)
3. **Zipkin**: 분산 트랜잭션 추적
4. **Prometheus/Grafana**: 시스템 메트릭 모니터링
5. **Redis/Vault**: 캐시, 락 및 비밀 금고 (선택 사항)

### [Phase 2] Spring Cloud 관리 서버
1. **Config Server (Port: 8888)**: 설정 우체국. `config-repo`의 절대 경로를 인식시켜야 합니다.
   ```bash
   ./gradlew :config-server:bootRun --args='--spring.profiles.active=native --server.port=8888 --spring.cloud.config.server.native.search-locations=file:///<프로젝트_경로>/config-repo'
   ```
2. **Discovery Service (Port: 8761)**: 사내 전화번호부(Eureka).
   ```bash
   ./gradlew :discovery:bootRun --args='--server.port=8761 --eureka.client.register-with-eureka=false --eureka.client.fetch-registry=false'
   ```

### [Phase 3] 보안 및 관문
1. **Auth Service (Port: 8084)**: 출입증(JWT) 발급. Vault가 없다면 끄고 실행합니다.
   ```bash
   ./gradlew :auth:bootRun --args='--server.port=8084 --spring.cloud.vault.enabled=false'
   ```
2. **Gateway Service (Port: 8080/8000)**: 단일 진입점. 모든 요청을 담당 서비스로 라우팅합니다.

### [Phase 4] 비즈니스 서비스
1. `master-data` (8082), `journal-ledger` (8081), `asset-lease` (8083) 등 순차 실행.

---

## 3. 관리자 대시보드 목록

| 서비스명 | URL (Local) | 주요 확인 사항 |
| :--- | :--- | :--- |
| **Eureka** | [http://localhost:8761](http://localhost:8761) | `Instances registered` 목록에 서비스 노출 여부 |
| **Kibana** | [http://localhost:5601](http://localhost:5601) | 통합 에러 로그 검색 (`service-name` 필터) |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | 서버 자원 및 지표 시각화 |
| **Zipkin** | [http://localhost:9411](http://localhost:9411) | 요청 지연 구간 및 Trace ID 추적 |
| **Kafka UI** | [http://localhost:8090](http://localhost:8090) | 토픽별 메시지 인입 현황 |

---

## 4. 트러블슈팅 (Troubleshooting)

| 증상 | 원인 | 해결책 |
| :--- | :--- | :--- |
| **Port already in use** | 이전 프로세스가 종료되지 않음 | `netstat -ano \| findstr :<PORT>` 후 해당 PID 강제 종료 |
| **Vault Connection Error** | 보안 금고 서버 미기동 | 실행 인자에 `--spring.cloud.vault.enabled=false` 추가 |
| **Eureka 정보 누락** | Discovery 기동 시 클라이언트 설정 간섭 | `--eureka.client.enabled=false`를 지우고 `register/fetch`만 false로 설정 |
| **Config 로딩 실패** | `config-repo` 경로 오규 | `search-locations`에 `file:///` 형식을 포함한 **절대 경로** 입력 확인 |

---

## 5. 운영 팁 (Best Practices)

- **정지 및 재시작:** 서비스가 죽었다 살아나도 Eureka가 자동으로 위치를 갱신하므로 안심해도 됩니다.
- **원장 잔액 불일치:** 전표와 잔액이 맞지 않으면 수동 재집계 배치를 실행하십시오. (`./gradlew :journal-ledger:batch:bootRun`)
- **컨테이너 환경:** 각 모듈 폴더 내 `Dockerfile`을 통해 독립 빌드가 가능하며, `docker-compose.yml`로 로컬 개발 환경을 빠르게 구성할 수 있습니다.
