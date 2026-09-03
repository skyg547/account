# Nginx SSL/TLS 인증서 설정 및 단일 진입점(Single Entry Point) 라우팅 가이드

이 문서는 개발·스테이징·운영 환경에서 다수의 서비스 포트를 외부에 개별 노출하지 않고, **Nginx 단일 진입점(포트 80/443)**을 통해 모든 트래픽을 안전하게 중계·암호화(SSL/TLS Termination)하는 표준 절차를 정의합니다.

또한 **Cloudflare Tunnel**, **Let's Encrypt (Certbot)**, **OpenSSL 자체 서명/로컬 Dev CA** 3가지 인증서 구축 방식을 독립된 절차로 분리하여 설명합니다.

---

## 📌 0. 핵심 원칙 및 선행 조건

### 0.1 포트 노출 제로화 (Zero Direct Port Exposure)
- **외부 공개 포트 최소화**: 호스트 방화벽 및 클라우드 보안 그룹에서는 오직 **Nginx(80/443)** 또는 **Cloudflare Tunnel(아웃바운드)**만 허용합니다.
- **내부 격리 통신**: 프론트엔드(3000), 게이트웨이(8000), 모니터링(그라파나/집킨/키바나), DB 관리자(pgAdmin) 및 백엔드 MSA 마이크로서비스들은 호스트 포트를 직접 열지 않고, 내부 컨테이너 브리지 네트워크(`account-network` 또는 `account-prod`) 내부에서만 통신합니다.

```
[ 클라이언트 / 브라우저 ]
         │
         ▼  (HTTPS 443 / HTTP 80 -> 301 Redirect)
┌─────────────────────────────────────────────────────────────┐
│  Nginx Reverse Proxy & SSL Termination (Port 80 / 443)      │
└──────────────────────────┬──────────────────────────────────┘
                           │ (내부 컨테이너 네트워크: account-network)
     ┌─────────────────────┼─────────────────────┬─────────────────────┐
     ▼                     ▼                     ▼                     ▼
Next.js 웹          Spring Cloud Gateway      Grafana / Zipkin      pgAdmin
(account-frontend)   (account-gateway)         (모니터링 대시보드)   (account-pgadmin)
:3000                 :8000                     :3000 / :9411         :80
```

### 0.2 단일 진입점 라우팅 토폴로지 및 컨테이너 별칭 (Single Entry Point Routing Topology)

Issue #570 (`agent/570-frontend-nginx-routing`)을 통해 단일 진입점 라우팅 토폴로지와 서비스 DNS 별칭 정규화가 완료되었습니다. 모든 내부 서비스는 `account-network` 내에서 네트워크 별칭(Network Alias)을 통해 상호 통신하며, 호스트 포트는 Nginx(80/443) 외에 직접 노출되지 않습니다.

#### 🌐 서브패스 라우팅 및 환경변수 정규화 명세표 (Routing Matrix)

| 서브패스 (Sub-path) | 대상 업스트림 (Target Upstream) | 포트 | 목적 및 설명 | 주요 프록시 헤더 및 컨테이너 환경변수 |
| :--- | :--- | :--- | :--- | :--- |
| `/` | `account-frontend` | 3000 | Next.js 웹 프론트엔드 (SSR & Pages) | `Upgrade $http_upgrade`, `Connection $connection_upgrade` (HMR 지원) |
| `/_next/static/` | `account-frontend` | 3000 | Next.js 정적 빌드 에셋 (정적 캐싱) | `expires 365d; access_log off;` |
| `/api/` | `account-gateway` | 8000 | Spring Cloud Gateway MSA 백엔드 통합 API | `proxy_set_header X-Forwarded-Prefix /api;`<br>`proxy_set_header X-Forwarded-Host $host;`<br>`proxy_set_header X-Forwarded-Proto $scheme;` |
| `/grafana/` | `account-grafana` | 3000 | Grafana 서버 메트릭 대시보드 | `proxy_set_header X-Forwarded-Prefix /grafana;`<br>`GF_SERVER_ROOT_URL=%(protocol)s://%(domain)s:%(http_port)s/grafana/`<br>`GF_SERVER_SERVE_FROM_SUB_PATH=true`<br>`Upgrade`, `Connection` (WebSocket 지원) |
| `/zipkin/` | `account-zipkin` | 9411 | Zipkin 분산 트레이싱 추적 콘솔 | `proxy_pass http://account-zipkin:9411/zipkin/;`<br>`proxy_set_header X-Forwarded-Prefix /zipkin;` |
| `/kibana/` | `account-kibana` | 5601 | Kibana Elasticsearch 통합 로그 검색 | `proxy_set_header X-Forwarded-Prefix /kibana;`<br>`SERVER_BASEPATH=/kibana`<br>`SERVER_REWRITEBASEPATH=true`<br>`Upgrade`, `Connection` (WebSocket 지원) |
| `/pgadmin/` | `account-pgadmin` | 80 | pgAdmin PostgreSQL 데이터베이스 관리자 | `proxy_pass http://account-pgadmin:80/;`<br>`proxy_set_header X-Forwarded-Prefix /pgadmin;`<br>`proxy_set_header X-Script-Name /pgadmin;`<br>`proxy_redirect off;` |

#### 🔒 제로 포트 노출 정책 (Zero Direct Host Port Exposure)
- **표준 운영/개발 토폴로지**: Nginx 리버스 프록시(`account-frontend-nginx`)만 호스트의 80/443 포트를 바인딩하며, `frontend`, `gateway`, `grafana`, `zipkin`, `kibana`, `pgadmin` 컨테이너는 호스트 포트를 직접 게시(`ports:`)하지 않고 내부 `expose:` 및 도커 브리지 네트워크(`account-network`)로 격리됩니다.
- **컨테이너 네트워크 별칭**: 개별 Compose 프로젝트 분리 시에도 Nginx가 일관되게 대상 컨테이너를 찾을 수 있도록 각 서비스 정의에 `networks.account-network.aliases`로 표준 별칭(`account-frontend`, `account-gateway`, `account-grafana`, `account-zipkin`, `account-kibana`, `account-pgadmin`)을 부여합니다.
- **Compose 파일 분리 및 연동**:
  - `docker-compose.yml`: 플랫폼 코어 서비스 (`frontend`, `gateway`, `account-frontend-nginx` 등)
  - `docker-compose.monitoring.yml`: 관측성 및 모니터링 서비스 (`grafana`, `zipkin`, `kibana`, `pgadmin`)
  - `frontend-nginx/docker-compose.yml`: Nginx 단독 기동용 Compose 설정 (외부 Compose 서비스에 대한 직접 `depends_on`을 제거하여 독립 검증 가능)

---

## 🔐 1. SSL/TLS 인증서 3대 구축 가이드

환경과 도메인 보유 여부에 따라 아래 3가지 방식 중 **하나**를 선택하여 독립적으로 적용합니다.

```
┌─────────────────────────┬─────────────────────────┬─────────────────────────┐
│ [방식 A] Cloudflare     │ [방식 B] Let's Encrypt  │ [방식 C] OpenSSL / CA   │
│         Tunnel          │         Certbot         │         자체 서명       │
├─────────────────────────┼─────────────────────────┼─────────────────────────┤
│ • 공인 도메인 필요       │ • 공인 도메인 + 공인 IP │ • 내부망 / 사설 IP      │
│ • 인바운드 포트 개방 0개 │ • 80 포트 HTTP-01 검증   │ • localhost / 폐쇄망    │
│ • SSL 자동 관리 (Edge)  │ • 90일 주기 자동 갱신   │ • 개발 CA 트러스트 등록 │
└─────────────────────────┴─────────────────────────┴─────────────────────────┘
```

---

### 1.1 [방식 A] Cloudflare Tunnel (Zero Trust / 인바운드 포트 불필요)

Cloudflare Tunnel(Argo Tunnel)은 호스트 서버에 인바운드 포트(80/443)를 전혀 개방하지 않고, 아웃바운드 터널 데몬(`cloudflared`)을 통해 Cloudflare Edge와 암호화 터널을 맺습니다.

- **장점**: 공인 IP/DDNS 불필요, 방화벽 포트 포워딩 불필요, Cloudflare Edge에서 SSL/TLS 자동 발급 및 갱신.
- **Nginx 역할**: Nginx는 SSL 종료를 직접 하지 않고, 터널 데몬으로부터 일반 HTTP(80) 트래픽을 전달받아 내부 서브패스로 라우팅합니다.

#### Step 1: Cloudflare Tunnel 토큰 발급
1. [Cloudflare Zero Trust 대시보드](https://one.dash.cloudflare.com/) ➔ **Networks** ➔ **Tunnels** 접속.
2. 새 터널 생성 후 **Docker 환경용 `TUNNEL_TOKEN`** 복사.
3. `.env` 또는 보안 환경변수에 설정 (`CLOUDFLARE_TUNNEL_TOKEN=<your-token-here>`).

#### Step 2: Compose 구성
```yaml
# compose.tunnel.yml
version: '3.8'

services:
  cloudflared:
    image: cloudflare/cloudflared:latest
    container_name: account-cloudflared
    restart: unless-stopped
    command: tunnel run
    environment:
      - TUNNEL_TOKEN=${CLOUDFLARE_TUNNEL_TOKEN:?Set CLOUDFLARE_TUNNEL_TOKEN in untracked .env}
    networks:
      - account-network

  account-frontend-nginx:
    image: nginx:1.25-alpine
    container_name: account-frontend-nginx
    restart: unless-stopped
    volumes:
      - ./frontend-nginx/nginx-tunnel.conf:/etc/nginx/conf.d/default.conf:ro
    networks:
      - account-network

networks:
  account-network:
    name: ${ACCOUNT_NETWORK_NAME:-account-dev-network}
    external: true
```

#### Step 3: Tunnel용 Nginx 설정 (`nginx-tunnel.conf`)
Cloudflare Edge에서 이미 HTTPS 암호화가 완료되므로, Nginx는 내부 80번 포트에서 라우팅만 수행합니다.

```nginx
# frontend-nginx/nginx-tunnel.conf
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

server {
    listen 80;
    server_name _;

    # 프록시 기본 헤더
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;

    # 1) Next.js 정적 자원
    location /_next/static/ {
        proxy_pass http://account-frontend:3000;
        expires 365d;
        access_log off;
    }

    # 2) Gateway API
    location /api/ {
        proxy_pass http://account-gateway:8000;
        proxy_set_header X-Forwarded-Prefix /api;
    }

    # 3) Grafana
    location /grafana/ {
        proxy_pass http://account-grafana:3000;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }

    # 4) Zipkin
    location /zipkin/ {
        proxy_pass http://account-zipkin:9411/zipkin/;
    }

    # 5) Kibana
    location /kibana/ {
        proxy_pass http://account-kibana:5601;
        proxy_set_header X-Forwarded-Prefix /kibana;
    }

    # 6) pgAdmin
    location /pgadmin/ {
        proxy_pass http://account-pgadmin:80/;
        proxy_set_header X-Script-Name /pgadmin;
        proxy_redirect off;
    }

    # 7) Frontend 메인 루트
    location / {
        proxy_pass http://account-frontend:3000;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }
}
```

#### Step 4: Cloudflare 대시보드 Public Hostname 라우팅 설정
- **Domain**: `account.example.com`
- **Service**: `HTTP`://`account-frontend-nginx:80`

---

### 1.2 [방식 B] Let's Encrypt + Certbot (공인 도메인 표준)

공인 도메인(`account.example.com`)과 공인 IP가 있고, 외부 80/443 포트 접속이 가능한 환경에서 무료 공인 인증서를 발급·자동 갱신하는 표준 절차입니다.

#### ⚠️ 최초 발급 부트스트랩 (The Bootstrap Problem & 2-Stage Setup)
Nginx 설정에 `ssl_certificate /etc/letsencrypt/live/...`를 명시한 상태에서 인증서 파일이 아직 존재하지 않으면 **Nginx가 기동 실패(`[emerg] cannot load certificate`)**합니다. 따라서 반드시 **2단계 부트스트랩**으로 진행합니다.

#### Step 1: 사전 볼륨 생성 및 HTTP 부트스트랩 Nginx 기동
먼저 ACME HTTP-01 챌린지만 처리하는 임시 Nginx를 기동합니다.

```bash
# 디렉터리 및 볼륨 준비
mkdir -p ./certbot/conf ./certbot/www ./frontend-nginx

# HTTP-01 챌린지용 임시 부트스트랩 설정 작성
cat << 'EOF' > ./frontend-nginx/nginx-bootstrap.conf
server {
    listen 80;
    server_name account.example.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 "ACME Bootstrap Ready\n";
    }
}
EOF
```

임시 Nginx 실행:
```bash
# Docker 기준 부트스트랩 기동
docker run -d --name account-nginx-bootstrap \
  -p 80:80 \
  -v $(pwd)/frontend-nginx/nginx-bootstrap.conf:/etc/nginx/conf.d/default.conf:ro \
  -v $(pwd)/certbot/www:/var/www/certbot:ro \
  nginx:1.25-alpine
```

#### Step 2: Certbot으로 인증서 최초 발급
```bash
docker run --rm --name certbot-init \
  -v $(pwd)/certbot/www:/var/www/certbot:rw \
  -v $(pwd)/certbot/conf:/etc/letsencrypt:rw \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d account.example.com \
  --email admin@example.com \
  --agree-tos \
  --no-eff-email

# 임시 부트스트랩 컨테이너 정리
docker stop account-nginx-bootstrap && docker rm account-nginx-bootstrap
```

발급 완료 시 파일 생성 위치:
- 인증서 체인: `./certbot/conf/live/account.example.com/fullchain.pem`
- 개인키: `./certbot/conf/live/account.example.com/privkey.pem`

#### Step 3: 프로덕션 HTTPS Nginx 설정 (`nginx-letsencrypt.conf`)
공인 도메인 환경에서는 **HSTS(Strict-Transport-Security)**를 활성화하여 전송 계층 보안을 강제합니다.

```nginx
# frontend-nginx/nginx-letsencrypt.conf
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

# 1. HTTP -> HTTPS 301 강제 리다이렉트 (ACME 챌린지 경로 유지)
server {
    listen 80;
    listen [::]:80;
    server_name account.example.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# 2. HTTPS SSL Termination & Reverse Proxy
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name account.example.com;

    # Let's Encrypt 인증서 경로 (컨테이너 내부 마운트 경로)
    ssl_certificate /etc/letsencrypt/live/account.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/account.example.com/privkey.pem;

    # 모던 SSL/TLS 보안 프로토콜 및 암호화 스위트
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;
    ssl_session_tickets off;

    # 공인 프로덕션 환경 전용 보안 헤더 (HSTS 적용)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    client_max_body_size 50M;
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;

    # 1) Next.js 정적 자원
    location /_next/static/ {
        proxy_pass http://account-frontend:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        expires 365d;
        access_log off;
    }

    # 2) Spring Cloud Gateway API
    location /api/ {
        proxy_pass http://account-gateway:8000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Prefix /api;
    }

    # 3) Grafana
    location /grafana/ {
        proxy_pass http://account-grafana:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }

    # 4) Zipkin
    location /zipkin/ {
        proxy_pass http://account-zipkin:9411/zipkin/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    # 5) Kibana
    location /kibana/ {
        proxy_pass http://account-kibana:5601;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Prefix /kibana;
    }

    # 6) pgAdmin
    location /pgadmin/ {
        proxy_pass http://account-pgadmin:80/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Script-Name /pgadmin;
        proxy_redirect off;
    }

    # 7) Frontend 메인 루트
    location / {
        proxy_pass http://account-frontend:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }
}
```

#### Step 4: 프로덕션 Compose 연동 및 기동
```yaml
# compose.prod-nginx.yml
version: '3.8'

services:
  account-frontend-nginx:
    image: nginx:1.25-alpine
    container_name: account-frontend-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./frontend-nginx/nginx-letsencrypt.conf:/etc/nginx/conf.d/default.conf:ro
      - ./certbot/conf:/etc/letsencrypt:ro
      - ./certbot/www:/var/www/certbot:ro
    networks:
      - account-network

networks:
  account-network:
    name: ${ACCOUNT_NETWORK_NAME:-account-dev-network}
    external: true
```

#### Step 5: 자동 갱신(Auto-Renewal) 라이프사이클 및 Nginx Reload
Let's Encrypt 인증서는 유효기간이 90일이며, 만료 30일 전부터 갱신이 가능합니다.

1. **갱신 모의 테스트 (Dry-Run)**:
   ```bash
   docker run --rm \
     -v $(pwd)/certbot/www:/var/www/certbot:rw \
     -v $(pwd)/certbot/conf:/etc/letsencrypt:rw \
     certbot/certbot renew --webroot -w /var/www/certbot --dry-run
   ```
2. **호스트 크론탭(Crontab) 등록 (매일 새벽 3시 실행)**:
   ```bash
   # crontab -e
   0 3 * * * docker run --rm -v /path/to/certbot/www:/var/www/certbot:rw -v /path/to/certbot/conf:/etc/letsencrypt:rw certbot/certbot renew --webroot -w /var/www/certbot --quiet && docker exec account-frontend-nginx nginx -s reload
   ```
3. **갱신 실패 시 롤백 및 복구 절차**:
   - 갱신 로그 확인: `/var/log/letsencrypt/letsencrypt.log` 검토.
   - 원인: DNS 레코드 불일치, 80 포트 방화벽 차단, Let's Encrypt ACME Rate Limit 초과.
   - 복구: 기존 인증서 백업 확인, DNS/방화벽 정상화 후 `certbot renew --force-renewal` 수동 재시도.

---

### 1.3 [방식 C] OpenSSL 자체 서명 / 로컬 개발 CA (사설 IP 및 로컬 개발용)

공인 도메인이 없는 사설 IP(`192.168.x.x`), 사설 VM, 또는 `localhost` 개발 환경에서 HTTPS를 구축하는 절차입니다.

#### ⚠️ HSTS(Strict-Transport-Security) 절대 적용 금지 안내
- **주의**: `localhost`나 자체 서명 환경에 HSTS 헤더를 적용하면, 브라우저가 해당 호스트에 대해 1년간 강제 HTTPS 및 인증서 엄격 검증을 캐싱하여 **이후 로컬 HTTP 개발 및 테스트가 영구적으로 차단되는 장애**를 유발합니다.
- 따라서 **자체 서명 템플릿에서는 HSTS 헤더를 절대 포함하지 않습니다.**

#### Step 1: SAN(Subject Alternative Name) 포함 자체 서명 인증서 생성
현대 브라우저와 HTTP 클라이언트는 `CN`(Common Name)만으로는 유효성을 인정하지 않고 반드시 `subjectAltName` 확장을 요구합니다.

```bash
# 인증서 저장 폴더 생성
mkdir -p ./frontend-nginx/certs

# OpenSSL 2048-bit RSA 키 및 365일 유효 인증서 생성 (SAN 확장 포함)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ./frontend-nginx/certs/selfsigned.key \
  -out ./frontend-nginx/certs/selfsigned.crt \
  -subj "/C=KR/ST=Seoul/L=Gangnam/O=Account-Dev/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

# 파일 권한 설정 (개인키 보안)
chmod 600 ./frontend-nginx/certs/selfsigned.key
chmod 644 ./frontend-nginx/certs/selfsigned.crt
```

#### Step 2: 자체 서명 Nginx 설정 (`nginx-selfsigned.conf`)
```nginx
# frontend-nginx/nginx-selfsigned.conf
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

# 1. HTTP -> HTTPS 301 리다이렉트
server {
    listen 80;
    listen [::]:80;
    server_name localhost;

    location / {
        return 301 https://$host$request_uri;
    }
}

# 2. HTTPS SSL Termination & Reverse Proxy
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name localhost;

    # 자체 서명 인증서 마운트 경로
    ssl_certificate /etc/nginx/certs/selfsigned.crt;
    ssl_certificate_key /etc/nginx/certs/selfsigned.key;

    # TLS 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;

    # 기본 보안 헤더 (⚠️ HSTS 제외)
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    client_max_body_size 50M;
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;

    # 1) Next.js 정적 자원
    location /_next/static/ {
        proxy_pass http://account-frontend:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        expires 365d;
        access_log off;
    }

    # 2) Spring Cloud Gateway API
    location /api/ {
        proxy_pass http://account-gateway:8000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Prefix /api;
    }

    # 3) Grafana
    location /grafana/ {
        proxy_pass http://account-grafana:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }

    # 4) Zipkin
    location /zipkin/ {
        proxy_pass http://account-zipkin:9411/zipkin/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    # 5) Kibana
    location /kibana/ {
        proxy_pass http://account-kibana:5601;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Prefix /kibana;
    }

    # 6) pgAdmin
    location /pgadmin/ {
        proxy_pass http://account-pgadmin:80/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Script-Name /pgadmin;
        proxy_redirect off;
    }

    # 7) Frontend 메인 루트
    location / {
        proxy_pass http://account-frontend:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }
}
```

#### Step 3: Compose 파일 및 볼륨 마운트
```yaml
# frontend-nginx/docker-compose.yml
version: '3.8'

services:
  account-frontend-nginx:
    image: nginx:1.25-alpine
    container_name: account-frontend-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx-selfsigned.conf:/etc/nginx/conf.d/default.conf:ro
      - ./certs/selfsigned.crt:/etc/nginx/certs/selfsigned.crt:ro
      - ./certs/selfsigned.key:/etc/nginx/certs/selfsigned.key:ro
    networks:
      - account-network

networks:
  account-network:
    name: ${ACCOUNT_NETWORK_NAME:-account-dev-network}
    external: true
```

#### Step 4: 개발 CA 및 자체 서명 인증서 신뢰(Trust Store) 등록
브라우저의 불완전한 보안 우회 플래그(`thisisunsafe` 등)를 사용하는 것은 보안상 위험하며 권장되지 않습니다. 대신 개발 머신의 OS 신뢰 저장소에 인증서를 등록하거나, `mkcert`를 사용하는 것이 정석입니다.

- **방법 1: `mkcert` 활용 (가장 권장)**:
  ```bash
  # mkcert 설치 및 로컬 CA 생성
  mkcert -install
  
  # localhost용 인증서 생성 후 frontend-nginx/certs로 복사
  mkcert -key-file ./frontend-nginx/certs/selfsigned.key -cert-file ./frontend-nginx/certs/selfsigned.crt localhost 127.0.0.1 ::1
  ```
- **방법 2: Linux 신뢰 저장소 등록 (Ubuntu/Debian)**:
  ```bash
  sudo cp ./frontend-nginx/certs/selfsigned.crt /usr/local/share/ca-certificates/account-dev.crt
  sudo update-ca-certificates
  ```
- **방법 3: macOS 키체인 등록**:
  ```bash
  sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ./frontend-nginx/certs/selfsigned.crt
  ```
- **방법 4: Windows 인증서 관리자 등록**:
  ```powershell
  Import-Certificate -FilePath .\frontend-nginx\certs\selfsigned.crt -CertStoreLocation Cert:\LocalMachine\Root
  ```
- **CLI 테스트 전용 검증**:
  - `curl -k -I https://localhost/` (CLI 빠른 테스트 전용)
  - `curl --cacert ./frontend-nginx/certs/selfsigned.crt -I https://localhost/` (인증서 체인 검증)

---

## ⚙️ 2. Docker vs Podman 실행 및 환경 차이점

이 저장소는 Docker와 Podman을 모두 지원합니다. 실행 시 아래 차이점을 고려해야 합니다.

| 항목 | Docker (`docker compose`) | Podman (`podman compose`) |
|---|---|---|
| **SELinux 레이블** | 일반 마운트 가능 | 볼륨 마운트 시 `:ro,z` 또는 `:z` 플래그 권장 |
| **비루트 포트 바인딩** | root 데몬이 80/443 바인딩 | Rootless 시 `sysctl net.ipv4.ip_unprivileged_port_start=80` 설정 필요 또는 8080/8443 매핑 |
| **네트워크 이름** | `name:` 필드 기본 지원 | `podman-compose` provider에 따라 `ACCOUNT_NETWORK_NAME` 환경변수 명시 필요 |

### 2.1 사전 검증 스크립트 (인증서 파일 존재 확인)
컨테이너를 띄우기 전 인증서 파일이 준비되었는지 검사합니다.

```bash
# 인증서 파일 존재 여부 사전 체크
test -f ./frontend-nginx/certs/selfsigned.crt || { echo "❌ CRT file not found"; exit 1; }
test -f ./frontend-nginx/certs/selfsigned.key || { echo "❌ KEY file not found"; exit 1; }
echo "✅ Certificate files verified."
```

### 2.2 컨테이너 빌드 및 기동 명령어
```bash
# Docker Compose 기동
docker compose -f frontend-nginx/docker-compose.yml up -d

# Podman Compose 기동
podman compose -f frontend-nginx/docker-compose.yml up -d
```

---

## 🧪 3. 라우팅 및 SSL 정상 동작 검증 절차

Nginx가 정상 기동되면 아래 순서로 라우팅과 SSL 핸드셰이크를 검증합니다.

```bash
# 1. HTTP 80 -> HTTPS 443 (301 Moved Permanently) 리다이렉트 검증
curl -I http://localhost/
# 기대 결과: HTTP/1.1 301 Moved Permanently, Location: https://localhost/

# 2. HTTPS 프론트엔드 루트 응답 검증 (200 OK)
curl -k -I https://localhost/
# 기대 결과: HTTP/2 200

# 3. Spring Cloud Gateway API 프록시 라우팅 검증
curl -k https://localhost/api/actuator/health
# 기대 결과: {"status":"UP"}

# 4. Grafana 대시보드 헬스체크 검증
curl -k -I https://localhost/grafana/api/health
# 기대 결과: HTTP/2 200 OK (또는 302 리다이렉트)

# 5. Zipkin UI 접속 검증
curl -k -I https://localhost/zipkin/
# 기대 결과: HTTP/2 200 OK

# 6. pgAdmin 서브패스 리버스 프록시 검증
curl -k -I https://localhost/pgadmin/
# 기대 결과: HTTP/2 200 OK (또는 로그인 페이지 302)
```

---

## ❓ 4. 실무 트러블슈팅 FAQ

### Q1. Nginx 시작 시 `host not found in upstream "account-frontend"` 오류가 발생합니다.
- **원인**: Nginx가 기동되는 시점에 업스트림 컨테이너(`account-frontend`, `account-gateway` 등)가 아직 시작되지 않았거나, 동일한 네트워크(`account-network`)에 연결되지 않아 도커 DNS 해석에 실패한 경우입니다.
- **해결책**:
  1. `docker network inspect account-dev-network`로 모든 컨테이너가 동일 네트워크에 있는지 확인합니다.
  2. Nginx 설정 상단에 동적 DNS 리졸버를 선언하고 변수 기반 `proxy_pass`를 활용할 수 있습니다:
     ```nginx
     resolver 127.0.0.11 valid=10s ipv6=off;
     ```
  3. 또는 `depends_on`을 통해 업스트림 서비스가 기동된 후 Nginx가 실행되도록 제어합니다.

### Q2. pgAdmin 접속 시 로그인 후 흰 화면이 나타나거나 리다이렉트가 깨집니다.
- **원인**: pgAdmin이 자신이 `/pgadmin` 서브패스 뒤에서 동작하고 있음을 인지하지 못해 정적 자원(CSS, JS)을 루트(`/`)에서 요청하기 때문입니다.
- **해결책**:
  1. Nginx `location /pgadmin/` 블록에 `proxy_set_header X-Script-Name /pgadmin;`과 `proxy_redirect off;`가 누락되지 않았는지 확인합니다.
  2. `proxy_pass http://account-pgadmin:80/;`의 끝에 슬래시(`/`)를 반드시 유지합니다.

### Q3. Grafana 대시보드에서 WebSocket 연결 실패 알림이 발생합니다.
- **원인**: Grafana 라이브 대시보드 업데이트는 WebSocket을 사용하며, Nginx에서 `Upgrade` 및 `Connection` 헤더를 업스트림으로 전달해야 합니다.
- **해결책**:
  - `nginx.conf` 최상단에 `map $http_upgrade $connection_upgrade`를 선언하고, `location /grafana/` 내에 `proxy_set_header Upgrade $http_upgrade;`, `proxy_set_header Connection $connection_upgrade;`를 설정합니다.

### Q4. Let's Encrypt 인증서 갱신 시 `Failed to connect to host for DVSNI challenge` 또는 `Timeout during connect` 오류가 납니다.
- **원인**: 80번 포트가 외부 방화벽/공유기 포트포워딩에서 차단되어 있거나, Nginx에서 `/.well-known/acme-challenge/` 경로가 올바른 webroot 디렉터리로 매핑되지 않았습니다.
- **해결책**:
  1. 외부에서 `http://account.example.com/.well-known/acme-challenge/test.txt` 접근이 가능한지 확인합니다.
  2. Nginx HTTP(80) 블록에 ACME 경로가 `root /var/www/certbot;`으로 정확히 연결되어 있는지 점검합니다.
