# 🏗️ 모던 재무 시스템 인프라 완벽 가이드 (초보자용)

우리가 구축한 MSA 인프라가 각각 어떤 역할을 하는지, 왜 도입했는지 한눈에 파악할 수 있도록 비유와 시각화 다이어그램을 통해 정리했습니다.

---

## 0. MSA 인프라 토폴로지 (Infrastructure Topology)

```mermaid
flowchart TD
    subgraph "External Clients"
        UI[Frontend App / Browser]
    end

    subgraph "API Gateway Layer"
        GW[API Gateway\n(Port: 8080)]
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

    subgraph "Observability (모니터링 & 로깅)"
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
    MS2 --> DB
    MS3 --> DB
    MS4 --> DB
    
    MS1 -.-> KAFKA
    MS2 -.-> KAFKA
    
    MS1 -.-> REDIS
    MS2 -.-> REDIS

    MS1 -.->|Logs & Metrics| ELK
    MS2 -.->|Traces| ZIPKIN
    MS3 -.->|Metrics| PROM
    
    style EUREKA fill:#ff9,stroke:#333,stroke-width:2px
    style GW fill:#bbf,stroke:#333,stroke-width:2px
    style KAFKA fill:#dfd,stroke:#333,stroke-width:2px
```

---

## 1. 뼈대와 통신망 (Core MSA Infrastructure)
*   **🗺️ Eureka (길찾기 앱):** 각 마이크로서비스가 어디(IP, Port)에 있는지 알려주는 '사내 전화번호부'입니다. (`discovery` 모듈)
*   **📜 Config Server (프랜차이즈 본사):** 수십 개로 쪼개진 서비스들의 `application.yml` 설정을 한 곳(`config-repo`)에서 통합 관리하고 실시간으로 배포합니다.
*   **🚪 API Gateway (호텔 1층 안내데스크):** 모든 클라이언트 요청을 한 곳(8080 포트)에서 받아 길을 안내(Routing)합니다.
    *   **Resilience4j (두꺼비집):** 뒷단 서버가 죽으면 게이트웨이가 두꺼비집을 내려 연쇄 장애를 막고 친절한 에러를 반환합니다 (서킷 브레이커).
*   **🔑 Auth Service (출입증 발급 센터):** 사용자가 로그인하면 위조 불가능한 출입증(JWT 토큰)을 발급합니다. 게이트웨이는 이 출입증을 검사합니다.

## 2. 관제 및 모니터링 (Observability)
*   **🌲 ELK Stack (중앙 블랙박스):** `elasticsearch`, `logstash`, `kibana`. 수많은 서버에 흩어진 에러 텍스트 로그를 한곳에 모아 구글처럼 0.1초 만에 검색하게 해줍니다.
*   **📊 Prometheus & Grafana (건강 검진 전광판):** 프로메테우스가 각 서버를 돌며 CPU, 메모리 등의 '체온'을 기록하면, 그라파나가 이를 예쁜 대시보드로 띄워줍니다.
*   **📦 Zipkin (배송 조회 시스템):** 에러가 났을 때, 게이트웨이부터 시작된 이 요청이 어느 서버에서 몇 초 만에 터졌는지 '운송장 번호(Trace ID)'로 한눈에 추적하게 해줍니다.

## 3. 동기 및 비동기 소통 (Communication)
*   **📞 OpenFeign (스마트 사내 전화기):** A서버가 B서버에게 당장 데이터를 물어봐야 할 때(동기 통신), 복잡한 HTTP 주소 세팅 없이 자바 인터페이스 하나로 유레카 및 서킷브레이커와 연동되어 우아하게 전화를 걸게 해줍니다.
*   **📻 Kafka (마을 대형 전광판):** A서버가 B서버에게 직접 전화하지 않고, "전표 승인됨!" 이벤트를 전광판에 띄웁니다. B서버는 자기가 안 바쁠 때 전광판을 읽고 일합니다(비동기 통신). 

## 4. 데이터베이스 안정성 (Database Management)
*   **🏗️ Flyway (DB 설계도 버전 관리):** `ddl-auto: update`라는 시한폭탄 대신, `V1__init.sql` 처럼 DB의 변경 내역(도면)을 차곡차곡 버전으로 관리하여 안전하게 테이블을 생성해 줍니다.

---

## 5. [진행 중] 협업 및 확장, 보안 고급 인프라 (Advanced MSA)
*   **📖 Swagger / OpenAPI (통합 메뉴판):**
    프론트엔드 개발자가 API 명세서를 따로 물어볼 필요 없이, 코드를 짜면 자동으로 예쁜 웹페이지 메뉴판(`localhost:8080/swagger-ui.html`)을 만들어 줍니다. 대문(Gateway)에 접속하면 모든 매장의 메뉴를 한 번에 볼 수 있습니다.
*   **🤝 Spring Cloud Contract (API 연동 각서 검증기):**
    팀 간의 API 스펙 약속을 문서(각서)로 쓰고, 개발자가 실수로 파라미터 이름을 바꾸면 배포를 막아버리는 '소비자 주도 계약 테스트' 도구입니다.
*   **🚀 Redis (초고속 화이트보드 및 자물쇠):**
    자주 쓰는 데이터를 0.01초 만에 꺼내주는 캐시 역할과, 따닥! 두 번 눌러서 전표가 2번 끊기는 대참사를 막아주는 분산 락(자물쇠)의 핵심 무기입니다.
*   **🔒 HashiCorp Vault (스위스 은행 비밀 금고):**
    소스코드에 텍스트로 적혀있던 DB 비밀번호, JWT 암호화 키를 모조리 이 금고에 숨기고, 스프링 부트가 켜질 때만 안전하게 꺼내오게 합니다.

---

## 6. 컨테이너 배포 환경 (Dockerization)
*   **🐳 모듈별 독립 컨테이너:**
    모든 MSA 서비스 및 인프라 모듈(예: `journal-ledger`, `elasticsearch` 등) 디렉토리 내에 독립적인 `Dockerfile` 및 `docker-compose.yml`이 생성되어 있습니다.
*   **실행 방법:**
    특정 모듈 폴더로 이동 후 `docker-compose up -d` 명령어를 통해 각 서비스를 즉시 컨테이너 환경으로 구동할 수 있습니다. (Spring Boot 애플리케이션의 경우 사전에 `./gradlew build`로 `.jar` 생성이 필요합니다.)
