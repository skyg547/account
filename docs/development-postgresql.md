# Development PostgreSQL Contract

이 문서는 개발 환경의 PostgreSQL 소유권, bootstrap, healthcheck와 공유 개발 DB 연결 경계를 정의합니다. 로컬 Spring Boot 직접 실행의 기본 DB는 H2이며, 이 PostgreSQL 계약은 `dev` profile과 컨테이너 실행에만 사용합니다.

## Two Development Modes

| Mode | PostgreSQL owner | Start rule | Service connection |
| --- | --- | --- | --- |
| Self-contained dev | 이 저장소의 `postgres/docker-compose.yml` | `self-contained-db` profile을 명시적으로 선택 | Compose network의 `postgres-db:5432` |
| Shared external dev | 개발 인프라 운영자 | `postgres/compose.external-dev.yml`의 연결 probe만 선택 | 승인된 secret 경로가 `DEV_DB_HOST`, `DEV_DB_PORT`, `DEV_DB_NAME`, `DEV_DB_USER`, `DEV_DB_PASSWORD` 주입 |

두 profile을 동시에 선택하지 않습니다. self-contained PostgreSQL은 기본 profile에 속하지 않으므로 명시하지 않으면 시작되지 않습니다. TCP port 도달 가능 여부는 로그인, schema 준비, migration 완료 또는 DB health를 의미하지 않습니다.

## Database, Owner And Runtime Matrix

Self-contained dev는 bounded context마다 migration 전용 login owner와 장기 실행 앱용
least-privilege runtime login을 분리해 만듭니다. 로컬 격리 환경에서는 owner끼리,
runtime끼리 예제 비밀번호를 공유하지만 owner와 runtime 비밀번호는 서로 달라야 합니다.
공유 개발 환경은 아래 이름을 기준으로 각 role의 secret을 분리해야 합니다.

| Bounded context | Database | Owner role | Runtime role |
| --- | --- | --- | --- |
| Auth | `auth_dev` | `auth_dev_owner` | `auth_dev_app` |
| Master Data | `master_data_dev` | `master_data_dev_owner` | `master_data_dev_app` |
| Internal Audit | `internal_audit_dev` | `internal_audit_dev_owner` | `internal_audit_dev_app` |
| Budget | `budget_dev` | `budget_dev_owner` | `budget_dev_app` |
| Journal Ledger | `journal_ledger_dev` | `journal_ledger_dev_owner` | `journal_ledger_dev_app` |
| Closing | `closing_dev` | `closing_dev_owner` | `closing_dev_app` |
| Loan | `loan_dev` | `loan_dev_owner` | `loan_dev_app` |
| Deposit | `deposit_dev` | `deposit_dev_owner` | `deposit_dev_app` |
| Asset Lease | `asset_lease_dev` | `asset_lease_dev_owner` | `asset_lease_dev_app` |
| Payable | `payable_dev` | `payable_dev_owner` | `payable_dev_app` |
| Receivable | `receivable_dev` | `receivable_dev_owner` | `receivable_dev_app` |
| Reconciliation | `reconciliation_dev` | `reconciliation_dev_owner` | `reconciliation_dev_app` |
| Tax | `tax_dev` | `tax_dev_owner` | `tax_dev_app` |
| Expenditure Resolution | `expenditure_resolution_dev` | `expenditure_resolution_dev_owner` | `expenditure_resolution_dev_app` |
| Reporting | `reporting_dev` | `reporting_dev_owner` | `reporting_dev_app` |
| Account Mart | `account_mart_dev` | `account_mart_dev_owner` | `account_mart_dev_app` |
| ECL | `ecl_dev` | `ecl_dev_owner` | `ecl_dev_app` |

Config Server, Discovery와 Gateway는 database를 사용하지 않습니다.

## Self-Contained Dev

1. Example 파일을 Git이 무시하는 `.env`로 복사합니다.
2. 모든 `replace-...` password를 로컬 전용 값으로 교체합니다.
3. PostgreSQL을 시작하고 health 상태를 확인합니다.

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f postgres/docker-compose.yml --profile self-contained-db up -d postgres-db
docker compose --env-file .env -f postgres/docker-compose.yml ps
```

`postgres-db` healthcheck는 `pg_isready`와 bootstrap manifest를 함께 검사합니다. Budget을 포함한 17개 database와 owner/runtime role 생성이 모두 끝나고 현재 `ACCOUNT_DATABASES` 값과 marker가 일치하기 전에는 healthy가 되지 않습니다. #66의 self-contained 서비스 Compose는 다음 dependency 계약을 사용해야 합니다.

```yaml
depends_on:
  postgres-db:
    condition: service_healthy
```

pgAdmin은 필요한 경우에만 별도 profile로 시작합니다.

```powershell
docker compose --env-file .env -f postgres/docker-compose.yml --profile self-contained-db --profile admin up -d pgadmin
```

init script는 named volume이 처음 만들어질 때만 실행됩니다. `ACCOUNT_DATABASES`를 바꿨다고 기존 volume에 자동 적용되지 않습니다. 기존 개발 DB에 database를 추가할 때는 승인된 DBA 절차를 사용하고, 자동화를 위해 volume을 삭제하지 않습니다.

owner role로 release migration을 모두 적용·validate한 뒤, API/Batch를 시작하기 전에
runtime grant gate를 실행합니다. 이 gate는 현재 업무 테이블과 sequence에만 app 권한을
부여하고 `flyway_schema_history*` 이력 테이블의 모든 runtime 권한을 명시적으로 회수합니다.
default privilege를 사용하지 않으므로 새 migration 뒤에는 반드시 다시 실행해야 하며,
gate가 끝나기 전에는 앱 컨테이너를 시작하지 않습니다.

```powershell
docker compose --env-file .env -f postgres/docker-compose.yml exec -T postgres-db `
  sh /account-runtime/grant-runtime-privileges.sh
```

중지할 때도 volume을 유지합니다.

```powershell
docker compose --env-file .env -f postgres/docker-compose.yml down
```

`down -v`와 named volume 삭제는 사람의 명시 승인 없이는 사용하지 않습니다.

## Shared External Dev

공유 개발 PostgreSQL을 사용할 때는 `self-contained-db` profile을 선택하지 않습니다. 별도 env 파일을 만들고 실제 host/user/password는 Git에 추가하지 않습니다.

```powershell
Copy-Item .env.external-dev.example .env.external-dev
docker compose --env-file .env.external-dev -f postgres/compose.external-dev.yml --profile external-dev up -d external-dev-db-check
docker compose --env-file .env.external-dev -f postgres/compose.external-dev.yml ps
```

`external-dev-db-check`는 실제 login으로 `SELECT 1`을 실행해야 healthy가 됩니다. 이 probe는 database/schema migration을 만들거나 수정하지 않습니다. #66의 external-dev 서비스 Compose는 probe의 `service_healthy`에 의존하고, 같은 `DEV_DB_*` 값을 서비스 컨테이너에 주입해야 합니다.

```text
SPRING_PROFILES_ACTIVE=dev
DEV_DB_HOST=<approved-shared-host>
DEV_DB_PORT=5432
DEV_DB_NAME=<bounded-context-database>
DEV_DB_USER=<bounded-context-runtime-role>
DEV_DB_PASSWORD=<secret-provider-value>
```

host, JDBC URL, username, password를 Git 추적 파일, Issue, worklog, 빌드 로그에 기록하지 않습니다. pgAdmin은 관리 UI일 뿐 애플리케이션 datasource endpoint가 아닙니다.

## Migration And Fail-Fast Policy

`config-repo/application-dev.yml`은 PostgreSQL driver를 고정하고 `DEV_DB_HOST`, `DEV_DB_NAME`, `DEV_DB_USER`, `DEV_DB_PASSWORD`가 없으면 시작을 실패시킵니다. Self-contained 서비스에서도 #66이 각 database/runtime role에 맞는 `DEV_DB_*` 값을 주입합니다. owner credential은 release migration에만 사용합니다. H2 fallback, `ddl-auto=update`, 자동 `schema.sql` 실행은 허용하지 않습니다.

- JPA: `ddl-auto=validate`
- Flyway: 장기 실행 앱에서는 disabled, 승인된 release migration runner에서만 migrate/validate
- SQL init: disabled
- 업무 schema: 각 bounded context의 versioned migration이 소유
- Spring Batch metadata: 해당 Batch 모듈의 migration/초기화 계약이 소유

빈 database에서 서비스가 실패하면 H2나 `ddl-auto=update`로 우회하지 않고 누락된 migration을 해당 모듈 Issue로 보완합니다.

## Verification And Rollback

Docker 사용 가능 환경:

```powershell
docker compose --env-file .env.example -f postgres/docker-compose.yml config
docker compose --env-file .env.external-dev.example -f postgres/compose.external-dev.yml config
docker compose --env-file .env -f postgres/docker-compose.yml --profile self-contained-db up -d postgres-db
docker compose --env-file .env -f postgres/docker-compose.yml ps
```

저장소 정적 계약:

```powershell
.\gradlew :config-server:test --offline --console=plain --max-workers=1 --no-daemon
git diff --check
```

Rollback은 Compose/config/runbook 변경을 revert하고 컨테이너를 `down`하는 것입니다. named volume과 원격 개발 DB는 rollback 대상에 포함하지 않습니다.
