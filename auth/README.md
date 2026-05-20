# 🔑 Auth Service (인증 및 권한 모듈)

`auth` 모듈은 사용자의 로그인을 처리하고, 다른 모든 서비스에서 통용되는 '출입증(JWT 토큰)'을 발급하는 센터입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

MSA 시스템에서는 서버가 10개로 쪼개져 있습니다. 사용자가 `master-data`에 접근할 때 로그인하고, `journal-ledger`에 접근할 때 또 로그인하게 할 수는 없습니다.

그래서 사용자는 **딱 한 번 `auth` 모듈에 로그인**합니다.
성공하면 `auth` 모듈은 위조가 불가능한 **JWT(JSON Web Token)**라는 전자 출입증을 만들어 줍니다. 사용자는 이후 모든 요청마다 이 출입증을 보여주고, 다른 서버들은 "아, `auth` 부서에서 도장 찍어준 출입증이구나!" 하고 믿고 통과시켜 줍니다.

---

## 2. 🔄 처리 흐름 및 모듈 경계 (Process Flow)

### 📌 로그인 API
- `POST /api/auth/login`
- 사용자 이름(`username`)과 비밀번호를 받아, 맞으면 JWT 토큰(`roles`, `departmentCode`, `roleVersion`, 승인된 역할 할당 정보)을 반환합니다.
- 역할은 단순 문자열이 아니라 `RoleAssignment` 값 객체로 관리하며, 승인 여부와 유효기간을 통과한 역할만 토큰과 응답에 포함합니다.

### 📌 헥사고날 아키텍처 (DDD)
- **Controller:** 로그인 요청 수신
- **UseCase -> Service:** 로그인 흐름 제어
- **Infrastructure:** 설정 기반의 사용자 조회, 비밀번호 검증(Bcrypt 등), JWT 발급 어댑터 구현

### 🚨 모듈 경계 (중요!)
- 사용자 식별과 권한 부여는 `auth`가 담당합니다.
- 하지만 **부서 정보(조직 구조)**는 `auth`가 소유하지 않습니다. 부서는 `master-data` 모듈의 소유입니다.
- `auth`는 사용자의 `departmentCode`만 글자(참조값)로 보관하며, 상세 정보가 필요할 때는 `master-data`(`GET /api/basic/departments/{departmentCode}`)를 호출하여 확인합니다.

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
이 모듈은 멀티스테이지 Dockerfile을 통해 빌드되며, 통합 환경에서 Eureka/Config 의존성을 물고 자동으로 구동됩니다.
```bash
docker-compose up -d auth
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :auth:bootRun
```
