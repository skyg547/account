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

: "${DEPOSIT_DB_URL:?DEPOSIT_DB_URL is required}"
: "${DEPOSIT_DB_USER:?DEPOSIT_DB_USER is required}"
: "${DEPOSIT_DB_PASSWORD:?DEPOSIT_DB_PASSWORD is required}"

: "${LOAN_DB_URL:?LOAN_DB_URL is required}"
: "${LOAN_DB_USER:?LOAN_DB_USER is required}"
: "${LOAN_DB_PASSWORD:?LOAN_DB_PASSWORD is required}"

: "${ASSET_LEASE_DB_URL:?ASSET_LEASE_DB_URL is required}"
: "${ASSET_LEASE_DB_USER:?ASSET_LEASE_DB_USER is required}"
: "${ASSET_LEASE_DB_PASSWORD:?ASSET_LEASE_DB_PASSWORD is required}"

check_database deposit "$DEPOSIT_DB_URL" "$DEPOSIT_DB_USER" "$DEPOSIT_DB_PASSWORD"
check_database loan "$LOAN_DB_URL" "$LOAN_DB_USER" "$LOAN_DB_PASSWORD"
check_database asset-lease "$ASSET_LEASE_DB_URL" "$ASSET_LEASE_DB_USER" "$ASSET_LEASE_DB_PASSWORD"

echo "External development products dependency gate PASS (3 PostgreSQL contexts)"
