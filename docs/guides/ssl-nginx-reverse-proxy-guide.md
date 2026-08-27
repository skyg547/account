# Nginx SSL/TLS 인증서 구축 및 단일 진입점(Single Entry Point) 라우팅 가이드

이 가이드는 Account MSA 시스템의 프론트엔드, API 게이트웨이, 모니터링/DB 대시보드를 **Nginx 단일 관문(포트 80/443)**으로 통합하고, **SSL/TLS 암호화(HTTPS)**를 구축하는 표준 아키텍처 및 실무 가이드를 제공합니다.

---

## 1. 📌 단일 진입점(Single Entry Point) 아키텍처 개요

### 1.1 개별 포트 노출의 한계 (Legacy Architecture)
전통적인 마이크로서비스 배포에서는 각 컴포넌트의 포트를 호스트에 개별 노출(Port Mapping)했습니다.
- Next.js 웹 프론트엔드: `3000`
- Spring Cloud Gateway API: `8000`
- Grafana 서버 메트릭: `3001`
- Kibana 로그 분석 콘솔: `5601`
- Zipkin 분산 트레이싱: `9411`
- pgAdmin PostgreSQL 웹 GUI: `5050`

이 방식은 다음과 같은 심각한 보안 및 운영 문제를 야기합니다:
1. **보안 공격 표면(Attack Surface) 확장**: 불필요한 포트가 외부에 열려 무차별 대입(Brute-force) 공격 위험 증가.
2. **인증서 관리 분산**: 각 서비스마다 SSL을 개별 적용할 수 없어 평문(HTTP) 통신에 노출되거나 관리가 파편화됨.
3. **CORS / 쿠키 보안 제약**: 포트가 서로 달라 브라우저의 동일 출처 정책(SOP)에 따른 복잡한 CORS 설정 및 `HttpOnly`/`SameSite` 쿠키 공유 제약 발생.

### 1.2 Nginx 단일 관문 통합 (Zero Direct Port Exposure)
모든 외부 인바운드 트래픽은 **Nginx(80/443)** 단 하나만을 통과하며, URL 서브패스(Sub-path)에 따라 Docker 내부 가상 네트워크(`account-network`)를 통해 각 컨테이너로 안전하게 중계(Reverse Proxy)됩니다.

```text
[클라이언트 브라우저 / 외부 API 호출자]
               │ HTTPS (443) / HTTP (80 ➔ 301 Redirect)
               ▼
   ┌──────────────────────────────────────────────┐
   │     frontend-nginx (Reverse Proxy)           │
   │  - SSL Termination (인증서 암복호화 일원화)  │
   │  - Security Headers (Clickjacking/XSS 방어) │
   └───────┬──────────────┬──────────────┬────────┘
           │              │              │ (account-network 내부 통신)
           ▼              ▼              ▼
     [frontend:3000] [gateway:8000] [grafana:3000] ... [kibana / zipkin / pgadmin]
```

### 1.3 HTTP(80) ➔ HTTPS(443) 301 영구 리다이렉트 원리
비암호화 포트(80)로 들어오는 모든 요청은 HTTP 응답 코드 `301 Moved Permanently`와 함께 `Location: https://<HOST><URI>` 헤더를 반환하여 안전한 HTTPS 채널로 즉시 전환시킵니다.

---

## 2. 🔑 SSL/TLS 인증서 3대 구축 가이드

환경(도메인 보유 여부, 사설망 여부 등)에 따라 가장 적합한 방식을 선택하여 적용합니다.

### 2.1 [방법 1] Cloudflare Tunnel (가장 권장 - 인증서 설치 불필요 완전 자동화)
- **적용 대상**: 공인 도메인을 보유하고 있으며, 라우터 포트포워딩(80/443) 없이 즉시 공인 HTTPS를 구축하고자 할 때.
- **원리**: 서버 내부에서 실행되는 `cloudflared` 에이전트가 Cloudflare Edge 네트워크와 아웃바운드 암호화 터널을 수립합니다. 인바운드 포트를 열지 않아도 Cloudflare가 전 세계 사용자에게 유효한 공인 SSL 인증서를 제공합니다.

#### 설정 절차:
1. Cloudflare 대시보드(Zero Trust ➔ Networks ➔ Tunnels)에서 신규 터널 생성.
2. `cloudflared` 토큰 복사 후 `docker-compose.yml`에 서비스 추가:
   ```yaml
   services:
     cloudflared:
       image: cloudflare/cloudflared:latest
       restart: unless-stopped
       command: tunnel --no-autoupdate run --token ${CLOUDFLARE_TUNNEL_TOKEN}
       networks:
         - account-network
   ```
3. Cloudflare 대시보드 Public Hostname 설정:
   - `account.yourdomain.com` ➔ `http://frontend-nginx:80`

---

### 2.2 [방법 2] Let's Encrypt + Certbot (공인 도메인 및 직접 호스팅 표준)
- **적용 대상**: 공인 IP와 도메인이 연결되어 있고 서버 80/443 포트가 개방되어 있는 환경.
- **원리**: 무료 공인 인증기관인 Let's Encrypt로부터 ACME 프로토콜(HTTP-01 챌린지)을 통해 인증서를 자동 발급받고 90일마다 자동 갱신합니다.

#### 1) 챌린지 디렉토리 준비 및 최초 발급
```bash
# Certbot 챌린지 및 인증서 저장용 디렉토리 생성
mkdir -p ./certbot/conf ./certbot/www

# Certbot을 통한 인증서 최초 발급 (웹루트 방식)
docker run -it --rm --name certbot \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d account.yourdomain.com \
  --email admin@yourdomain.com \
  --agree-tos --no-eff-email
```

#### 2) Nginx에 인증서 볼륨 마운트
`frontend-nginx/docker-compose.yml`:
```yaml
services:
  frontend-nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./certbot/conf:/etc/letsencrypt:ro
      - ./certbot/www:/var/www/certbot:ro
```

---

### 2.3 [방법 3] OpenSSL 자체 서명 인증서 (사설 IP / 로컬 개발망 1분 생성)
- **적용 대상**: 도메인이 없는 사설 IP(`192.168.x.x`) 개발 서버 또는 로컬(`localhost`) 환경.
- **원리**: 자체 생성한 개인키로 서명한 X.509 인증서를 생성하여 개발망에서도 프로덕션과 동일한 HTTPS 암호화 동작을 검증합니다.

#### 자체 서명 인증서 원라이너 생성 명령어 (SAN 포함):
```bash
# 인증서 저장 폴더 생성
mkdir -p ./frontend-nginx/certs

# OpenSSL 2048-bit RSA 키 및 365일 유효 인증서 생성
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ./frontend-nginx/certs/selfsigned.key \
  -out ./frontend-nginx/certs/selfsigned.crt \
  -subj "/C=KR/ST=Seoul/L=Gangnam/O=Account/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

---

## 3. 🛠️ Nginx HTTPS 통합 리버스 프록시 템플릿

아래는 HTTP ➔ HTTPS 자동 리다이렉트와 6대 서비스 서브패스 라우팅이 완비된 `nginx.conf` 프로덕션 템플릿입니다.

```nginx
# WebSocket 업그레이드 매핑
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

# ------------------------------------------------------------------------------
# 1. HTTP Server (Port 80) -> HTTPS (Port 443) 301 강제 리다이렉트
# ------------------------------------------------------------------------------
server {
    listen 80;
    listen [::]:80;
    server_name localhost;

    # Let's Encrypt ACME 챌린지 경로 허용
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# ------------------------------------------------------------------------------
# 2. HTTPS Server (Port 443) - SSL Termination & Sub-path Reverse Proxy
# ------------------------------------------------------------------------------
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name localhost;

    # SSL 인증서 경로 지정 (자체 서명 또는 Let's Encrypt)
    ssl_certificate /etc/nginx/certs/selfsigned.crt;
    ssl_certificate_key /etc/nginx/certs/selfsigned.key;

    # 모던 SSL/TLS 보안 프로토콜 및 암호화 스위트 (TLSv1.2, TLSv1.3)
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;

    # 보안 강화 헤더
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 클라이언트 바디 및 타임아웃
    client_max_body_size 50M;
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;

    # 1) Next.js 정적 빌드 자원 캐싱
    location /_next/static/ {
        proxy_pass http://frontend:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        expires 365d;
        access_log off;
    }

    # 2) Spring Cloud Gateway API (/api/ -> gateway:8000)
    location /api/ {
        proxy_pass http://gateway:8000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Prefix /api;
        proxy_set_header X-Forwarded-Host $host;
    }

    # 3) Grafana 서버 메트릭 대시보드 (/grafana/ -> grafana:3000)
    location /grafana/ {
        proxy_pass http://grafana:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }

    # 4) Zipkin 분산 트레이싱 (/zipkin/ -> zipkin:9411/zipkin/)
    location /zipkin/ {
        proxy_pass http://zipkin:9411/zipkin/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    # 5) Kibana 통합 로그 검색 (/kibana/ -> kibana:5601)
    location /kibana/ {
        proxy_pass http://kibana:5601;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Prefix /kibana;
    }

    # 6) pgAdmin DB 관리자 콘솔 (/pgadmin/ -> pgadmin:80)
    location /pgadmin/ {
        proxy_pass http://pgadmin:80/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Script-Name /pgadmin;
        proxy_redirect off;
    }

    # 7) Next.js 웹 프론트엔드 기본 루트 (/ -> frontend:3000)
    location / {
        proxy_pass http://frontend:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

---

## 4. 🚀 컨테이너 실행 및 검증 절차

### 4.1 Podman / Docker Compose 실행
```bash
# frontend-nginx 서비스 빌드 및 기동
docker compose -f frontend-nginx/docker-compose.yml up -d --build
```

### 4.2 cURL을 활용한 검증 스크립트
```powershell
# 1. HTTP 80 포트 요청 시 301 HTTPS 리다이렉트 응답 확인
curl -I http://localhost/

# 2. HTTPS 메인 프론트엔드 200 OK 응답 확인 (-k: 자체서명 인증서 허용)
curl -k -I https://localhost/

# 3. Gateway API 헬스체크 확인
curl -k https://localhost/api/actuator/health

# 4. Grafana 메트릭 대시보드 헬스체크
curl -k -I https://localhost/grafana/api/health
```

---

## 5. 💡 실무 트러블슈팅 FAQ

### Q1. 브라우저에서 `NET::ERR_CERT_AUTHORITY_INVALID` 경고가 뜹니다.
- **원인**: OpenSSL 자체 서명 인증서는 공인 CA가 서명하지 않았기 때문에 브라우저가 보안 경고를 띄웁니다.
- **해결책**:
  - Chrome/Edge: 브라우저 화면 빈 곳을 클릭하고 `thisisunsafe`를 키보드로 타이핑하여 우회.
  - 또는 로컬 개발 시 `chrome://flags/#allow-insecure-localhost`를 활성화(Enabled).

### Q2. Nginx 기동 시 `host not found in upstream "frontend"` 오류로 종료됩니다.
- **원인**: Nginx가 시작될 때 프록시 대상 컨테이너(`frontend`, `gateway`)가 아직 시작되지 않았거나 도커 DNS 해석이 지연됨.
- **해결책**:
  - Nginx 설정에 도커 내장 DNS 리졸버를 명시합니다: `resolver 127.0.0.11 ipv6=off valid=10s;`
  - `docker-compose.yml`의 `depends_on`에 의존 컨테이너를 지정합니다.

### Q3. pgAdmin 접속 시 로그인 후 흰 화면이 나오거나 리다이렉트가 깨집니다.
- **원인**: pgAdmin이 서브패스(`/pgadmin`)를 인식하지 못해 정적 자원을 루트(`/`)에서 요청함.
- **해결책**:
  - Nginx에 `proxy_set_header X-Script-Name /pgadmin;`과 `proxy_redirect off;`를 반드시 추가하고, `proxy_pass http://pgadmin:80/;` 끝에 슬래시(`/`)를 유지합니다.