# 🧱 Docker Compose 컨테이너별 자원 제한(Resource Limits) 표준화 가이드

이 문서는 Account 재무/회계 마이크로서비스 시스템의 **4코어 12GB RAM 호스트 환경에 최적화된 컨테이너별 CPU/메모리 자원 상한선(Resource Limits) 및 예약량(Reservations) 표준 명세**입니다.

---

## 1. 🎯 도입 목적 및 배경

### ❌ 리소스 상한선 부재 시의 위험 (연쇄 장애 / Cascading Failure)
- 20여 개 MSA 컨테이너 중 한 프로세스(예: Next.js SSR 프론트엔드, Elasticsearch 인덱서, 대용량 배치)가 메모리를 비정상적으로 과다 점유(OOM)할 경우, **호스트 서버의 물리 메모리가 고갈되어 다른 모든 비즈니스 API가 강제 종료(Killed)**되는 치명적인 위험이 발생합니다.

### ⭕ 자원 상한선 도입 효과
- Docker Compose의 `deploy.resources.limits` 및 `reservations`를 표준화하여,
- **각 컨테이너의 최대 메모리/CPU 사용량을 엄격히 격리(Isolation)**함으로써 단일 서비스 장애가 전사 시스템으로 전파되지 않도록 완벽히 방어합니다.

---

## 2. 📊 전사 표준 리소스 할당 매트릭스 (4-Core / 12GB Host 기준)

| 계층 (Tier) | 서비스명 | CPU Limit | RAM Limit | RAM Reservation | 비고 및 튜닝 옵션 |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **인프라/DB** | `postgres-db` | 0.50 | 1024MB | 256MB | 17개 도메인 스키마 공유 인스턴스 |
| **인프라/캐시** | `redis` | 0.25 | 256MB | - | 분산 락, 토큰 캐시 전용 |
| **인프라/메시징**| `kafka` | 1.00 | 1024MB | 512MB | 단일 브로커/컨트롤러 모드 |
| **플랫폼 코어** | `config-server` | 0.50 | 768MB | 256MB | Spring Native/Git 설정 서버 |
| **플랫폼 코어** | `discovery` | 0.50 | 768MB | 256MB | Eureka 서비스 레지스트리 |
| **플랫폼 코어** | `gateway` | 0.50 | 768MB | 256MB | Spring Cloud Gateway 단일 진입점 |
| **도메인 API** | `*:api` (17개) | 0.50 | 768MB | 256MB | Inbound REST API (`x-api-service`) |
| **도메인 Batch**| `*:batch` (15개) | 1.00 | 1024MB | 256MB | 대량 Chunk 처리 CLI (`x-batch-service`) |
| **프론트엔드** | `frontend` | 1.00 | 1024MB | 256MB | Next.js 14 SSR Node.js 런타임 |
| **웹 프록시** | `frontend-nginx`| 0.25 | 128MB | - | Nginx 리버스 프록시 단일 진입점 |
| **관측성** | `prometheus` | 0.50 | 512MB | - | 시계열 메트릭 TSDB |
| **관측성** | `grafana` | 0.50 | 256MB | - | 모니터링 시각화 대시보드 |
| **관측성** | `zipkin` | 0.50 | 512MB | - | 분산 트레이싱 수집기 |
| **관측성/로그** | `elasticsearch` | 1.00 | 1536MB | 512MB | `ES_JAVA_OPTS=-Xms512m -Xmx512m` |

---

## 3. 🛠️ Compose IaC 명세 표준 템플릿

```yaml
# Spring Boot API 표준 (x-api-service)
x-api-service: &api-service
  restart: on-failure
  init: true
  deploy:
    resources:
      limits:
        cpus: "0.50"
        memory: 768m
      reservations:
        memory: 256m
  logging: *default-logging
  networks: [account-network]

# Spring Batch 표준 (x-batch-service)
x-batch-service: &batch-service
  restart: "no"
  init: true
  deploy:
    resources:
      limits:
        cpus: "1.00"
        memory: 1024m
      reservations:
        memory: 256m
  logging: *default-logging
  networks: [account-network]
```

---

## 4. 🔍 런타임 리소스 모니터링 및 검증

컨테이너 기동 후 실제 리소스 점유율은 아래 명령어로 실시간 모니터링할 수 있습니다:

```bash
# 실시간 컨테이너별 CPU / RAM 점유율 스냅샷
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
```