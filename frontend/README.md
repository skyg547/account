# 🎨 Account.AI Frontend (Modern Financial System UI)

본 모듈은 MSA 기반 재무 시스템의 사용자 인터페이스를 담당하는 **Next.js 15** 애플리케이션입니다. 
초보자분들도 쉽게 적응할 수 있도록 설계되었으니, 아래 가이드를 천천히 따라와 주세요!

---

## 📚 초보자를 위한 학습 & 개발 가이드
처음 오셨다면 아래 문서들을 순서대로 읽어보시는 것을 강력 추천합니다.

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

이 값 없이 빌드하면 manifest에 `rewrites: []`가 박혀, `NEXT_PUBLIC_API_URL=/api`가 만드는
`/api/*` 호출이 Gateway에 닿지 못하고 404가 됩니다. 런타임 환경변수로는 고칠 수 없습니다.

새 self-contained DB에서는 API보다 먼저 17개 migration과 runtime grant를 완료해야 합니다.
전체 순서는 [`docs/development-compose.md`](../docs/development-compose.md)를 따릅니다.

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
