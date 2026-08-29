# Development Compose Runbook

루트 개발 Compose는 로컬 Gradle/JAR 실행을 대체하지 않습니다. `local`은 각 모듈의 H2
프로파일을 사용하고, 이 문서의 컨테이너 환경은 `dev` 프로파일과 PostgreSQL을 사용합니다.
운영은 별도 [`compose.prod.yml`](../compose.prod.yml)과 불변 digest image 계약을 따릅니다.

## 컨테이너 실행 경로는 두 가지다

이 저장소에는 성격이 다른 Compose 경로가 둘 있고, **프로파일이 서로 다르다.**
둘을 섞지 않는다.

| 경로 | 파일 | 프로파일 | 용도 |
| --- | --- | --- | --- |
| 통합 | 루트 [`docker-compose.yml`](../../docker-compose.yml) + DB mode overlay | `dev` | 36개 target 전체. `.env.dev`와 profile 선택이 필수이며 이 문서의 나머지 절이 다룬다 |
| 모듈별 | `<module>/docker-compose.yml` | **`docker`** | 이미 떠 있는 `account-network`에 서비스를 하나씩 붙일 때. 개발 중 흔히 쓰는 경로다 |
| 저자원 인증 | [`tools/compose.minimal-auth-external-dev.yml`](../../tools/compose.minimal-auth-external-dev.yml) | Compose=`external-dev`, Spring=`native`/`docker` | 외부 Auth/Master Data DB를 사용하는 Frontend 로그인 경로만 순차 빌드·기동한다 |

모듈별 경로의 정본 프로파일은 `docker`다. `config-server`만 예외로 `native`를 쓰는데,
이는 `native`가 파일시스템에서 설정을 읽는 모드를 켜는 스위치이기 때문이다
(`CONFIG_REPO_LOCATION=file:/config-repo`). `dev`로 바꾸면 config-repo 서빙 자체가 깨진다.

| 서비스 | 프로파일 |
| --- | --- |
| `config-server` | `native` (예외) |
| `discovery`, `auth`, `master-data`, `gateway` | `docker` |

프로파일에 대응하는 설정 파일이 없으면 그 프로파일은 **빈 라벨**이 되어 아무 설정도
적용되지 않는다. Config Server는 `{application}-{profile}.yml`을 `{application}.yml`보다
우선 적용하므로, 컨테이너 전용 오버라이드는
[`config-repo/master-data-docker.yml`](../../config-repo/master-data-docker.yml)처럼
`-docker` 접미사 파일에 둔다. 이 함정으로 master-data가 PostgreSQL 대신 H2로 동작한
사례는 #480을 참고한다.

## 실행 인벤토리

[`deploy/image-targets.json`](../deploy/image-targets.json)이 실행 대상의 단일 진실 원천입니다.
루트 [`docker-compose.yml`](../docker-compose.yml)은 다음 36개 target을 모두 정의합니다.

- 플랫폼: Config Server, Discovery, Gateway, Frontend
- API: 17개
- Batch: 15개
- 제외: 실제 Job/Step이 없는 Auth Batch와 Internal Audit Batch, 모든 Core/Library/Aggregator

Java 서비스는 저장소 루트를 build context로 유지하면서 각 실행 모듈의 개별 `Dockerfile`을 선택해
독립적으로 컨테이너화합니다. Frontend 개발 컨테이너는 `frontend/Containerfile.dev`, 운영 image는
`frontend/Containerfile`을 사용합니다.

## 프로파일

| Profile | 실행 대상 |
| --- | --- |
| `self-contained` | 저장소 소유 PostgreSQL, Redis, Kafka와 schema readiness gate |
| `external-dev` | 공유 개발 PostgreSQL 17개 context의 인증/schema/최소권한 probe |
| `foundation` | Auth, Master Data, Internal Audit, Budget API |
| `accounting` | Journal Ledger, Closing, Payable, Receivable, Expenditure Resolution, Tax, Reporting API |
| `products` | Loan, Deposit, Asset Lease API |
| `risk` | Account Mart, ECL, Reconciliation API |
| `platform` | Gateway와 Frontend |
| `apis` | 17개 API, Gateway, Frontend |
| `migration` | 한 context씩 실행하는 migration-runner와 runtime grant helper |
| `batch` | 15개 Batch 정의; 자동 Job 실행은 기본 비활성 |

## Windows / Podman 환경 Compose 설치 가이드

Windows에서 WSL2 + Podman 또는 Docker를 사용할 때 `docker compose` 명령을 실행하기 위한 CLI 설치 방법입니다.

### 방법 1: winget으로 Docker Compose CLI 설치 (권장)
Windows PowerShell 관리자 권한에서 실행합니다.
```powershell
winget install Docker.DockerCompose
```
설치 완료 후 새 터미널에서 버전을 확인합니다.
```powershell
docker-compose --version
# 또는
docker compose version
```

### 방법 2: Python podman-compose 설치
```powershell
pip install podman-compose
podman-compose --version
```

## Self-contained PostgreSQL vs Local H2 개념

- **Local Profile (H2 In-Memory DB)**: 빠른 단위 테스트 및 모듈 단독 개발용 (`application-local.yml`). 외부 DB 설치 없이 `./gradlew :module:api:bootRun` 또는 IntelliJ에서 단독 구동.
- **Dev Profile (PostgreSQL + Docker Compose)**: 마이크로서비스 간 연동 테스트 및 E2E 검증용 (`compose.self-contained.yml`). 실제 PostgreSQL 17개 DB 스키마, Redis, Kafka 컨테이너와 함께 풀스택 구동.

## Self-contained PostgreSQL 실행 절차

예제는 실제 secret이 아닙니다. 복사 후 모든 `replace-with-...` 값을 바꿉니다.

```powershell
Copy-Item .env.dev.example .env.dev
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\validate-dev-env.ps1 `
  -Mode SelfContained -EnvFile .\.env.dev
```

Docker Compose provider가 있는 환경에서 먼저 렌더링만 확인합니다. `config --quiet`을 사용해
환경값이 터미널에 출력되지 않게 합니다.

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml `
  --profile self-contained --profile apis config --quiet
```

DB bootstrap과 플랫폼 기반을 먼저 시작합니다.

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml `
  --profile self-contained up -d postgres-db redis kafka config-server discovery
```

새 DB에는 schema가 없으므로 API를 먼저 열지 않습니다. 17개 READY context 각각에 대해
승인된 owner secret을 현재 shell/secret provider로 주입하고 `migrate`, 이어서 `validate`를
실행합니다. 다음은 Auth 한 context의 예시이며 값은 명령 기록이나 문서에 저장하지 않습니다.

```powershell
$env:MIGRATION_CONTEXT = 'auth'
$env:MIGRATION_DB_URL = 'jdbc:postgresql://postgres-db:5432/auth_dev'
$env:MIGRATION_DB_USER = 'auth_dev_owner'
$env:MIGRATION_DB_PASSWORD = '<approved-local-owner-secret>'
$env:MIGRATION_EXPECTED_DATABASE = 'auth_dev'
$env:MIGRATION_ALLOW_MIGRATE = 'true'
$env:MIGRATION_CHANGE_TICKET = 'LOCAL-DEV-BOOTSTRAP'
$env:MIGRATION_ACTION = 'migrate'

docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml `
  --profile self-contained --profile migration run --rm migration-runner

$env:MIGRATION_ACTION = 'validate'
docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml `
  --profile self-contained --profile migration run --rm migration-runner
```

전체 migration 이후 runtime role에 업무 테이블 DML/sequence 권한을 부여하고 Flyway history
권한을 회수합니다.

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml `
  --profile self-contained --profile migration run --rm runtime-grants
```

`self-contained-schema-check`는 17개 DB에서 업무 테이블 전체의 runtime DML 권한, sequence
사용 권한, Flyway history 비노출을 모두 확인해야 healthy가 됩니다. 그 전에는 API/Batch가
열리지 않습니다.

전체 API와 Edge를 시작합니다.

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml `
  --profile self-contained --profile apis up -d --build --wait
```

업무군만 실행할 때는 `foundation`, `accounting`, `products`, `risk` 중 하나와 필요하면
`platform`을 선택합니다.

## Shared external-dev PostgreSQL

공유 개발 서버 주소와 자격정보는 `.env.external-dev` 또는 승인된 secret provider에서만
주입합니다. 저장소에 host나 값을 기록하지 않습니다.

```powershell
Copy-Item .env.external-dev.example .env.external-dev
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\validate-dev-env.ps1 `
  -Mode ExternalDev -EnvFile .\.env.external-dev

docker compose --env-file .env.external-dev `
  -f docker-compose.yml -f compose.external-dev.yml `
  --profile external-dev --profile apis config --quiet

docker compose --env-file .env.external-dev `
  -f docker-compose.yml -f compose.external-dev.yml `
  --profile external-dev --profile apis up -d --build --wait
```

이 모드는 `self-contained` profile을 선택하지 않으므로 PostgreSQL/Redis/Kafka 컨테이너와
host port를 만들지 않습니다. `external-dev-db-check`는 17개 runtime 계정으로 모두 접속해
업무 테이블 전체의 DML 권한, sequence 사용 권한, Flyway history 비노출을 확인합니다. 외부
DB migration과 권한 변경은 자동 실행하지 않습니다. JDBC URL에 승인된 `sslmode` 같은 연결
옵션이 있으면 probe도 같은 옵션을 보존합니다.

## 저자원 external-dev 인증 스택 (#520)

전체 `apis` 또는 `foundation` profile은 이 용도에 너무 큽니다. `apis`는 17개 API를,
`foundation`은 Auth와 Master Data 외에 Budget과 Internal Audit까지 선택합니다. 4 CPU 개발
서버에서는 다음 **7개 컨테이너만** 사용하는 별도 실행기를 사용합니다.

```text
PostgreSQL 권한 + Redis PING gate
  -> Config Server
  -> Discovery
  -> Auth + Master Data
  -> Gateway
  -> Frontend
```

여기서 `external-dev`는 **Compose가 외부 개발 DB 모드를 선택하는 이름**입니다. Spring
프로파일 이름이 아닙니다. Config Server는 파일 설정 저장소를 읽기 위해 `native`, 나머지
Java 서비스는 모듈별 컨테이너 정본인 `docker`를 사용합니다. 따라서 실행 로그에
`docker`가 보이는 것은 정상이며, 아래 Compose 프로젝트 라벨까지 일치해야 #520의
`external-dev` 실행으로 판단합니다.

### 1단계: 승인된 환경 파일 준비

`.env.external-dev`는 Git에 커밋하지 않고 mode `600`으로 보관합니다. 다음 11개 키가
필요하지만, 값은 문서·Issue·터미널 출력에 붙이지 않습니다.

```text
ENCRYPT_KEY
AUTH_JWT_SECRET
AUTH_INTERNAL_API_TOKEN
AUTH_DB_URL / AUTH_DB_USER / AUTH_DB_PASSWORD
DEV_DB_HOST / DEV_DB_PORT / DEV_DB_NAME / DEV_DB_USER / DEV_DB_PASSWORD
```

파일이 빈 스캐폴드이거나 승인된 외부 Auth/Master Data DB 정보가 없으면 여기서 멈춥니다.
현재 실행 중인 컨테이너 환경이나 PostgreSQL 컨테이너에서 값을 추출하지 않습니다.

### 2단계: 값 비노출 사전 검사

저장소 루트에서 실행합니다. 성공 시 키 개수와 PASS만 출력하며 값과 렌더링 결과는
출력하지 않습니다.

```bash
python3 tools/run-minimal-auth-external-dev.py preflight \
  --env-file .env.external-dev --engine podman
```

Docker 사용자는 `--engine docker`로 바꿉니다. Podman은 이 서버처럼 최신
`docker-compose` provider를 사용하는 구성을 기준으로 합니다. Python `podman-compose`의
동일 동작은 별도 검증 없이 주장하지 않습니다.

### 3단계: 이미지 순차 빌드

```bash
python3 tools/run-minimal-auth-external-dev.py build \
  --env-file .env.external-dev --engine podman
```

Config Server → Discovery → Auth → Master Data → Gateway → Frontend 순서로 하나씩 빌드합니다.
Java 빌드는 `--max-workers=1`, Compose는 `COMPOSE_PARALLEL_LIMIT=1`을 사용합니다. 전체 API나
Batch image는 빌드하지 않습니다.

### 4단계: 기동과 값 비노출 smoke

기존 `config-server`, `discovery`, `auth`, `master-data`, `gateway`, `account-frontend`가
실행 중이면 새 스택과 중복되어 메모리를 사용합니다. 실행기는 이를 감지해 `up`을
실패시킵니다. 승인된 환경값과 새 image가 준비되기 전에는 기존 컨테이너를 먼저 멈추지
마세요. 교체 시점에는 기존 6개 이름만 정확히 정지하고, 새 스택 검증 실패 시 같은 이름을
다시 시작할 수 있게 기존 컨테이너를 삭제하지 않습니다.

```bash
python3 tools/run-minimal-auth-external-dev.py up \
  --env-file .env.external-dev --engine podman
python3 tools/run-minimal-auth-external-dev.py smoke \
  --env-file .env.external-dev --engine podman
python3 tools/run-minimal-auth-external-dev.py status --engine podman
```

- Frontend: <http://127.0.0.1:13000>
- Gateway: <http://127.0.0.1:18000>
- 기존 3000/8000 컨테이너와 충돌하지 않도록 격리 기본 포트를 사용합니다.
- `MINIMAL_DEV_FRONTEND_PORT`, `MINIMAL_DEV_GATEWAY_PORT`로 바꿀 수 있습니다.
- Config/Discovery/Auth/Master Data/DB gate는 호스트 포트를 만들지 않습니다.
- DB gate는 Auth와 Master Data 두 DB의 접속, 업무 테이블 권한, sequence 권한,
  Flyway history 비노출만 조회하고 기존 `account-redis:6379`에 값 없는 `PING`만 보냅니다.
  migration, DDL/DML, Redis key 읽기·쓰기는 실행하지 않습니다.
- Frontend의 빈 로그인 요청이 Gateway를 거쳐 Auth의 기존 HTTP 400 검증 응답으로
  돌아오는지만 최대 30회, 5초 간격으로 확인하고 응답 본문·토큰·계정정보를 출력하지
  않습니다. 각 요청은 Node에서 4초, Compose exec 프로세스에서 15초로 제한됩니다. Eureka
  등록이 health보다 늦는 정상 첫 기동을 위한 제한된 재시도입니다.
- Java 컨테이너 CPU 합계는 3 CPU, 전체 7개 제한 합계는 4 CPU 미만입니다. Java는 각
  1 GiB, Frontend는 768 MiB 상한을 사용하며 실제 사용량은 `status`로 확인합니다.
- 이 최소 모드에서는 tracing을 끄고 console-only Logback 파일을 사용해 아직 복구 전인
  `localhost:5000` Logstash로 재접속하지 않습니다. 실제 관측 주소 통일은 #565가 담당합니다.

Compose 서비스 이름을 `minimal-*`로 분리했기 때문에 같은 `account-network`의 기존
`config-server`, `auth`, `gateway` 컨테이너 DNS와 충돌하지 않습니다. 기존
`account-redis`에는 gate가 값 없는 `PING`을 보내고, Auth에는 기존 모듈 Docker 계약과 같은
`SPRING_DATA_REDIS_HOST/PORT`를 전달합니다. 이 브랜치에서 실제로 확인한 것은 PING까지이며
Auth health 반영은 승인된 env로 하는 live gate에 남겨 둡니다. 기존 Redis를 생성·재시작·삭제하지
않습니다. Zipkin, Logstash, Elasticsearch, Kibana, Prometheus, Grafana와 Batch는 이
최소 스택에 포함하지 않으며 각각 #561~#566이 담당합니다.

빌드부터 smoke까지 한 번에 실행하려면 같은 인자로 `all`을 사용할 수 있습니다. `up --wait`
또는 `all`의 smoke/status가 실패하면 실행기가 고정 project label의 컨테이너만 자동 정지하고
volume은 보존합니다. 개별 `smoke` 실패는 원인 확인을 위해 자동 정지하지 않으므로 아래
`stop`을 직접 실행합니다.

### 5단계: 정확한 롤백

```bash
python3 tools/run-minimal-auth-external-dev.py stop --engine podman
```

고정 Compose project label이 붙은 #520 컨테이너만 정지합니다. 환경 파일을 잃어도 정지가
가능하며 기존 컨테이너, 외부 DB, `account-network`, Redis와 named volume은 보존합니다.
`down -v`, `prune`, 전체 컨테이너 일괄 정지는 사용하지 않습니다.

## Batch

Batch 전체를 `up`하지 않습니다. 실행할 한 서비스와 승인된 Job/식별 파라미터를 명시합니다.

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml `
  --profile self-contained --profile batch run --rm `
  -e SPRING_BATCH_JOB_ENABLED=true `
  closing-batch `
  --spring.batch.job.name=fxValuationJob `
  valuationDate=2026-04-30 valuationBatchId=20260430
```

기본 `SPRING_BATCH_JOB_ENABLED=false`, non-web, host port 없음이 유지됩니다. Job별 restart와
멱등성 계약은 해당 모듈 문서를 따릅니다.

## 종료와 롤백

```powershell
docker compose --env-file .env.dev `
  -f docker-compose.yml -f compose.self-contained.yml down
```

`down -v`를 사용하지 않습니다. Compose 변경 롤백은 Issue #66 커밋을 revert하고 컨테이너만
내립니다. Named volume, 적용된 migration, 외부 DB와 registry image는 자동 삭제하지 않습니다.

## 검증 경계

이 저장소에서 Docker CLI 또는 Podman Compose provider가 없으면 다음 정적 게이트까지만
증명할 수 있습니다.

```powershell
.\gradlew.bat :config-server:test :gateway:test :migration-runner:test `
  :migration-runner:bootJar verifyProductionRuntimeDependencies --offline
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\validate-dev-env.ps1 -SelfTest
git diff --check
```

실제 close gate는 Docker/Podman Compose provider에서 두 DB mode의 `config --quiet`, 대표
image build, self-contained 17-context migrate/validate/grant, API `up --wait`, Gateway route,
Frontend와 대표 Batch PostgreSQL smoke입니다.
