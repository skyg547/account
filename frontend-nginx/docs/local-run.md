# frontend-nginx 로컬 실행 및 검증 가이드 (Local Run & Verification)

## 1. 전제 조건
- Docker / Podman 및 Compose provider 설치
- `account-network` 도커 네트워크 생성 상태

## 2. 이미지 빌드 및 실행
```bash
# Nginx 컨테이너 빌드 및 백그라운드 실행
docker compose -f frontend-nginx/docker-compose.yml up -d --build
```

## 3. 라우팅 검증
```powershell
# 프론트엔드 HTML 응답 확인
(Invoke-WebRequest -Uri "http://localhost/").StatusCode

# Gateway API 상태 확인
(Invoke-WebRequest -Uri "http://localhost/api/actuator/health").StatusCode

# Grafana 응답 확인
(Invoke-WebRequest -Uri "http://localhost/grafana/").StatusCode
```

## 4. 컨테이너 중지
```bash
docker compose -f frontend-nginx/docker-compose.yml down
```