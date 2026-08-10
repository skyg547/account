#!/bin/sh
set -eu

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${ACCOUNT_DATABASES:?ACCOUNT_DATABASES is required}"

old_ifs=$IFS
IFS=','

for raw_database in $ACCOUNT_DATABASES; do
    database=$(printf '%s' "$raw_database" | tr -d '[:space:]')
    case "$database" in ''|*[!a-z0-9_]*) exit 1 ;; esac
    app="${database}_app"

    result=$(psql \
        --username "$POSTGRES_USER" \
        --dbname "$database" \
        --no-psqlrc \
        --tuples-only \
        --no-align \
        --quiet \
        --set=ON_ERROR_STOP=1 \
        --set=app="$app" <<'SQL'
SELECT CASE WHEN
    EXISTS (
        SELECT 1 FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename NOT LIKE 'flyway\_schema\_history%' ESCAPE '\'
    )
    AND NOT EXISTS (
        SELECT 1 FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename NOT LIKE 'flyway\_schema\_history%' ESCAPE '\'
          AND NOT (
              has_table_privilege(:'app', format('%I.%I', schemaname, tablename), 'SELECT')
              AND has_table_privilege(:'app', format('%I.%I', schemaname, tablename), 'INSERT')
              AND has_table_privilege(:'app', format('%I.%I', schemaname, tablename), 'UPDATE')
              AND has_table_privilege(:'app', format('%I.%I', schemaname, tablename), 'DELETE')
          )
    )
    AND NOT EXISTS (
        SELECT 1 FROM pg_class sequence
        JOIN pg_namespace namespace ON namespace.oid = sequence.relnamespace
        WHERE namespace.nspname = 'public'
          AND sequence.relkind = 'S'
          AND NOT (
              has_sequence_privilege(:'app', sequence.oid, 'USAGE')
              AND has_sequence_privilege(:'app', sequence.oid, 'SELECT')
          )
    )
    AND NOT EXISTS (
        SELECT 1 FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename LIKE 'flyway\_schema\_history%' ESCAPE '\'
          AND has_table_privilege(:'app', format('%I.%I', schemaname, tablename), 'SELECT')
    )
    THEN 1 ELSE 0 END;
SQL
    )
    test "$result" = "1"
done

IFS=$old_ifs
echo "Self-contained runtime schema gate PASS"
