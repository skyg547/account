# 📈 Grafana (그라파나) - "건강 상태 모니터링 전광판 (시각화 도구)"

## 1. 초보자를 위한 개념 설명
프로메테우스가 각 병실(서버)을 돌아다니며 환자들(Spring Boot)의 체온과 심박수(숫자 데이터)를 재서 노트에 빼곡히 적어두었다면,
**그라파나**는 그 노트를 넘겨받아 개발자가 한눈에 알아보기 쉽게 **'예쁜 그래프와 전광판 대시보드'**로 그려주는 시각화 도구입니다.

---

## 2. 이 폴더의 구성
- `docker-compose.yml`: 그라파나 대시보드 컨테이너 실행 명세서.
- `provisioning/datasources/datasource.yml`: 그라파나 부팅 시 `http://prometheus:9090`을 기본 데이터소스로 자동 연결(`read-only` 마운트)하는 프로비저닝 설정.
- `Dockerfile`: Grafana 공식 이미지를 상속하는 OCI 빌드 파일.

---

## 3. 실행 및 검증 방법

### ⚠️ 필수 선행 조건
- Docker 공유 네트워크 `account-network`가 생성되어 있어야 합니다.

### 🚀 실행 명령어
```bash
# 독립 실행
docker compose -f grafana/docker-compose.yml up -d
```

### 🔍 헬스체크 및 검증
```bash
# 1. 헬스 상태 확인 (HTTP 200)
curl -s http://localhost:3000/api/health

# 2. Nginx 단일 진입점 서브패스 접속
http://localhost/grafana/
```

---

## 4. 롤백(Rollback) 절차
```bash
# Grafana 컨테이너만 안전하게 종료 (데이터 볼륨 및 타 서비스 보존)
docker compose -f grafana/docker-compose.yml down
```