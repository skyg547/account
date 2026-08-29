# 📊 Prometheus (프로메테우스) - "MSA 시계열 메트릭 수집 엔진"

## 1. 초보자를 위한 개념 설명
프로메테우스는 각 MSA 서비스(Spring Boot)의 `/actuator/prometheus` 엔드포인트를 주기적으로 방문(Scraping)하여,
**CPU 사용률, 메모리 점유율, 초당 HTTP 요청 수(TPS), 응답 지연시간** 등의 숫자를 수집하고 저장하는 시계열 데이터베이스입니다.

---

## 2. 이 폴더의 구성
- `prometheus.yml`: 메트릭을 수집할 대상 서비스(auth, master-data, discovery, config-server, gateway) 설정.
- `docker-compose.yml`: 프로메테우스 컨테이너 실행 명세서 (설정 파일 `read-only` 마운트 및 리소스 상한선 포함).

---

## 3. 실행 및 검증 방법

### ⚠️ 필수 선행 조건
- Docker 공유 네트워크 `account-network`가 생성되어 있어야 합니다.

### 🚀 실행 명령어
```bash
# 프로메테우스 독립 실행
docker compose -f prometheus/docker-compose.yml up -d
```

### 🔍 헬스체크 및 검증
```bash
# 1. 프로메테우스 준비 상태 확인 (HTTP 200)
curl -s http://localhost:9090/-/ready

# 2. 타깃 수집 현황 확인
curl -s http://localhost:9090/api/v1/targets
```

---

## 4. 롤백(Rollback) 절차
```bash
# 프로메테우스 컨테이너만 안전하게 종료 (데이터 볼륨 및 타 서비스 보존)
docker compose -f prometheus/docker-compose.yml down
```