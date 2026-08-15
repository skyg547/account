# 🎨 Account.AI Frontend (Modern Financial System UI)

본 모듈은 MSA 기반 재무 시스템의 사용자 인터페이스를 담당하는 **Next.js 15** 애플리케이션입니다. 
초보자분들도 쉽게 적응할 수 있도록 설계되었으니, 아래 가이드를 천천히 따라와 주세요!

---

## 📚 초보자를 위한 학습 & 개발 가이드
처음 오셨다면 아래 문서들을 순서대로 읽어보시는 것을 강력 추천합니다.

0.  [**🚀 실행 가이드 (로컬·개발·운영)**](../docs/guides/frontend-runtime-guide.md): 실행 방법 4가지의 차이, 프로파일별 기동/배포 절차, 자주 겪는 문제를 한곳에 정리했습니다. **실행이 막히면 여기부터 보세요.**
1.  [**🐣 프론트엔드 입문 가이드**](./docs/beginner-guide.md): 우리 프로젝트의 구조와 기초 개념을 설명합니다.
2.  [**🖥️ 화면 목록 및 기능 명세**](./docs/screen-inventory.md): 현재 구축된 모든 화면과 주요 기능을 한눈에 확인합니다.
3.  [**🛠️ 실전 개발 가이드**](./docs/development-guide.md): 페이지를 만들고 디자인을 입히는 구체적인 방법을 알려줍니다.
4.  [**🚒 런북 (장해 해결)**](./docs/runbook.md): 에러가 났을 때 당황하지 않고 대처하는 법을 모았습니다.

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

상대 경로 `/api` 요청(로그인 포함)은 Next.js 개발 서버가 `GATEWAY_INTERNAL_URL`로 전달하며,
로컬 npm 실행 시 기본 Gateway 주소는 `http://localhost:8000`입니다. Gateway를 다른 주소에서
실행할 때만 환경 변수를 지정하세요. 기존 PAT/Governance 화면은 아직 Gateway 전용 route와
권한 경계가 완성되지 않았으므로 기존 `NEXT_PUBLIC_AUTH_API_URL`,
`NEXT_PUBLIC_GOVERNANCE_API_URL` 직접 override 계약을 유지합니다.

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

`/api/*` rewrite는 **빌드 시점에** `.next/routes-manifest.json`에 고정됩니다. 그래서
`GATEWAY_INTERNAL_URL`은 컨테이너 런타임이 아니라 **이미지 빌드 시점에** 있어야 하며,
`frontend/Containerfile`이 `ARG GATEWAY_INTERNAL_URL=http://gateway:8000`으로 기본값을
제공합니다. Gateway가 다른 주소에 있으면 빌드할 때 override 하세요.

```powershell
podman build -f frontend/Containerfile `
  --build-arg GATEWAY_INTERNAL_URL=http://my-gateway:8000 frontend
```

저장소 빌드 도구(`tools/container-images.ps1`)를 쓸 때는 `deploy/image-targets.json`의
frontend `buildArgs`가 전달됩니다. 동일한 이름의 환경 변수를 두면 manifest를 수정하지 않고
override 됩니다.

```powershell
$env:GATEWAY_INTERNAL_URL = "http://my-gateway:8000"
pwsh tools/container-images.ps1
```

이 값 없이 빌드하면 manifest에 `rewrites: []`가 박혀, `NEXT_PUBLIC_API_URL=/api`가 만드는
`/api/*` 호출이 Gateway에 닿지 못하고 404가 됩니다. 런타임 환경변수로는 고칠 수 없습니다.

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
- API 주소는 build-time `NEXT_PUBLIC_API_URL`을 사용합니다. `.env.local`은 Gateway `http://localhost:8000/api`, production 기본은 ingress/reverse proxy를 통과하는 same-origin `/api`입니다.
- Docker build에서는 `NEXT_PUBLIC_API_URL` build argument를 주입해야 하며, 저장소 Compose는 로컬 Gateway 8000을 기본값으로 전달합니다.
- 브라우저가 임의의 사용자 ID/역할 헤더를 만들지 않고 로그인 응답의 Bearer 토큰만 전달합니다. Backend actor와 권한은 Gateway의 검증된 JWT 정보에서 결정됩니다.
- API 오류는 빈 목록이나 Mock 데이터로 숨기지 않고 화면에 표시합니다.

---
**담당자: [프론트]**
*마지막 수정: 2026-04-22*
