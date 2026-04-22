# 📋 모던 재무 시스템 운영 런북 (Runbook)

이 문서는 모던 재무 시스템(MSA)의 설치, 실행 및 모니터링을 위한 운영 가이드입니다.

---

## 1. 시스템 아키텍처 요약
- **인프라:** Eureka(Discovery), Config Server, Gateway, Auth
- **비즈니스:** Master Data, Journal Ledger (Core)
- **플랫폼:** Kafka(Event), Zipkin(Tracing), ELK(Logging), Prometheus/Grafana(Metrics)

---

## 2. 서비스 실행 순서 (중요)

시스템의 의존 관계에 따라 반드시 아래 순서대로 실행해야 합니다.

### Step 1: 기본 인프라 (Docker)
먼저 필요한 미들웨어를 실행합니다.
1. `cd kafka && docker-compose up -d` (Kafka, Zookeeper)
2. `cd elasticsearch && docker-compose up -d` (ELK Stack)
3. `cd zipkin && docker-compose up -d` (Zipkin)
4. `cd prometheus && docker-compose up -d` (Monitoring)

### Step 2: Spring Cloud 관리 서버
1. `discovery` (Port: 8761)
2. `config-server` (Port: 8888)

### Step 3: 인증 및 게이트웨이
1. `auth` (Port: 8080)
2. `gateway` (Port: 8000)

### Step 4: 비즈니스 서비스
1. `master-data` (Port: 8082)
2. `journal-ledger` (Port: 8081)
3. `asset-lease` (Port: 8083)

---

## 3. 관리자 대시보드 주소

| 서비스명 | URL | 설명 |
| :--- | :--- | :--- |
| **Eureka** | [http://localhost:8761](http://localhost:8761) | 서비스 등록 상태 확인 |
| **Zipkin** | [http://localhost:9411](http://localhost:9411) | 분산 트랜잭션 추적 |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | 시스템 메트릭 시각화 |
| **Kibana** | [http://localhost:5601](http://localhost:5601) | 중앙 집중 로그 통합 검색 |
| **Kafka UI** | [http://localhost:8090](http://localhost:8090) | 이벤트 메시지 모니터링 |

---

## 4. 장애 대응 및 유지보수

### 로그 확인
- 실시간 로그: `kubectl logs -f [pod-name]` (K8s) 또는 각 서비스의 로그 파일 확인.
- 전체 통합 로그: Kibana 대시보드에서 `service-name` 필드로 필터링.

### 원장 잔액 불일치 시 (재집계 배치 실행)
만약 전표 데이터와 잔액이 맞지 않는 상황이 발생하면 수동으로 배치를 실행합니다.
```bash
./gradlew :journal-ledger:batch:bootRun
```

### 새로운 룰 적용
`Journal Rule Engine`은 DB 기반이므로, 별도의 재시작 없이 `journal_rules` 테이블에 데이터를 입력하는 즉시 Kafka 이벤트 처리에 반영됩니다.
