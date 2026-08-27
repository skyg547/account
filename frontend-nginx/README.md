# 🌐 Frontend Nginx (단일 진입점 리버스 프록시)

`frontend-nginx`는 통합 개발 서버 및 운영 환경에서 프론트엔드, API 게이트웨이, 모니터링/관리자 웹 UI의 단일 진입점(Single Entry Point) 역할을 수행하는 Nginx 리버스 프록시 모듈입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

마이크로서비스 아키텍처(MSA)에서는 웹 화면(3000), 백엔드 API Gateway(8000), 서버 대시보드 Grafana(3001), 분산 추적 Zipkin(9411), 로그 검색 Kibana(5601), DB 관리 도구 pgAdmin(5050) 등 수많은 서버 프로세스가 각자의 포트에서 동작합니다.

만약 이 모든 포트를 사용자나 외부에 직접 개방한다면:
1. 사용자가 접속할 때마다 포트 번호(3000, 8000, 3001 등)를 외워야 합니다.
2. 외부 방화벽에 수많은 포트를 열어야 하므로 보안 공격 표면(Attack Surface)이 넓어집니다.
3. 브라우저의 동일 출처 정책(SOP/CORS)으로 인해 프론트엔드와 백엔드 간 통신 설정이 복잡해집니다.

`frontend-nginx`는 표준 웹 포트(80 / 443) 단 하나만을 외부로 노출하고, URL 서브패스(Sub-path)에 따라 요청을 내부 컨테이너로 안전하게 중계(Reverse Proxy)합니다.

---

## 2. 🗺️ 라우팅 명세 (Routing Matrix)

| URL 경로 (Sub-path) | 프록시 대상 서비스 (Target) | 설명 |
| :--- | :--- | :--- |
| `/` | `frontend:3000` | Next.js 웹 프론트엔드 애플리케이션 (SSR & Dynamic Routes) |
| `/_next/static/` | `frontend:3000` | Next.js 빌드 정적 에셋 (브라우저 캐싱 최적화) |
| `/api/` | `gateway:8000` | Spring Cloud Gateway MSA 백엔드 통합 API 라우터 |
| `/grafana/` | `grafana:3000` | 서버 메트릭 & 프로메테우스 시각화 대시보드 (WebSocket 지원) |
| `/zipkin/` | `zipkin:9411/zipkin/` | 마이크로서비스 간 분산 트레이싱 추적 UI |
| `/kibana/` | `kibana:5601` | Logstash/Elasticsearch 통합 로그 분석 콘솔 |
| `/pgadmin/` | `pgadmin:80` | PostgreSQL 데이터베이스 웹 관리자 콘솔 (`X-Script-Name` 지원) |

---

## 3. 🛡️ 보안 및 네트워크 최적화 (Security & Performance)

- **Zero Direct Port Exposure**: 외부에는 Nginx 포트(80)만 노출되며, 내부 MSA 컨테이너들의 포트는 도커 브릿지 네트워크(`account-network`) 내부에서만 통신합니다.
- **표준 보안 헤더 탑재**:
  - `X-Frame-Options: SAMEORIGIN` (Clickjacking 방지)
  - `X-XSS-Protection: 1; mode=block` (XSS 필터링 활성화)
  - `X-Content-Type-Options: nosniff` (MIME 스니핑 방지)
  - `Referrer-Policy: strict-origin-when-cross-origin` (리퍼러 정보 보호)
- **실시간 통신 지원 (WebSocket)**: Next.js HMR 및 Grafana Live 대시보드를 위한 `$http_upgrade` 및 Connection 헤더 자동 업그레이드 지원.

---

## 4. 🧭 실행 및 검증 (Execution Guide)

### Docker Compose 기반 실행
```bash
# 공통 네트워크가 구성된 상태에서 frontend-nginx 실행
docker-compose -f frontend-nginx/docker-compose.yml up -d
```

### 서브패스별 헬스체크 및 접근 확인
- 웹 프론트엔드: `http://localhost/`
- 백엔드 API Gateway: `http://localhost/api/actuator/health`
- Grafana 대시보드: `http://localhost/grafana/`
- Zipkin 분산 트레이싱: `http://localhost/zipkin/`
- Kibana 로그 콘솔: `http://localhost/kibana/`
- pgAdmin DB 관리자: `http://localhost/pgadmin/`

---

## 5. 📚 상세 문서 목록

1. [docs/routing-architecture.md](./docs/routing-architecture.md) - Nginx 리버스 프록시 세부 헤더 및 라우팅 아키텍처
2. [docs/local-run.md](./docs/local-run.md) - 로컬/개발 환경 실행 및 트러블슈팅 가이드