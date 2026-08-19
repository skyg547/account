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

Java 서비스는 저장소 루트 `Containerfile`과 정확한 `GRADLE_PROJECT`, `JAR_DIRECTORY`만
사용합니다. Frontend 개발 컨테이너는 `frontend/Containerfile.dev`, 운영 image는
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

Config Server와 Discovery는 선택된 업무 profile의 공통 선행 서비스입니다. Gateway는 특정
업무 API 하나의 장애 때문에 시작이 차단되지 않으며, Frontend만 Gateway readiness를
기다립니다. 업무 API/Batch는 host port를 공개하지 않습니다. 개발 host에는 Gateway와
Frontend, self-contained 인프라만 `127.0.0.1`로 bind합니다.

## Self-contained PostgreSQL

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
