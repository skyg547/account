# 🔍 Kibana (키바나) - "로그 검색 및 분석 대시보드"

## 1. 초보자를 위한 개념 설명
Elasticsearch에 쌓인 수많은 MSA 서비스 로그들을 웹 화면에서 검색창에 텍스트를 입력하여 실시간으로 확인하고 시각화할 수 있는 관리자 화면입니다.

---

## 2. 이 폴더의 구성
- `kibana.yml`: 키바나 웹 서버 및 Elasticsearch 연결 설정 (Nginx 서브패스 `/kibana` 포함).
- `docker-compose.yml`: 키바나 실행 명세서 (Elasticsearch green/yellow 헬스체크 의존성 및 리소스 상한선 포함).

---

## 3. 실행 및 검증 방법

### ⚠️ 필수 선행 조건
- Docker 공유 네트워크 `account-network`와 `elasticsearch` 컨테이너가 먼저 실행되어 있어야 합니다.

### 🚀 실행 명령어
```bash
# 키바나 독립 실행
docker compose -f kibana/docker-compose.yml up -d
```

### 🔍 헬스체크 및 검증
```bash
# 1. 키바나 상태 확인 (available / warn 레벨)
curl -s http://localhost:5601/api/status

# 2. Nginx 단일 진입점 서브패스 접속
http://localhost/kibana
```

---

## 4. 롤백(Rollback) 절차
```bash
# Kibana 컨테이너만 안전하게 종료 (Elasticsearch 데이터 보존)
docker compose -f kibana/docker-compose.yml down
```