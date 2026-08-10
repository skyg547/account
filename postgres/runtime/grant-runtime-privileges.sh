#!/bin/sh
set -eu

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${ACCOUNT_DATABASES:?ACCOUNT_DATABASES is required}"

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

    app="${database}_app"
    echo "Granting runtime privileges in '$database' to '$app'"

    psql \
        --username "$POSTGRES_USER" \
        --dbname "$database" \
        --set=ON_ERROR_STOP=1 \
        --set=app="$app" <<'SQL'
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app')
\gexec

SELECT format(
    'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE %I.%I TO %I',
    schemaname,
    tablename,
    :'app')
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename NOT LIKE 'flyway\_schema\_history%' ESCAPE '\'
\gexec

SELECT format(
    'REVOKE ALL PRIVILEGES ON TABLE %I.%I FROM %I',
    schemaname,
    tablename,
    :'app')
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename LIKE 'flyway\_schema\_history%' ESCAPE '\'
\gexec

SELECT format(
    'GRANT USAGE, SELECT ON SEQUENCE %I.%I TO %I',
    sequence_schema,
    sequence_name,
    :'app')
FROM information_schema.sequences
WHERE sequence_schema = 'public'
\gexec
SQL
done

IFS=$old_ifs
