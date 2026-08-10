#!/bin/sh
set -eu

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${ACCOUNT_DATABASES:?ACCOUNT_DATABASES is required}"
: "${ACCOUNT_DB_OWNER_PASSWORD:?ACCOUNT_DB_OWNER_PASSWORD is required}"
: "${ACCOUNT_DB_APP_PASSWORD:?ACCOUNT_DB_APP_PASSWORD is required}"

if [ "$ACCOUNT_DB_OWNER_PASSWORD" = "$ACCOUNT_DB_APP_PASSWORD" ]; then
    echo "Development owner and runtime passwords must differ" >&2
    exit 1
fi

old_ifs=$IFS
IFS=','

for raw_database in $ACCOUNT_DATABASES; do
    database=$(printf '%s' "$raw_database" | tr -d '[:space:]')
    case "$database" in
        ''|*[!a-z0-9_]*)
            echo "Invalid development database name: $database" >&2
            exit 1
            ;;
    esac
    if [ "$database" = "$POSTGRES_DB" ]; then
        echo "Service database must differ from the administrative database: $database" >&2
        exit 1
    fi

    owner="${database}_owner"
    app="${database}_app"
    echo "Provisioning development database '$database' with owner '$owner' and runtime role '$app'"

    psql \
        --username "$POSTGRES_USER" \
        --dbname "$POSTGRES_DB" \
        --set=ON_ERROR_STOP=1 \
        --set=database="$database" \
        --set=owner="$owner" \
        --set=owner_password="$ACCOUNT_DB_OWNER_PASSWORD" \
        --set=app="$app" \
        --set=app_password="$ACCOUNT_DB_APP_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'owner', :'owner_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'owner')
\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'owner', :'owner_password')
\gexec

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app')
\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'app', :'app_password')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', :'database', :'owner')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'database')
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', :'database', :'owner')
\gexec

SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'database')
\gexec

SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'database', :'app')
\gexec
SQL

    psql \
        --username "$POSTGRES_USER" \
        --dbname "$database" \
        --set=ON_ERROR_STOP=1 \
        --set=owner="$owner" \
        --set=app="$app" <<'SQL'
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SELECT format('ALTER SCHEMA public OWNER TO %I', :'owner')
\gexec

SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app')
\gexec
SQL
done

IFS=$old_ifs

psql \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set=ON_ERROR_STOP=1 \
    --set=database_manifest="$ACCOUNT_DATABASES" <<'SQL'
CREATE TABLE IF NOT EXISTS public.account_dev_bootstrap_status (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    database_manifest text NOT NULL,
    completed boolean NOT NULL,
    completed_at timestamptz NOT NULL
);

INSERT INTO public.account_dev_bootstrap_status (
    singleton,
    database_manifest,
    completed,
    completed_at
)
VALUES (true, :'database_manifest', true, clock_timestamp())
ON CONFLICT (singleton) DO UPDATE
SET database_manifest = EXCLUDED.database_manifest,
    completed = EXCLUDED.completed,
    completed_at = EXCLUDED.completed_at;
SQL
