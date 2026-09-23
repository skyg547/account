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
| 업무 패키지 오버레이 | [`tools/compose.{accounting\|products\|risk}-external-dev.yml`](../../tools/) | `external-dev` | 저자원 인증 기반 스택 위에 특정 업무 패키지만 격리 기동 (가용 메모리 13GiB 최적화) |

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

### Microservice Port Inventory

아래 기본 포트는 각 API의 로컬 `application.yml` 또는 Config Server 설정에서 확인한 현재 값입니다.
루트 Compose와 Accounting/Products/Risk external-dev 오버레이는 API마다 다른 기본 포트를 그대로
쓰지 않고, 모든 API 컨테이너에 `SERVER_PORT: "8080"`을 명시합니다. 따라서 컨테이너 안에서 실행되는
공통 readiness probe도 `http://127.0.0.1:8080/actuator/health/readiness`를 사용해야 합니다.

| Microservice | 로컬/Config 기본 포트 | 루트·업무 패키지 컨테이너 `SERVER_PORT` |
| --- | ---: | ---: |
| Account Mart API | 8085 | 8080 |
| Asset Lease API | 8089 (application) / 8083 (기존 Config Server) | 8080 |
| Auth API | 8084 (Config Server) | 8080 |
| Budget API | 8096 | 8080 |
| Closing API | 8086 | 8080 |
| Deposit API | 8084 | 8080 |
| ECL API | 8083 | 8080 |
| Expenditure Resolution API | 8095 | 8080 |
| Internal Audit API | 8083 | 8080 |
| Journal Ledger API | 8081 | 8080 |
| Loan API | 8087 (Config Server) | 8080 |
| Master Data API | 8082 | 8080 |
| Payable API | 8091 | 8080 |
| Receivable API | 8092 | 8080 |
| Reconciliation API | 8093 | 8080 |
| Reporting API | 8097 (Config Server) | 8080 |
| Tax API | 8094 | 8080 |

`SERVER_PORT`는 Spring 설정의 기본값보다 우선합니다. 새 API를 오버레이에 추가할 때는 서비스의
`environment`에 `SERVER_PORT: "8080"`이 있는지와 상속한 healthcheck의 포트가 같은지 함께
검토합니다. 저자원 최소 인증/거버넌스 오버레이처럼 전용 포트를 유지하는 예외는 해당 서비스의
healthcheck도 같은 전용 포트로 설정합니다. Asset Lease의 application fallback은 8089이지만 기존
Config Server 설정은 아직 8083입니다. external-dev에서는 명시적인 8080 override가 둘보다 우선하며,
Config Server 기본값 정리는 이 Issue의 두 application 설정 변경 범위 밖입니다.

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

## 업무 패키지별 독립 실행 오버레이 (Accounting, Products, Risk)

저자원 환경(예: 가용 메모리 13GiB 호스트)에서 36개 전체 컨테이너를 동시에 기동하면 JVM 힙 메모리 및 호스트 메모리 부족으로 인해 OOM이 발생할 수 있습니다. 또한 루트 `compose.external-dev.yml`은 17개 DB 환경변수를 모두 필수로 요구(`:?`)하므로, 특정 패키지만 테스트하려 해도 미관련 변수 미입력으로 인해 렌더링 자체가 차단됩니다.

이를 해결하기 위해 이미 검증된 저자원 인증 기반 스택([`tools/compose.minimal-auth-external-dev.yml`](../../tools/compose.minimal-auth-external-dev.yml)) 위에 원하는 업무 패키지만 선별하여 독립 기동할 수 있는 3종의 독립 Compose 오버레이를 제공합니다:

1. **Accounting (회계 코어 패키지)**:
   - 파일: `tools/compose.accounting-external-dev.yml`
   - 서비스: `accounting-db-check`, `journal-ledger-api`, `closing-api`, `payable-api`, `receivable-api`, `expenditure-resolution-api`, `tax-api`, `reporting-api`
   - 의존성: 회계 7개 DB 컨텍스트 검증
2. **Products (금융 상품 패키지)**:
   - 파일: `tools/compose.products-external-dev.yml`
   - 서비스: `products-db-check`, `deposit-api`, `loan-api`, `asset-lease-api`
   - 의존성: 금융 상품 3개 DB 컨텍스트 검증
3. **Risk (리스크/데이터 패키지)**:
   - 파일: `tools/compose.risk-external-dev.yml`
   - 서비스: `risk-db-check`, `account-mart-api`, `ecl-api`, `reconciliation-api`
   - 의존성: 리스크 3개 DB 컨텍스트 검증

### 주요 설계 및 리소스 제약
- **네트워크 공유**: `account-network` (external: true)를 통해 실행 중인 Discovery, Config Server, PostgreSQL 컨테이너와 통신합니다.
- **리소스 제한**: API 서비스 당 CPU 0.50, Memory 768m (reservation 256m) 한도를 강제하여 호스트 OOM을 차단합니다.
- **Fail-Closed DB 게이트**: 선택한 패키지의 DB만 검증하며, 타 패키지의 미입력 환경변수로 인한 기동 실패를 방지합니다.

### 기동 및 종료 가이드
```bash
# 1. 기반 스택이 먼저 실행 중이어야 함 (network: account-network 공유)
# podman compose -f tools/compose.minimal-auth-external-dev.yml up -d

# 2. 원하는 업무 패키지 기동 (예: Accounting 패키지)
podman compose -f tools/compose.accounting-external-dev.yml up -d

# 3. 상태 확인
podman compose -f tools/compose.accounting-external-dev.yml ps

# 4. 업무 패키지 중지 (기반 스택은 유지)
podman compose -f tools/compose.accounting-external-dev.yml stop
```

## Docker vs Podman Compose Provider 환경 구성 및 차이점

이 저장소는 Docker와 Podman을 모두 지원하며, Compose CLI를 통해 동일한 스택을 구동할 수 있습니다. 다만 Podman 사용 시 Compose provider 선택에 반드시 주의해야 합니다.

### Docker vs Podman 차이점 및 Provider 요구사항

| 구분 | Docker | Podman |
| :--- | :--- | :--- |
| **데몬 아키텍처** | 루트 데몬 (`dockerd`) 기반 | 데몬리스(Daemonless), rootless 기본 지원 |
| **Compose 명령어** | `docker compose` (공식 플러그인) | `podman compose` (외부 provider 호출) |
| **권장 Provider** | Docker Compose V2 (Go 바이너리) | **`docker-compose` v2.x (Go 바이너리)** (권장) |
| **금지 Provider** | N/A | ⚠️ **`podman-compose` (Python 패키지) 사용 금지** |

> ⚠️ **Podman 사용자 필독 (Provider 함정 주의)**:
> `pip install podman-compose`로 설치되는 Python 기반 `podman-compose`는 최신 Compose 사양의 `name:` 속성(네트워크 격리명)을 올바르게 처리하지 못합니다. 이로 인해 컨테이너들이 서로 다른 네트워크로 분리되어 **서비스 간 DNS 해석 실패 및 통신 단절**이 발생합니다.
> 따라서 Podman 환경에서는 반드시 Go 바이너리 기반의 **`docker-compose` (v2.x)**가 provider로 연결되도록 구성해야 합니다.

### Provider 설치 및 확인 방법

#### 1) Windows / WSL2 환경
Windows PowerShell(관리자)에서 winget을 통해 Docker Compose CLI를 설치합니다.
```powershell
winget install Docker.DockerCompose
```

#### 2) Linux (Ubuntu/Debian) 환경
```bash
# Docker Compose V2 플러그인 설치
sudo apt-get update && sudo apt-get install -y docker-compose-plugin

# 또는 바이너리 직접 다운로드 (~/.docker/cli-plugins/docker-compose)
mkdir -p ~/.docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-linux-x86_64 -o ~/.docker/cli-plugins/docker-compose
chmod +x ~/.docker/cli-plugins/docker-compose
```

#### 3) Provider 버전 검증
새 터미널에서 Compose provider가 정상 인식되는지 확인합니다.
```bash
# Podman 환경
podman compose version

# Docker 환경
docker compose version
```
출력 결과에 `Docker Compose version v2.x.x`가 표시되면 정상입니다.


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

## 초보자용 Podman/Docker 저자원 최소 인증 스택 실행 및 롤백 가이드 (#521, #520)

저사양 개발 서버(예: 4 CPU / 11 GiB RAM) 환경에서 17개 API 전체(`--profile apis`) 또는 `foundation` 프로파일을 기동하면 극심한 CPU/메모리 경합과 OOM(Out of Memory)으로 인해 전체 서비스가 비정상 종료될 수 있습니다.

따라서 초보자 및 저자원 환경에서는 로그인 및 핵심 게이트웨이 검증에 필요한 **7개 필수 컨테이너만** 선별하여 순차 빌드·기동하는 전용 실행기(`tools/run-minimal-auth-external-dev.py`)를 사용합니다.

```text
[최소 인증 스택 토폴로지]
PostgreSQL 권한 + Redis PING gate (minimal-db-check)
  -> Config Server (minimal-config-server)
  -> Discovery (minimal-discovery)
  -> Auth + Master Data (minimal-auth, minimal-master-data)
  -> Gateway (minimal-gateway, 호스트 127.0.0.1:18000)
  -> Frontend (minimal-frontend, 호스트 127.0.0.1:13000)
```

> **용어 주의**: 여기서 `external-dev`는 Compose 프로젝트가 외부 개발 DB 모드를 선택하는 이름이며 Spring 프로파일 이름이 아닙니다. Config Server는 파일 기반 설정 저장을 위해 Spring 프로파일 `native`를 쓰고, 나머지 Java 서비스는 컨테이너 정본인 `docker` 프로파일을 사용합니다. 실행 로그에 `docker` 프로파일이 표시되는 것은 정상입니다.

---

### 1단계: 사전 환경 점검 (Prerequisites)

실행 전 개발 호스트의 런타임 및 Provider 버전을 확인합니다.

```bash
# 1. Java 및 Node/npm 버전 확인 (Java 17+, Node 20 LTS)
java -version
node --version
npm --version

# 2. 컨테이너 엔진 및 Compose provider 확인
podman compose version
# (Docker 환경일 경우)
docker compose version
```

---

### 2단계: 승인된 환경 파일 준비 및 값 비노출 사전 검사 (Preflight)

`.env.external-dev`는 민감한 인증정보를 포함하므로 절대 Git에 커밋하지 않고 파일 권한을 `600`(`chmod 600 .env.external-dev`)으로 설정하여 보관합니다.

다음 12개 필수 키가 설정되어 있어야 합니다 (실제 secret 값은 문서·Issue·터미널에 노출하지 않습니다).

```text
ENCRYPT_KEY
AUTH_JWT_SECRET
AUTH_INTERNAL_API_TOKEN
BFF_GATEWAY_SHARED_SECRET
AUTH_DB_URL / AUTH_DB_USER / AUTH_DB_PASSWORD
DEV_DB_HOST / DEV_DB_PORT / DEV_DB_NAME / DEV_DB_USER / DEV_DB_PASSWORD
```

* `BFF_GATEWAY_SHARED_SECRET`은 32~512-byte 신규 난수로 생성하여 Frontend/Gateway에만 주입합니다.
* 승인된 외부 Auth/Master Data DB 정보가 없거나 파일이 비어 있으면 진행하지 않습니다.

저장소 루트에서 **값 비노출 사전 검사(preflight)**를 실행합니다:

```bash
# Podman 환경
python3 tools/run-minimal-auth-external-dev.py preflight \
  --env-file .env.external-dev --engine podman

# Docker 환경
python3 tools/run-minimal-auth-external-dev.py preflight \
  --env-file .env.external-dev --engine docker
```

성공 시 키 개수와 `PASS`만 출력되며 비밀값이나 렌더링 결과는 화면에 노출되지 않습니다.
추가로 구문 유효성 검증을 위해 `config --quiet`를 확인합니다:

```bash
podman compose --env-file .env.external-dev \
  -f tools/compose.minimal-auth-external-dev.yml config --quiet
```

---

### 3단계: 이미지 1-Worker 순차 빌드 (Sequential Build)

4 CPU 저자원 환경에서는 병렬 빌드로 인한 CPU 과부하를 막기 위해 Java 빌드는 `--max-workers=1`, Compose 빌드는 `COMPOSE_PARALLEL_LIMIT=1`로 제한하여 6개 이미지를 순차 빌드합니다.

```bash
python3 tools/run-minimal-auth-external-dev.py build \
  --env-file .env.external-dev --engine podman
```

* **빌드 순서**: `Config Server` → `Discovery` → `Auth` → `Master Data` → `Gateway` → `Frontend`
* 전체 17개 API나 Batch 이미지는 빌드하지 않습니다.

#### 💡 이미지 재빌드 시 영향과 컨테이너 재생성 조건
1. **소스 코드 변경 시 영향**:
   - 백엔드 Java 코드 변경 시: 해당 모듈의 `bootJar` 패키징 후 이미지를 재빌드해야 합니다.
   - Frontend 화면 코드 변경 시: rootless Podman의 권한 격리와 재현성을 위해 호스트 소스(`/app`)를 bind mount하지 않고 빌드 이미지 산출물을 실행하므로, 소스 수정 시 hot reload가 아닌 **`build` 재실행**이 필수입니다.
2. **컨테이너 재생성 조건**:
   - 이미지를 새로 빌드하더라도 **기존 실행 중인 컨테이너는 이전 이미지를 계속 참조**합니다.
   - 따라서 새 코드를 런타임에 반영하려면 반드시 아래 4단계의 `up`을 다시 실행하여 컨테이너를 재생성(`--force-recreate`)해야 합니다.

---

### 4단계: 기존 컨테이너 안전 정지 및 기동, Smoke 검증

기존에 구동 중이던 `config-server`, `discovery`, `auth`, `master-data`, `gateway`, `account-frontend` 6개 컨테이너가 있으면 메모리 중복 및 포트 충돌이 발생합니다.
따라서 기존 6개 컨테이너만 정확한 이름으로 정지합니다 (컨테이너를 삭제하지 않고 정지만 하여 복구 가능 상태 유지).

```bash
# 1. 기존 6개 애플리케이션 컨테이너만 안전하게 정지 (삭제 아님)
podman stop config-server discovery master-data auth gateway account-frontend

# 2. 최소 인증 스택 기동
python3 tools/run-minimal-auth-external-dev.py up \
  --env-file .env.external-dev --engine podman

# 3. 값 비노출 검증 Smoke 테스트 실행
python3 tools/run-minimal-auth-external-dev.py smoke \
  --env-file .env.external-dev --engine podman

# 4. 컨테이너 리소스 상태 확인
python3 tools/run-minimal-auth-external-dev.py status --engine podman
```

#### 정상 상태 검증 포인트
* **Frontend 로그인 화면**: <http://127.0.0.1:13000/login> 접속 시 HTTP 200 응답 확인.
* **Gateway 헬스 엔드포인트**: <http://127.0.0.1:18000/actuator/health/readiness> 상태 확인.
* **Frontend → Gateway → Auth 인증 경로 검증**: `smoke` 단계에서 빈 로그인 요청 시 Auth 서비스의 기존 입력값 검증 실패 응답인 **HTTP 400**으로 돌아오면 전체 통신 경로가 정상 연결된 것입니다 (응답 본문, 토큰, 계정정보는 비노출).

---

### 5단계: 안전한 종료 및 롤백 절차 (Safe Rollback)

작업 완료 후 또는 검증 실패 시, 아래 명령으로 안전하게 중지하고 롤백합니다:

```bash
# 1. 새 최소 스택 프로젝트 컨테이너만 안전하게 정지
python3 tools/run-minimal-auth-external-dev.py stop --engine podman

# 2. 필요 시 기존 6개 정지 컨테이너 다시 복구 시작
podman start config-server discovery master-data auth gateway account-frontend
```

* 고정 Compose project label(`account-minimal-auth-external-dev`)이 붙은 컨테이너만 정지합니다.
* 외부 PostgreSQL, Redis, named volume, 공유 네트워크는 전혀 훼손되지 않고 보존됩니다.

#### ⚠️ 절대 금지 명령어 (Strictly Prohibited)

초보자가 흔히 저지르는 아래 파괴적 명령어는 데이터 유실 및 환경 파괴를 유발하므로 **절대 실행하지 마십시오**:

1. ❌ **`podman compose down -v` 또는 `docker compose down -v` 사용 금지**:
   - `-v` 옵션은 볼륨(Named Volume)을 강제 삭제하여 PostgreSQL 데이터베이스 내용, Flyway 마이그레이션 이력, Redis 캐시가 영구 손실됩니다.
2. ❌ **`podman container prune -f` / `docker container prune -f` 사용 금지**:
   - 현재 프로젝트와 무관한 개발 서버 호스트의 다른 중지된 컨테이너까지 전부 강제 삭제됩니다.
3. ❌ **`podman image prune -f` / `docker image prune -f` 사용 금지**:
   - 베이스 이미지와 빌드 캐시가 손실되어 다음 빌드 시 다운로드 및 컴파일 시간이 폭증합니다.
4. ❌ **롤백 목적의 `git checkout <commit> -- <files>` 사용 금지**:
   - 로컬 워킹 디렉터리에서 작업 중이던 미커밋 소스 파일이나 설정 파일이 덮어쓰여 영구 삭제될 위험이 있습니다. 롤백은 오직 검토된 Git Revert 커밋 및 서비스 정지(`stop`)로만 수행합니다.

---

### 6단계: 초보자 실무 트러블슈팅 Q&A

* **Q1. Frontend 기동 시 `EACCES: permission denied, open '/app/next-env.d.ts'` 오류가 발생합니다.**
  - **원인**: rootless Podman 환경에서 호스트 소스 디렉터리를 컨테이너 내부 `/app`에 바인드 마운트할 경우, non-root `node` 사용자와 호스트 사용자의 UID/GID 불일치로 쓰기 권한이 거부됩니다.
  - **해결**: 최소 스택에서는 호스트 소스 바인드 마운트를 사용하지 않고 빌드된 이미지 내부 소스를 사용하며, 오직 프로젝트 전용 named volume(`node_modules`, `.next`)만 쓰기 가능하도록 격리되어 있으므로 `python3 tools/run-minimal-auth-external-dev.py build` 후 `up`을 실행합니다.

* **Q2. `smoke` 실행 시 HTTP 503 Service Unavailable 오류가 발생합니다.**
  - **원인**: Eureka 서비스 디스커버리에 Auth 서비스가 등록되기 전에 Gateway가 요청을 라우팅하려고 시도한 경우입니다.
  - **해결**: 정상적인 첫 기동 시 Eureka 등록 지연이 발생할 수 있습니다. `smoke` 스크립트는 최대 30회(5초 간격) 자동으로 재시도하므로 잠시 대기하면 정상적으로 HTTP 400 응답으로 전환됩니다.

* **Q3. 4 CPU 서버에서 메모리 부족 경고가 발생합니다.**
  - **원인**: 전체 17개 API 프로파일(`--profile apis`)을 기동했거나 Java 서비스들의 메모리 상한이 초과된 경우입니다.
  - **해결**: 최소 인증 스택에서는 Java 컨테이너 1 GiB / 0.60 CPU, Frontend 1 GiB 상한이 적용되어 전체 4 CPU 미만을 유지합니다. 고사양 전용 `--profile apis` 대신 반드시 최소 스택 실행기(`run-minimal-auth-external-dev.py`)를 사용하십시오.

## Ghost Container & Systemd Healthcheck Timer Troubleshooting

rootless Podman은 주기적인 healthcheck를 사용자 systemd transient timer로 실행할 수 있습니다.
컨테이너 프로세스나 `conmon`이 비정상 종료되면 호스트의 `/proc/<pid>`는 사라졌는데 Podman DB는
계속 `running`, `podman ps`는 `Up (unhealthy)`로 표시할 수 있습니다. 이때 OCI runtime을 직접
확인하면 `crun state <container-id>`가 `the container is not running`을 반환하지만,
`podman inspect`의 healthcheck `FailingStreak`은 수만 회까지 계속 증가할 수 있습니다. 전체 컨테이너
ID를 이름으로 쓰는 `<64자리-ID>.timer`가 남아 Podman socket을 반복 호출하기 때문입니다.
`podman ps -a --sync`도 끊어진 runtime/conmon 상태를 항상 `exited`로 바꾸지는 못합니다.

단순히 readiness가 실패했지만 `/proc/<pid>`가 존재하는 실행 컨테이너는 ghost가 아닙니다. 먼저
포트와 `/actuator/health/readiness` 응답을 확인합니다. 반대로 state가 `running`이어도 PID가 0,
비정상 값, 또는 `/proc/<pid>`가 없으면 helper는 effective state를 `dead (pid missing)`으로 바꿉니다.
보고 예시는 다음과 같습니다.

```text
<container-id>  <name>  running -> dead (pid missing)
```

저장소 helper는 기본적으로 보고만 합니다. 동기화 전후 snapshot 모두에서 Podman이 보고한 PID와
`/proc`을 대조하며, 이 effective state로 ghost와 stale healthcheck timer를 함께 판정합니다.
동기화 중 `running`에서 `exited`/`stopped`로 바뀐 컨테이너와 계속
`unknown`/`stopping`/`removing`인 컨테이너도 기존과 같이 후보에 포함합니다.

```bash
# 1. 대상만 확인: 컨테이너나 unit을 삭제/중지하지 않음
sh tools/cleanup-ghost-containers.sh

# 2. 출력된 전체 64자리 ID와 이름이 이 개발 스택의 대상인지 검토한 뒤에만 적용
sh tools/cleanup-ghost-containers.sh --apply <full-container-id> [<full-container-id> ...]

# 3. cleanup 후 반드시 0/0/0 재확인
sh tools/cleanup-ghost-containers.sh
# Ghost containers: 0
# Stale healthcheck timers: 0
# Port-dropped containers: 0
```

`--apply`는 사용자가 명시한 전체 64자리 ID만 처리합니다. 현재 state/PID를 다시 확인하고, 살아 있는
PID로 재기동된 대상은 timer와 함께 건너뜁니다. dead ghost라면 정확히 일치하는 user-systemd
`<ID>.timer`와 실행 중인 `<ID>.service`를 중지하고 failed 상태를 reset한 뒤
`podman rm --force --ignore <ID>`를 실행합니다. 끝에는 제거한 ghost 수와 처리한 timer 수를
출력합니다. `--volumes`를 전달하지 않으므로 named/anonymous volume과 image는 보존합니다.
그래도 제거 전에는 컨테이너 이름과 ID를 반드시 검토하고, 업무 컨테이너라면 해당 Compose 파일로
재생성할 준비를 합니다. 전체 호스트를 대상으로 하는 `podman container prune -f`, `podman rm -af`,
`systemctl --user reset-failed` 무대상 실행은 다른 프로젝트까지 건드리므로 사용하지 않습니다.

### 실행 중 컨테이너의 rootlessport 리스너 유실

장기 실행한 rootless Podman 컨테이너에서는 컨테이너의 메인 프로세스는 살아 있어도 호스트 포트
포워딩을 담당하는 `rootlessport` 자식 프로세스만 종료될 수 있습니다. 이때 `podman ps`는 계속
`running`과 `0.0.0.0:3000->3000/tcp` 같은 Published Port를 표시하지만 `ss -tln`에는 해당
호스트 포트가 없고 HTTP 요청은 connection refused(code 7) 또는 reset(code 56)으로 실패합니다.

기본 진단은 실행 중인 컨테이너의 Published TCP Port와 실제 호스트 LISTEN 소켓을 대조합니다.
`ss`가 없으면 `/proc/net/tcp`와 `/proc/net/tcp6`를 사용합니다. 누락이 있으면 컨테이너별로
`Port drop: <ID> <Name> (<port> not listening on host)`와 합계가 출력됩니다.

```bash
# 보고만 수행하며 컨테이너를 변경하지 않음
sh tools/cleanup-ghost-containers.sh

# 감지된 모든 port-drop 컨테이너를 재시작하고 리스너 복구를 검증
sh tools/cleanup-ghost-containers.sh --fix-ports

# 검토한 전체 64자리 ID만 선택적으로 복구
sh tools/cleanup-ghost-containers.sh --fix-ports <full-container-id> [<full-container-id> ...]

# helper 없이 한 컨테이너만 수동 복구할 때
podman restart <container>
ss -tln
```

`--fix-ports`는 실행 중이고 Published TCP Port가 실제로 누락된 대상을 재확인한 뒤에만
`podman restart`를 실행합니다. 재시작 후 모든 Published TCP Port가 다시 LISTEN인지 최대 5회
검증하며, 복구되지 않으면 오류로 종료합니다. 재시작은 컨테이너 내부의 일시적인 서비스 중단을
만드므로 전체 자동 복구 전에 이름과 ID를 검토하고, 데이터베이스 같은 상태 저장 서비스의 재시작
정책도 확인합니다.

### external-dev 서비스를 깨끗하게 재기동하기

cleanup 재확인이 `0/0/0`이면 사용 중인 실행 경로 하나만 선택해 재기동합니다. 전체 17-API 경로는
프로젝트 범위의 `stop` 후 같은 Compose 인자와 profile로 다시 생성합니다. 외부 DB와 volume은
삭제하지 않습니다.

```bash
podman compose --env-file .env.external-dev \
  -f docker-compose.yml -f compose.external-dev.yml \
  --profile external-dev --profile apis stop
podman compose --env-file .env.external-dev \
  -f docker-compose.yml -f compose.external-dev.yml \
  --profile external-dev --profile apis up -d --no-build --pull never --wait
```

저자원 최소 인증 + 업무 패키지 경로에서는 기반 스택을 먼저 재기동한 다음 필요한 패키지만
`account-<package>-external-dev` project 범위로 정지하고 검증 runner로 순차 기동합니다.

```bash
python3 tools/run-minimal-auth-external-dev.py stop --engine podman
python3 tools/run-minimal-auth-external-dev.py up \
  --env-file .env.external-dev --engine podman

# 예: Accounting만 깨끗하게 재기동
podman compose --project-name account-accounting-external-dev \
  --env-file .env.external-dev \
  -f tools/compose.accounting-external-dev.yml --profile external-dev stop
python3 tools/run-business-external-dev.py up --package accounting \
  --env-file .env.external-dev --engine podman
```

Products/Risk는 위 두 명령의 `accounting`을 각각 `products`/`risk`로 바꿉니다. 이미지를 새로
빌드해야 할 때만 먼저 같은 runner의 `build` action을 실행합니다. `down -v`, 전체 container/image
prune, 공유 `account-network` 삭제는 clean restart 절차가 아닙니다.


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
