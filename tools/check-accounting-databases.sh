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

: "${JOURNAL_LEDGER_DB_URL:?JOURNAL_LEDGER_DB_URL is required}"
: "${JOURNAL_LEDGER_DB_USER:?JOURNAL_LEDGER_DB_USER is required}"
: "${JOURNAL_LEDGER_DB_PASSWORD:?JOURNAL_LEDGER_DB_PASSWORD is required}"

: "${CLOSING_DB_URL:?CLOSING_DB_URL is required}"
: "${CLOSING_DB_USER:?CLOSING_DB_USER is required}"
: "${CLOSING_DB_PASSWORD:?CLOSING_DB_PASSWORD is required}"

: "${PAYABLE_DB_URL:?PAYABLE_DB_URL is required}"
: "${PAYABLE_DB_USER:?PAYABLE_DB_USER is required}"
: "${PAYABLE_DB_PASSWORD:?PAYABLE_DB_PASSWORD is required}"

: "${RECEIVABLE_DB_URL:?RECEIVABLE_DB_URL is required}"
: "${RECEIVABLE_DB_USER:?RECEIVABLE_DB_USER is required}"
: "${RECEIVABLE_DB_PASSWORD:?RECEIVABLE_DB_PASSWORD is required}"

: "${EXPENDITURE_RESOLUTION_DB_URL:?EXPENDITURE_RESOLUTION_DB_URL is required}"
: "${EXPENDITURE_RESOLUTION_DB_USER:?EXPENDITURE_RESOLUTION_DB_USER is required}"
: "${EXPENDITURE_RESOLUTION_DB_PASSWORD:?EXPENDITURE_RESOLUTION_DB_PASSWORD is required}"

: "${TAX_DB_URL:?TAX_DB_URL is required}"
: "${TAX_DB_USER:?TAX_DB_USER is required}"
: "${TAX_DB_PASSWORD:?TAX_DB_PASSWORD is required}"

: "${REPORTING_DB_URL:?REPORTING_DB_URL is required}"
: "${REPORTING_DB_USER:?REPORTING_DB_USER is required}"
: "${REPORTING_DB_PASSWORD:?REPORTING_DB_PASSWORD is required}"

check_database journal-ledger "$JOURNAL_LEDGER_DB_URL" "$JOURNAL_LEDGER_DB_USER" "$JOURNAL_LEDGER_DB_PASSWORD"
check_database closing "$CLOSING_DB_URL" "$CLOSING_DB_USER" "$CLOSING_DB_PASSWORD"
check_database payable "$PAYABLE_DB_URL" "$PAYABLE_DB_USER" "$PAYABLE_DB_PASSWORD"
check_database receivable "$RECEIVABLE_DB_URL" "$RECEIVABLE_DB_USER" "$RECEIVABLE_DB_PASSWORD"
check_database expenditure-resolution "$EXPENDITURE_RESOLUTION_DB_URL" "$EXPENDITURE_RESOLUTION_DB_USER" "$EXPENDITURE_RESOLUTION_DB_PASSWORD"
check_database tax "$TAX_DB_URL" "$TAX_DB_USER" "$TAX_DB_PASSWORD"
check_database reporting "$REPORTING_DB_URL" "$REPORTING_DB_USER" "$REPORTING_DB_PASSWORD"

echo "External development accounting dependency gate PASS (7 PostgreSQL contexts)"
