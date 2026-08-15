# 프론트엔드 실행 가이드 (로컬 · 개발 · 운영)

이 문서는 Frontend(Next.js)를 **로컬**, **개발(컨테이너)**, **운영** 세 가지 방식으로
실행하고 배포하는 방법을 초보자 기준으로 설명합니다.

백엔드 모듈 실행은 [local-development.md](./local-development.md)와
[development-compose.md](./development-compose.md)를, 이미지 빌드 규격은
[container-images.md](./container-images.md)를 따릅니다.

---

## 0. 먼저 읽기: 실행 방법이 4개인 이유

저장소에는 Frontend를 실행하는 방법이 4가지 있고, 처음 보면 헷갈리기 쉽습니다.
**목적이 다르므로 필요한 것 하나만 고르면 됩니다.**

| # | 방법 | 실행 위치 | 빌드 파일 | 포트 | 언제 쓰나 |
|---|------|-----------|-----------|------|-----------|
| 1 | `npm run dev` | `frontend/` | 없음(로컬 Node) | 3000 | **화면만 빠르게 개발** (제일 간단) |
| 2 | `frontend/compose.dev.yml` | `frontend/` | `Containerfile.dev` | 3000 | 컨테이너로 프론트만 단독 기동 |
| 3 | 루트 `docker-compose.yml` | **저장소 루트** | `Containerfile.dev` | 3000 | 백엔드 API까지 붙여 통합 확인 |
| 4 | `frontend/docker-compose.yml` | `frontend/` | `Containerfile` | **4000** | 운영 이미지 스모크 테스트 |

> **가장 흔한 실수**: 3번(루트)과 4번(frontend 폴더)을 헷갈려서 엉뚱한 디렉터리에서
> `docker compose up`을 실행하는 것입니다. 3번은 반드시 **저장소 루트**에서,
> 2번과 4번은 **`frontend/` 폴더 안**에서 실행합니다.

이 저장소는 Docker와 Podman을 모두 지원합니다. 아래 예시의 `docker compose`는
Podman 사용자라면 그대로 `podman compose`로 바꿔 쓰면 됩니다.

---

## 1. 로컬 실행 (`local`) — Node.js 직접 실행

Docker가 필요 없고 가장 빠릅니다. **화면 개발은 보통 이 방법이면 충분합니다.**

### 준비물

- Node 20 LTS (`package.json`의 `engines`가 `>=20.0.0 <21.0.0`으로 고정)
- `.nvmrc` / `.node-version`이 있으므로 `nvm use`를 실행하면 자동으로 맞춰집니다

### 실행

```powershell
cd frontend
npm ci
npm run dev
```

브라우저에서 <http://localhost:3000> 을 엽니다.

### API 연결은 어떻게 되나요?

- `.env.local`의 `NEXT_PUBLIC_API_URL=http://localhost:8000/api` → 브라우저가 Gateway를
  직접 호출합니다.
- 상대 경로 `/api/*` 요청은 `next.config.ts`의 rewrite가 `GATEWAY_INTERNAL_URL`로
  넘겨주며, 개발 모드 기본값은 `http://localhost:8000`입니다.

**Gateway가 안 떠 있어도 화면은 정상적으로 뜹니다.** API 호출만 실패하므로
레이아웃·스타일 작업은 백엔드 없이 그대로 진행할 수 있습니다.

Gateway를 다른 주소에서 돌린다면:

```powershell
$env:GATEWAY_INTERNAL_URL = "http://localhost:9000"
npm run dev
```

---

## 2. 개발 실행 (`dev`) — 컨테이너

### 2-1. 프론트엔드만 단독으로 (권장)

`frontend/compose.dev.yml`은 **Gateway·Config Server·Discovery 없이** Frontend 하나만
띄웁니다. 사전에 만들어 둬야 하는 네트워크도 없습니다.

```powershell
cd frontend
docker compose -f compose.dev.yml up -d --build
```

- 접속: <http://localhost:3000>
- 소스가 bind mount되어 있어 **파일을 저장하면 즉시 hot reload**됩니다.
- `node_modules`와 `.next`는 named volume이라, Windows 호스트의 파일이 리눅스 컨테이너
  빌드 산출물을 덮어쓰지 않습니다.

포트를 바꾸고 싶다면:

```powershell
$env:FRONTEND_DEV_PORT = "3100"
docker compose -f compose.dev.yml up -d --build
```

호스트에서 Gateway를 돌리고 있다면 컨테이너 안에서는 `localhost`가 컨테이너 자신을
가리키므로, 기본값이 `host.docker.internal`로 잡혀 있습니다. 다른 주소라면:

```powershell
$env:GATEWAY_INTERNAL_URL = "http://host.docker.internal:9000"
docker compose -f compose.dev.yml up -d --build
```

상태 확인과 종료:

```powershell
docker compose -f compose.dev.yml ps
docker compose -f compose.dev.yml logs -f
docker compose -f compose.dev.yml down
```

의존성(`package.json`)을 바꿨는데 반영이 안 되면 named volume을 지우고 다시 빌드합니다.

```powershell
docker compose -f compose.dev.yml down
docker volume rm account-frontend-dev_frontend_dev_node_modules
docker compose -f compose.dev.yml up -d --build
```

### 2-2. 백엔드까지 함께 (루트 Compose)

루트 `docker-compose.yml`의 `frontend` 서비스는 `depends_on: gateway(healthy)`가 걸려
있어 **Gateway → Config Server + Discovery**가 함께 떠야 기동됩니다. 실제 API를 붙여
확인할 때 사용합니다. 자세한 내용은 아래 3장을 보세요.

---

## 3. 루트 Compose 사용법 (초보자용)

루트 `docker-compose.yml`은 플랫폼 4개 + API 17개 + Batch 15개, 총 36개 실행 대상을
정의합니다. 한 번에 다 띄우지 않고 **profile로 필요한 만큼만** 켜는 구조입니다.

### 3-1. profile이 뭔가요?

Compose의 `--profile`은 "이번에 켤 서비스 묶음"을 고르는 스위치입니다. profile을
지정하지 않으면 해당 서비스는 아예 생성되지 않습니다.

| Profile | 켜지는 것 |
|---------|-----------|
| `self-contained` | 저장소가 직접 띄우는 PostgreSQL·Redis·Kafka와 스키마 준비 확인 |
| `external-dev` | 공유 개발 PostgreSQL 접속·권한 확인 (DB 컨테이너는 안 띄움) |
| `platform` | Gateway + Frontend |
| `apis` | API 17개 + Gateway + Frontend |
| `foundation` | Auth, Master Data, Internal Audit, Budget API |
| `accounting` | 원장·결산·채무·채권·지출결의·세무·보고 API |
| `products` | Loan, Deposit, Asset Lease API |
| `risk` | Account Mart, ECL, Reconciliation API |
| `migration` | DB migration 실행기와 권한 부여 helper |
| `batch` | Batch 15개 (자동 Job 실행은 기본 꺼짐) |

Config Server와 Discovery는 업무 profile을 고르면 함께 따라오는 선행 서비스입니다.

### 3-2. 처음 한 번만 하는 준비

```powershell
Copy-Item .env.dev.example .env.dev
```

`.env.dev`를 열어 `replace-with-...`로 되어 있는 값을 **로컬 전용 값**으로 모두 바꿉니다.
이 파일은 Git에 올라가지 않습니다.

값 형식이 맞는지 검사합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\validate-dev-env.ps1 `
  -Mode SelfContained -EnvFile .\.env.dev
```

### 3-3. Gateway + Frontend만 띄우기

백엔드 API 없이 Edge만 확인할 때 (`platform` profile):

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml `
  -f compose.self-contained.yml `
  --profile platform `
  up -d --build
```

- Frontend: <http://localhost:3000>
- Gateway: <http://localhost:8000>

### 3-4. 전체 API까지 띄우기

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml `
  -f compose.self-contained.yml `
  --profile self-contained `
  --profile apis `
  up -d --build
```

> **주의**: 새로 만든 self-contained DB는 스키마가 비어 있어서 API가 바로 뜨지 않습니다.
> 17개 context의 migration과 권한 부여를 **먼저** 끝내야 합니다. 전체 순서는
> [development-compose.md](./development-compose.md)를 따르세요.

### 3-5. Frontend만 다시 빌드/재기동

다른 서비스는 그대로 두고 Frontend만 갱신합니다.

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml `
  -f compose.self-contained.yml `
  --profile self-contained `
  --profile apis `
  up -d --build frontend
```

코드만 바꿨다면 bind mount + `next dev` 조합이라 **재빌드 없이 hot reload**로 충분합니다.

### 3-6. 종료

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml down
```

`down -v`는 쓰지 않습니다. named volume과 적용된 migration이 함께 지워집니다.

---

## 4. 운영 실행 (`prod`)

운영은 **소스 빌드를 하지 않습니다.** `compose.prod.yml`에는 `build:`가 없고, 모든
이미지는 불변(immutable) digest로만 지정합니다.

### 4-1. 운영 이미지 빌드

운영에 올릴 이미지는 릴리스 파이프라인에서 미리 만들어 registry에 올립니다.

```powershell
docker build -f frontend/Containerfile `
  --build-arg GATEWAY_INTERNAL_URL=http://gateway:8000 `
  --build-arg NEXT_PUBLIC_API_URL=/api `
  -t account/frontend:local frontend
```

> ⚠️ **가장 중요한 함정 — `GATEWAY_INTERNAL_URL`은 빌드 시점 값입니다.**
>
> `next.config.ts`의 `/api/*` rewrite는 `next build` 때 `.next/routes-manifest.json`에
> **구워집니다.** 그래서 이 값을 컨테이너 실행 시 환경변수로 넘겨도 **아무 효과가 없습니다.**
> 빠뜨리고 빌드하면 manifest에 `rewrites: []`가 박혀서, `NEXT_PUBLIC_API_URL=/api`가
> 만드는 `/api/*` 호출이 Gateway에 닿지 못하고 전부 404가 됩니다.
>
> 개발 모드(`next dev`)는 설정을 실행 시점에 읽으므로 런타임 환경변수로 동작합니다.
> **개발에서는 되는데 운영 이미지에서만 404**가 나면 대부분 이 문제입니다.

빌드된 이미지에 rewrite가 제대로 들어갔는지 확인하는 방법:

```powershell
docker run --rm --entrypoint sh account/frontend:local -c "cat .next/routes-manifest.json"
```

출력의 `rewrites`에 `/api/:path*` → Gateway 주소가 보이면 정상입니다.

저장소 빌드 도구를 쓸 때는 `deploy/image-targets.json`의 frontend `buildArgs`가
전달되며, 같은 이름의 환경변수를 두면 manifest 수정 없이 override 됩니다.

```powershell
$env:GATEWAY_INTERNAL_URL = "http://my-gateway:8000"
pwsh tools/container-images.ps1
```

### 4-2. 운영 기동

`.env.prod`에 `FRONTEND_IMAGE`를 `@sha256:...` digest로 지정해야 합니다. 태그(`:latest`)는
운영 검증에서 거부됩니다.

```powershell
docker compose --env-file .env.prod -f compose.prod.yml --profile prod up -d
```

운영 Frontend 컨테이너에 적용되는 보안 제약:

- `read_only: true` (쓰기는 `/tmp`, `/app/.next/cache` tmpfs만)
- `cap_drop: ALL`, `no-new-privileges`
- `pids_limit: 128`, CPU 0.75 / 메모리 512m 상한
- non-root `nextjs` 사용자로 실행

호스트에 열리는 포트는 Gateway와 Frontend뿐이며, 공용 인터넷에 직접 노출하지 말고
승인된 reverse proxy 뒤에 둡니다. 자세한 절차는
[production-compose.md](./production-compose.md)를 따릅니다.

### 4-3. 운영 이미지 로컬 스모크 테스트

운영 이미지가 제대로 뜨는지만 확인하려면 `frontend/docker-compose.yml`을 씁니다.

```powershell
cd frontend
docker compose up --build
```

- 접속 포트는 **4000**입니다 (`4000:3000`).
- ⚠️ 이 파일은 `account-network`를 `external: true`로 요구합니다. 즉 **루트 Compose를
  최소 한 번 먼저 올려서** 그 네트워크(`account-dev-network`)가 이미 있어야 동작합니다.
  없으면 `network account-network declared as external, but could not be found` 에러가 납니다.

네트워크만 따로 만들어도 됩니다.

```powershell
docker network create account-dev-network
```

---

## 5. 환경변수 정리

| 변수 | 적용 시점 | 기본값 | 설명 |
|------|-----------|--------|------|
| `NEXT_PUBLIC_API_URL` | **빌드** | `/api` | 브라우저가 호출할 API 주소 |
| `GATEWAY_INTERNAL_URL` | 운영은 **빌드**, 개발은 런타임 | `http://gateway:8000` | `/api/*` rewrite 대상 |
| `FRONTEND_DEV_PORT` | 런타임 | `3000` | `compose.dev.yml` 호스트 포트 |
| `DEV_FRONTEND_PORT` | 런타임 | `3000` | 루트 개발 Compose 호스트 포트 |
| `PROD_FRONTEND_PORT` | 런타임 | `3000` | 운영 Compose 호스트 포트 |
| `FRONTEND_IMAGE` | 런타임 | 없음(필수) | 운영 이미지 digest |
| `ACCOUNT_NETWORK_NAME` | 런타임 | `account-dev-network` | 개발 네트워크 이름 |

---

## 6. 자주 겪는 문제

### `npm ci`가 실패합니다 (컨테이너 빌드가 통째로 막힘)

`Containerfile`과 `Containerfile.dev`가 **둘 다** `npm ci`를 쓰기 때문에, 이게 깨지면
개발 컨테이너·운영 이미지·루트 Compose의 `frontend`가 전부 빌드되지 않습니다.
자주 나오는 두 가지 원인:

**1) `EUSAGE` — package.json과 lockfile이 어긋남**

```
npm error `npm ci` can only install packages when your package.json and
npm error package-lock.json ... are in sync.
npm error Missing: autoprefixer@10.5.4 from lock file
```

`package.json`에 의존성을 추가/변경하고 `package-lock.json`을 함께 갱신하지 않으면
발생합니다. **로컬에 `node_modules`가 이미 있으면 눈치채지 못하고 넘어가므로**, 깨끗한
컨테이너 빌드에서만 드러납니다.

lockfile을 다시 만들어 커밋하세요. 컨테이너 런타임과 같은 Node 20/npm 10으로 생성해야
결정론적입니다.

```powershell
docker run --rm -v "${PWD}:/app" -w /app node:20-alpine `
  npm install --package-lock-only --no-audit --no-fund
```

**2) `ERESOLVE` — peer dependency 충돌**

`next`가 요구하는 `react` peer 범위와 실제 `react` 버전이 맞지 않을 때 발생합니다.
(예: `next@15.0.x`의 peer는 `^18.2.0 || 19.0.0-rc-...`라서 `react@19.0.0` 정식판을
만족하지 않습니다. `next@15.0.4`부터 `^19.0.0`을 받습니다.)

`--force`나 `--legacy-peer-deps`로 우회하지 마세요. 로컬 `node_modules`는 만들어져도
컨테이너 빌드에서 다시 막히고, 원인만 가려집니다. 호환되는 버전 조합으로 올린 뒤
위 명령으로 lockfile을 재생성하는 것이 정석입니다.

> 의존성을 바꿨다면 커밋 전에 **컨테이너에서 한 번 빌드해 보세요.** 로컬에서만
> `npm run dev`가 되는 것은 검증이 아닙니다.
>
> ```powershell
> cd frontend
> docker compose -f compose.dev.yml build --no-cache
> ```

### `network account-network declared as external, but could not be found`

`frontend/docker-compose.yml`을 루트 Compose 없이 실행한 경우입니다.
위 4-3의 네트워크 생성 명령을 먼저 실행하거나, 개발 목적이라면 `compose.dev.yml`을 쓰세요.

### 화면은 뜨는데 `/api` 호출이 전부 404

운영 이미지를 `GATEWAY_INTERNAL_URL` 없이 빌드한 경우입니다. 4-1의 경고를 참고해
**다시 빌드**해야 합니다. 컨테이너 환경변수로는 고칠 수 없습니다.

### 컨테이너 안에서 호스트의 Gateway에 연결이 안 됩니다

컨테이너 안의 `localhost`는 호스트가 아니라 컨테이너 자신입니다.
`http://host.docker.internal:8000`을 사용하세요. `compose.dev.yml`은 이미 기본값으로
쓰고 있고, `extra_hosts`로 매핑도 걸어 두었습니다.

### 의존성을 추가했는데 컨테이너에 반영되지 않습니다

`node_modules`가 named volume이라 이미지를 다시 빌드해도 기존 volume이 그대로 마운트됩니다.
2-1의 volume 삭제 후 재빌드 절차를 따르세요.

### Frontend가 계속 기동 대기 상태입니다 (루트 Compose)

루트 Compose의 `frontend`는 Gateway가 healthy가 될 때까지 기다립니다. Gateway는 다시
Config Server와 Discovery에 의존합니다. 프론트만 필요하다면 `compose.dev.yml`을 쓰세요.

---

## 7. 관련 문서

- [local-development.md](./local-development.md) — 백엔드 `local` 프로파일(H2) 실행
- [development-compose.md](./development-compose.md) — 개발 Compose 전체 절차와 migration 순서
- [production-compose.md](./production-compose.md) — 운영 Compose와 환경변수 검증
- [container-images.md](./container-images.md) — 이미지 빌드 계약
- [frontend/README.md](../../frontend/README.md) — 프론트엔드 모듈 개요
