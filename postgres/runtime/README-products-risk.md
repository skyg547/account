# Products / Risk development database provisioning (GH-649)

`provision-products-risk-external-dev.py` targets only `deposit`, `loan`,
`asset-lease`, `account-mart`, `ecl` and `reconciliation` in the existing
`account-postgres` container. Budget is excluded. It imports Accounting's existing
stdin transport and environment parsing helpers without altering that workflow.

The user-approved local `.env.external-dev` must already be an owned, single-link,
non-symlink regular file with mode `0600`. Its canonical `AUTH_DB_URL` supplies the
external development endpoint; no administrator password is read. Podman local
socket administration and the existing `account-network` are required. The Java 17
runner image must already exist; image pulls and package installation are disabled.

Build the exact migration artifact with the repository's approved JDK/toolchain:

```sh
./gradlew :migration-runner:test :migration-runner:bootJar --offline --no-daemon --max-workers=1
```

Use the canonical local env file path, including when running from an isolated
worktree. Do not source, print, copy into Git, or attach the env file to diagnostics.
The following preflight reads inputs internally without printing their values and
performs no database mutation:

```sh
python3 postgres/runtime/provision-products-risk-external-dev.py \
  --env-file /home/ho/dev/account/.env.external-dev \
  --jar migration-runner/build/libs/account-migration-runner.jar
```

After the existing Issue #649 authorization and preflight, run the same command
with `--apply`. Repeat `--apply` once to prove idempotent migrate/validate and ACL
reapplication, then run the Products and Risk acceptance gates:

```sh
python3 postgres/runtime/provision-products-risk-external-dev.py \
  --env-file /home/ho/dev/account/.env.external-dev \
  --jar migration-runner/build/libs/account-migration-runner.jar --apply
bash tools/check-products-databases.sh --env-file /home/ho/dev/account/.env.external-dev
bash tools/check-risk-databases.sh --env-file /home/ho/dev/account/.env.external-dev
```

Each context receives `<CONTEXT>_DB_URL`, `<CONTEXT>_DB_USER`,
`<CONTEXT>_DB_PASSWORD` and `<CONTEXT>_DB_OWNER_PASSWORD`, where the context token
uses uppercase underscores. New owner/app passwords are distinct random values;
existing saved credentials are reused and never rotated. A locked atomic mode-600
replacement is flushed before the first DB mutation. Secrets travel to containers
only over stdin, never via argv, container configuration or mounted env files.
Captured child output and errors are redacted.

Schema changes run as `<database>_owner`, with development-only approval ticket
`GH-649`. Runtime `<database>_app` receives CONNECT, public-schema USAGE, table DML
and sequence USAGE/SELECT. PUBLIC/app database, schema, table and sequence grants
are rebuilt transactionally, and owner default table/sequence grants to these
principals are removed globally and for `public`. Future tables remain inaccessible
until this grant workflow runs again. Both Flyway history tables remain inaccessible
to runtime roles. Account Mart partition parents, current children and default
partitions are included in the table catalog; SERIAL and identity sequences use
`pg_class` with `relkind='S'`.

The runner applies domain migrations plus the separate Spring Batch metadata
history, validates both with no pending migrations, checks all table/sequence ACLs,
proves a wrong password is rejected by password authentication, authenticates the
real runtime role and checks that an attempted transactional DDL operation fails.
The original unrelated database catalog, bootstrap manifest and container/image/
mount identity must remain identical. Catalog checks do not read business rows.

The two histories are `flyway_schema_history` and `flyway_schema_history_batch` for
all six contexts. Account Mart uses published V1-V5 and forward-only V6; existing
migration files are never rewritten. A nonempty existing DB must validate before
any changes; incomplete histories require reviewed forward recovery. Existing role
attributes, memberships, database ownership and public-schema ownership are checked
before mutation. A fully provisioned rerun preserves data and credentials.

Rollback means stop application onboarding and retain the new DBs, roles and saved
credentials for investigation. Do not delete databases/volumes, reset passwords,
run Flyway clean/repair, or overwrite published migrations. Failure partway through
six contexts leaves completed contexts intact; an empty database can resume, while
a partial history needs the migration runbook's reviewed recovery path. ACL changes
briefly take catalog locks; migration transaction/locking behavior belongs to the
published versioned SQL. No API/Batch deployment or business-data smoke is implied
by provisioning success.
