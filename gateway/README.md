# 🚪 API Gateway (스프링 클라우드 게이트웨이)

`gateway` 모듈은 호텔의 "1층 안내데스크" 역할을 하는 외부 클라이언트(프론트엔드 등)의 단일 진입점입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**Q. 프론트엔드가 각 모듈을 직접 호출하면 안 되나요?**
백엔드 모듈이 수십 개로 쪼개져 있으면, 프론트엔드 개발자는 `master-data`(8089), `journal-ledger`(8081) 등 모든 포트 번호와 IP를 외우고 있어야 합니다.
이때 **API Gateway(8000 포트)** 하나만 열어두면 프론트엔드는 게이트웨이만 바라보게 됩니다. 게이트웨이는 요청의 URL(`/api/master-data/...`)을 보고 유레카(사내 전화번호부)에 물어봐서 적절한 서버로 요청을 토스(Routing)해 줍니다.

**게이트웨이가 해주는 든든한 역할들:**
- **주소 은닉 및 자동 라우팅:** 앞서 말한 단일 진입점 역할.
- **공통 보안 (JWT 1차 검문):** 모든 서비스가 로그인 검사를 할 필요 없이, 안내데스크에서 먼저 출입증(JWT)을 확인합니다.
- **서킷 브레이커 (두꺼비집):** 뒷단 서버 하나가 죽었을 때 게이트웨이가 즉시 차단막을 내려 시스템 전체가 뻗는 연쇄 붕괴를 막습니다 (Resilience4j 적용).

---

## 2. 🔄 JWT 검증 및 보안 설정

`gateway`와 `auth` 모듈은 서로 같은 JWT 서명키를 알고 있어야 합니다. 게이트웨이에서 먼저 토큰의 위변조를 검사합니다.

```yaml
auth:
  jwt:
    secret: ${AUTH_JWT_SECRET:modern-account-system-super-secret-key-1234567890}
    issuer: ${AUTH_JWT_ISSUER:auth-service}
```
*엔터프라이즈 환경에서는 `docker-compose.yml` 또는 Vault를 통해 환경변수로 운영값을 주입받습니다.*

---

## 3. 🐳 실행 방법 (Docker & Local)

**실행 순서:** Eureka(`discovery`)와 Config(`config-server`)가 켜진 후에 실행되어야 합니다.

**최신 엔터프라이즈 Docker 환경 (권장):**
루트 디렉토리의 통합 `docker-compose.yml`을 통해 헬스체크 및 의존성이 보장된 상태로 실행됩니다.
```bash
docker-compose up -d gateway
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :gateway:bootRun
```
