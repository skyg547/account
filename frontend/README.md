# 🎨 Account.AI Frontend (Modern Financial System UI)

본 모듈은 MSA 기반 재무 시스템의 사용자 인터페이스를 담당하는 **Next.js 15** 애플리케이션입니다. 
초보자분들도 쉽게 적응할 수 있도록 설계되었으니, 아래 가이드를 천천히 따라와 주세요!

---

## 📚 초보자를 위한 학습 & 개발 가이드
처음 오셨다면 아래 문서들을 순서대로 읽어보시는 것을 강력 추천합니다.

0.  [**🚀 실행 가이드 (로컬·개발·운영)**](../docs/guides/frontend-runtime-guide.md): 실행 방법 4가지의 차이, 프로파일별 기동/배포 절차, 자주 겪는 문제를 한곳에 정리했습니다. **실행이 막히면 여기부터 보세요.**
1.  [**🎓 프론트엔드 핵심 개념 & 교육 가이드**](./docs/frontend-core-education-guide.md): 훅(Hook)의 원리, Context API vs Fetch/Axios, 상태 저장소 3단계 및 Next.js 15 아키텍처를 총정리한 마스터 교육서입니다.
2.  [**📐 종합 화면 레이아웃 & 화면 리스트 설계서**](./docs/ui-layout-and-screen-specification.md): 전체 4단 레이아웃 구조와 백엔드 16대 모듈 직결 122개 화면 상세 명세서입니다.
3.  [**🐣 프론트엔드 입문 가이드**](./docs/beginner-guide.md): KBank 핀테크 디자인, 폴더 구조와 기초 개념을 설명합니다.
4.  [**🖥️ 화면 목록 및 기능 명세 (인벤토리)**](./docs/screen-inventory.md): 현재 구축된 모든 화면과 주요 기능을 한눈에 확인합니다.
5.  [**🛠️ 실전 개발 가이드**](./docs/development-guide.md): Next.js 15와 Tailwind v4로 새 페이지를 만들고 디자인을 입히는 법을 알려줍니다.
6.  [**🚀 빌드 & 배포 완전 정복 가이드**](./docs/build-deploy-guide.md): 로컬 실행, 프로덕션 정적 빌드, Docker 컨테이너 배포 및 트러블슈팅을 다룹니다.
7.  [**🚒 런북 (장애 해결)**](./docs/runbook.md): 에러가 났을 때 당황하지 않고 대처하는 법을 모았습니다.
8.  [**🔐 HttpOnly BFF 인증 세션 가이드**](./docs/auth-session-guide.md): 로그인, 쿠키, 로그아웃, 보안 검증과 롤백을 설명합니다.

---

## 🚀 빠른 시작 (Running Locally)

### 📌 Node.js 런타임 규격 (Node Runtime Pinning)
* **권장 Node 버전:** Node 20 LTS (`v20.18.0`) / NPM 10+ (`>=10.0.0`)
* **로컬 버전 자동 고정:** `.nvmrc` 및 `.node-version`이 포함되어 있어 `nvm use` 실행 시 자동으로 지정된 Node 20 LTS 런타임을 사용합니다.
* **환경 간 정합성 (Runtime Parity):** `package.json`의 `engines` 규격 (`>=20.0.0 <21.0.0`) 및 Dockerfile/Containerfile (`node:20-alpine`)과 100% 일치시켜 로컬 개발, 컨테이너, CI/CD 간 런타임 불일치 및 Lockfile v3 변형을 예방합니다.

### 💻 내 컴퓨터에서 바로 실행하기
```bash
# 1. 프론트엔드 폴더로 이동
cd frontend

# 2. package-lock.json 그대로 의존성 설치
npm ci

# 3. 개발 서버 실행
npm run dev -- --hostname 0.0.0.0
```
이제 브라우저에서 [http://localhost:3000](http://localhost:3000)을 열어보세요!

상대 경로 `/api` 요청(로그인 포함)은 Next.js BFF가 `GATEWAY_INTERNAL_URL`로 전달하며,
로컬 npm 실행 시 기본 Gateway 주소는 `http://localhost:8000`입니다. Gateway를 다른 주소에서
실행할 때만 환경 변수를 지정하세요. 로그인은 항상 same-origin `/api/auth/login`을 사용하므로
Gateway와 Auth가 모두 실행 중이어야 하며, Auth에 미리 발급된 계정의 아이디와 비밀번호가
필요합니다. Frontend와 Gateway에는 별도 `BFF_GATEWAY_SHARED_SECRET`을 같은 값으로 주입해야
하며 JWT 키나 내부 API token을 재사용하지 않습니다. 운영자가 발급한 계정만 사용합니다. 기존 PAT/Governance 화면은 아직
Gateway 전용 route와 권한 경계가 완성되지 않아 직접 override 계약을 유지합니다.

### 🧩 컨테이너로 프론트엔드만 단독 기동하기 (`compose.dev.yml`)

Gateway·Config Server·Discovery 없이 Frontend 하나만 개발 모드로 띄웁니다. 사전에 만들어
둬야 하는 네트워크도 없습니다.

```powershell
cd frontend
docker compose -f compose.dev.yml up -d --build
```

접속은 [http://localhost:3000](http://localhost:3000). 소스가 bind mount되어 있어 저장 즉시
hot reload되고, `node_modules`/`.next`는 named volume이라 Windows 호스트 파일이 리눅스
컨테이너 산출물을 덮어쓰지 않습니다. 포트는 `FRONTEND_DEV_PORT`, Gateway 주소는
`GATEWAY_INTERNAL_URL`(기본 `http://host.docker.internal:8000`)로 바꿉니다.

백엔드를 **컨테이너**로 띄워 붙이려면 overlay를 함께 얹습니다. 루트 Compose가 Gateway를
`127.0.0.1`에만 publish해서 `host.docker.internal`로는 닿지 않기 때문입니다.

```powershell
docker compose -f compose.dev.yml -f compose.dev.backend.yml up -d --build
```

이때 루트 Compose는 `platform`이 아니라 `apis`(또는 `foundation`) profile로 떠 있어야
합니다. `auth-api`가 없으면 로그인이 되지 않습니다. 자세한 내용은
[실행 가이드](../docs/guides/frontend-runtime-guide.md)를 보세요.

### 🔁 `dev` 프로파일 Compose에서 Frontend만 재빌드하기

루트 self-contained Compose가 이미 떠 있는 상태에서 다른 서비스는 그대로 두고 `frontend`
서비스만 재빌드/재기동하려면:

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml `
  -f compose.self-contained.yml `
  --profile self-contained `
  --profile apis `
  up -d --build frontend
```

`frontend` 서비스는 소스 디렉터리를 bind mount하고 `next dev`로 뜨기 때문에, 코드만 바꿨다면
재빌드 없이 hot reload로 충분합니다. `package.json`/`package-lock.json`을 바꿔 의존성이
변경된 경우에는 `node_modules`가 named volume(`frontend_node_modules`)이라 이미지 재빌드만으로는
새 패키지가 반영되지 않을 수 있으니, 재빌드 전에 해당 volume을 먼저 제거하세요.

### 🐳 루트 Compose로 통합 실행하기 (Docker/Podman 필요)

루트 Compose는 Config Server, Discovery, Gateway, Frontend와 API 서비스를 함께 실행합니다.
처음 실행할 때 예제 환경 파일을 복사하고 모든 `replace-with-...` 값을 로컬 전용 값으로 바꾸세요.

```powershell
Copy-Item .env.dev.example .env.dev
docker compose --env-file .env.dev `
  -f docker-compose.yml `
  -f compose.self-contained.yml `
  --profile self-contained `
  --profile apis `
  up --build
```

명령은 저장소 루트에서 실행합니다. self-contained 모드는 로컬 PostgreSQL, Redis, Kafka를 함께
기동하며 Frontend는 [http://localhost:3000](http://localhost:3000), Gateway는
[http://localhost:8000](http://localhost:8000)에 바인딩됩니다. 개발 Frontend 컨테이너는
Node.js 20과 `npm ci` 의존성 레이어를 사용하고 non-root 사용자로 Next 개발 서버를 실행합니다.
production 이미지는 별도 `frontend/Containerfile` 계약을 그대로 사용합니다.

`/api/*`는 build-time rewrite가 아니라 Route Handler BFF가 처리합니다. 따라서
`GATEWAY_INTERNAL_URL`은 **컨테이너 런타임**에 주입하고 브라우저 공개 변수로 만들지 않습니다.
Gateway가 다른 주소여도 Frontend image를 다시 빌드할 필요가 없습니다.
서명된 로그인 rate key용 `BFF_GATEWAY_SHARED_SECRET`도 런타임 서버 전용이며 두 서비스에
동일한 32~512-byte 값을 주입합니다.

```powershell
podman build -f frontend/Containerfile -t account/frontend:local frontend
podman run --rm -p 3000:3000 `
  -e GATEWAY_INTERNAL_URL=http://my-gateway:8000 `
  -e BFF_GATEWAY_SHARED_SECRET=$env:BFF_GATEWAY_SHARED_SECRET account/frontend:local
```

저장소 빌드 도구(`tools/container-images.ps1`)는 Gateway 주소 없이 동일한 Frontend image를
만듭니다. 주소 변경은 실행 Compose나 orchestrator의 런타임 환경에서만 합니다.

```powershell
pwsh tools/container-images.ps1
```

운영 BFF는 `GATEWAY_INTERNAL_URL`이 비어 있거나 URL 형식이 안전하지 않으면 요청을 503으로
fail-closed 합니다. 저장소 Compose는 `http://gateway:8000`을 런타임에 제공합니다.

새 self-contained DB에서는 API보다 먼저 17개 migration과 runtime grant를 완료해야 합니다.
전체 순서는 [`docs/guides/development-compose.md`](../docs/guides/development-compose.md)를 따릅니다.

### 📦 운영 이미지만 단독 검증하기 (`frontend/docker-compose.yml`)

이 폴더의 `docker-compose.yml`은 위 루트 통합 Compose와 별개로, `frontend/Containerfile`
production standalone 이미지만 실행합니다 (`npm run dev`가 아니라 `next start` 기반 이미지,
호스트 포트 `4000` → 컨테이너 `3000`). `external: true`로 선언된 `account-network`를 그대로
사용하므로, **루트 Compose를 최소 한 번 먼저 올려 그 네트워크(`account-dev-network`, 루트
`docker-compose.yml`에서 생성)가 존재해야만 동작합니다.**

```powershell
cd frontend
docker compose up --build
```

자세한 이미지 계약은 [`docs/guides/container-images.md`](../docs/guides/container-images.md)를
참고하세요.

---

## 🏗️ 우리의 기술 약속 (Tech Stack)

*   **뼈대:** Next.js 15 (최신 App Router 방식)
*   **언어:** TypeScript (실수를 줄여주는 꼼꼼한 조수)
*   **디자인:** Vanilla CSS + CSS Modules (우리만의 독창적인 스타일)
*   **테마:** **글래스모피즘(Glassmorphism)** - 유리처럼 비치는 고급스러운 디자인

---

## 📂 폴더 구조 퀵뷰
*   `src/app`: 웹사이트의 주소와 페이지 내용 (`Page`, `Layout`)
*   `src/components`: 반복해서 재사용할 수 있는 예쁜 부품들
*   `src/styles`: 전역 디자인 규칙 (`globals.css`)
*   `docs`: 여러분을 위한 상세 가이드 문서함

## 🔗 거래처 등록·승인 API 연동

- `/master-data/partner`는 거래처 목록과 `BUSINESS_PARTNER` 변경 요청의 승인 대기열을 실제 Master Data API에서 조회합니다.
- 신규 거래처는 직접 저장 API를 호출하지 않고 `POST /api/master-data/change-requests`로 `REQUESTED` 요청을 만듭니다.
- 승인과 반려는 각각 `POST /api/master-data/change-requests/{id}/approve`, `POST /api/master-data/change-requests/{id}/reject`를 사용합니다.
- 브라우저는 same-origin `/api`만 호출하고 Gateway 주소와 JWT를 알지 못합니다.
- BFF가 HttpOnly 세션 쿠키를 읽어 Gateway Bearer 헤더를 서버에서 만들며 브라우저가 보낸 사용자 ID/역할/Authorization 헤더는 전달하지 않습니다.
- Backend actor와 권한은 Gateway가 검증한 JWT 정보에서 결정됩니다. 자세한 실행·검증은 [HttpOnly BFF 인증 세션 가이드](./docs/auth-session-guide.md)를 따릅니다.
- API 오류는 빈 목록이나 Mock 데이터로 숨기지 않고 화면에 표시합니다.

---
**담당자: [프론트]**
*마지막 수정: 2026-04-22*
