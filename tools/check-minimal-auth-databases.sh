#!/bin/sh
set -eu

check_database() {
    label=$1
    jdbc_url=$2
    username=$3
    password=$4

    case "$jdbc_url" in
        jdbc:postgresql://*/*) ;;
        *)
            echo "Invalid PostgreSQL URL contract for $label" >&2
            return 1
            ;;
    esac

    connection_uri=${jdbc_url#jdbc:}
    case "$connection_uri" in
        *'@'*)
            echo "Embedded PostgreSQL credentials are not allowed for $label" >&2
            return 1
            ;;
    esac

    if ! result=$(PGPASSWORD="$password" psql \
        --dbname "$connection_uri" \
        --username "$username" \
        --no-psqlrc \
        --tuples-only \
        --no-align \
        --quiet \
        --set=ON_ERROR_STOP=1 \
        --command "SELECT CASE WHEN
            EXISTS (
                SELECT 1 FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename NOT LIKE 'flyway\_schema\_history%' ESCAPE '\\'
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename NOT LIKE 'flyway\_schema\_history%' ESCAPE '\\'
                  AND NOT (
                      has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'SELECT')
                      AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'INSERT')
                      AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'UPDATE')
                      AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'DELETE')
                  )
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_class sequence
                JOIN pg_namespace namespace ON namespace.oid = sequence.relnamespace
                WHERE namespace.nspname = 'public'
                  AND sequence.relkind = 'S'
                  AND NOT (
                      has_sequence_privilege(current_user, sequence.oid, 'USAGE')
                      AND has_sequence_privilege(current_user, sequence.oid, 'SELECT')
                  )
            )
            AND NOT EXISTS (
                SELECT 1 FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename LIKE 'flyway\_schema\_history%' ESCAPE '\\'
                  AND has_table_privilege(current_user, format('%I.%I', schemaname, tablename), 'SELECT')
            )
            THEN 1 ELSE 0 END" 2>/dev/null); then
        echo "External development database connection failed for $label" >&2
        return 1
    fi

    if [ "$result" != "1" ]; then
        echo "External development database privilege gate failed for $label" >&2
        return 1
    fi
}

: "${AUTH_DB_URL:?AUTH_DB_URL is required}"
: "${AUTH_DB_USER:?AUTH_DB_USER is required}"
: "${AUTH_DB_PASSWORD:?AUTH_DB_PASSWORD is required}"
: "${DEV_DB_HOST:?DEV_DB_HOST is required}"
: "${DEV_DB_PORT:?DEV_DB_PORT is required}"
: "${DEV_DB_NAME:?DEV_DB_NAME is required}"
: "${DEV_DB_USER:?DEV_DB_USER is required}"
: "${DEV_DB_PASSWORD:?DEV_DB_PASSWORD is required}"
: "${REDIS_HOST:?REDIS_HOST is required}"
: "${REDIS_PORT:?REDIS_PORT is required}"

case "$REDIS_PORT" in
    *[!0-9]*|'')
        echo "Invalid Redis port contract" >&2
        exit 1
        ;;
esac

check_database auth "$AUTH_DB_URL" "$AUTH_DB_USER" "$AUTH_DB_PASSWORD"
check_database master-data \
    "jdbc:postgresql://${DEV_DB_HOST}:${DEV_DB_PORT}/${DEV_DB_NAME}" \
    "$DEV_DB_USER" \
    "$DEV_DB_PASSWORD"

if ! command -v nc >/dev/null 2>&1; then
    echo "Redis readiness client is unavailable" >&2
    exit 1
fi

redis_response=$(
    printf '*1\r\n$4\r\nPING\r\n' \
        | nc -w 3 "$REDIS_HOST" "$REDIS_PORT" 2>/dev/null \
        | head -c 7
) || true
if [ "$redis_response" != "$(printf '+PONG\r\n')" ]; then
    echo "External development Redis readiness failed" >&2
    exit 1
fi

echo "External development dependency gate PASS (2 PostgreSQL contexts, Redis PING)"
