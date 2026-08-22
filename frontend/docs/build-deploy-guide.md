# 🚀 프론트엔드 빌드 & 배포 완전 정복 가이드 (초보자용)

본 문서는 로컬 개발 환경 실행부터 컨테이너 기동, 프로덕션 정적 빌드, CI/CD 배포까지 초보자도 10분 안에 따라 할 수 있도록 정리한 실전 가이드입니다.

---

## 1. 💻 1단계: 내 컴퓨터에서 로컬 실행하기 (Local Development)

### 사전 준비물
* **Node.js**: Node 20 LTS (`v20.18.0` 이상 권장, `node -v`로 확인)
* **NPM**: NPM 10 이상 (`npm -v`로 확인)

### 실행 순서
```bash
# 1. frontend 디렉터리로 이동
cd frontend

# 2. lockfile 기준 정확한 의존성 설치 (npm install 대신 npm ci 권장)
npm ci

# 3. 개발 서버 시작 (Next.js 핫 리로딩 지원)
npm run dev
```

* **접속 주소**: [http://localhost:3000](http://localhost:3000)
* **로그인 준비**: Gateway와 Auth를 먼저 실행하고, 운영자가 Auth에 미리 발급한 계정의 아이디와 비밀번호를 준비합니다.
* 로그인은 Mock Fallback 없이 same-origin `/api/auth/login`을 통해 실제 Gateway와 Auth를 호출합니다. 다른 업무 화면의 Mock 데이터는 백엔드가 꺼져 있어도 화면 개발에 사용할 수 있습니다.

---

## 2. 🔨 2단계: 프로덕션 빌드 검증 (Build & Type Check)

배포 전, TypeScript 문법 오류나 Next.js 정적 생성에 결함이 없는지 검증합니다.

```bash
# 122개 정적 라우트 전체 빌드
npm run build
```

* **빌드 결과 확인**:
  ```
  ✓ Compiled successfully
  ✓ Generating static pages (122/122)
  ○  (Static)  prerendered as static content
  ```
* 빌드가 성공하면 `.next` 디렉터리에 최적화된 프로덕션 산출물이 생성됩니다.
* 빌드된 프로덕션 서버 실행: `npm run start`

---

## 3. 🐳 3단계: Docker 컨테이너로 실행하기 (Container Deployment)

### 방법 A. 프론트엔드만 단독 컨테이너로 기동 (`compose.dev.yml`)
백엔드나 DB 없이 프론트엔드 컨테이너만 독립 실행하고 싶을 때 사용합니다.

```powershell
cd frontend
docker compose -f compose.dev.yml up -d --build
```
* 소스코드가 컨테이너 내부로 실시간 마운트(Bind Mount)되어 있어 코드를 수정하면 즉시 브라우저에 반영됩니다.
* 컨테이너 중지: `docker compose -f compose.dev.yml down`

---

### 방법 B. 백엔드 MSA 전체와 함께 실행 (루트 `docker-compose.yml`)
Gateway, Auth-API, Core, Batch, PostgreSQL과 프론트엔드를 전체 통합 기동할 때 사용합니다.

```powershell
# 프로젝트 루트(c:\dev\account)에서 실행
Copy-Item .env.dev.example .env.dev
docker compose --env-file .env.dev -f docker-compose.yml -f compose.self-contained.yml --profile apis up -d --build
```

---

## 4. 🚒 4단계: 자주 발생하는 문제 & 초간단 해결법 (Troubleshooting)

### Q1. 브라우저에서 `GET /_next/static/css/... 404`가 발생하면서 스타일이 깨져요!
* **원인**: `npm run build`를 실행한 직후 기존에 켜져 있던 `npm run dev` 서버가 이전 메모리 캐시를 참조할 때 발생합니다.
* **해결법**:
  ```powershell
  # 1. frontend 디렉터리에서
  Remove-Item -Recurse -Force .next
  # 2. dev 서버 재시작
  npm run dev
  ```

### Q2. 3000번 포트가 이미 사용 중이라고 나와요!
* **해결법**:
  ```powershell
  # 포트 3000을 점유 중인 프로세스 조회 및 종료
  Get-Process node | Stop-Process -Force
  npm run dev
  ```
