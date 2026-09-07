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

: "${ACCOUNT_MART_DB_URL:?ACCOUNT_MART_DB_URL is required}"
: "${ACCOUNT_MART_DB_USER:?ACCOUNT_MART_DB_USER is required}"
: "${ACCOUNT_MART_DB_PASSWORD:?ACCOUNT_MART_DB_PASSWORD is required}"

: "${ECL_DB_URL:?ECL_DB_URL is required}"
: "${ECL_DB_USER:?ECL_DB_USER is required}"
: "${ECL_DB_PASSWORD:?ECL_DB_PASSWORD is required}"

: "${RECONCILIATION_DB_URL:?RECONCILIATION_DB_URL is required}"
: "${RECONCILIATION_DB_USER:?RECONCILIATION_DB_USER is required}"
: "${RECONCILIATION_DB_PASSWORD:?RECONCILIATION_DB_PASSWORD is required}"

check_database account-mart "$ACCOUNT_MART_DB_URL" "$ACCOUNT_MART_DB_USER" "$ACCOUNT_MART_DB_PASSWORD"
check_database ecl "$ECL_DB_URL" "$ECL_DB_USER" "$ECL_DB_PASSWORD"
check_database reconciliation "$RECONCILIATION_DB_URL" "$RECONCILIATION_DB_USER" "$RECONCILIATION_DB_PASSWORD"

echo "External development risk dependency gate PASS (3 PostgreSQL contexts)"
