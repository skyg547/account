#!/bin/sh
set -eu

check_database() {
    label=$1
    jdbc_url=$2
    username=$3
    password=$4

    case "$jdbc_url" in
        jdbc:postgresql://*/*) ;;
        *) echo "Invalid PostgreSQL URL contract for $label" >&2; return 1 ;;
    esac

    connection=${jdbc_url#jdbc:postgresql://}
    authority=${connection%%/*}
    database_and_query=${connection#*/}
    database=${database_and_query%%\?*}
    connection_uri=${jdbc_url#jdbc:}
    case "$authority" in
        *@*) echo "Embedded PostgreSQL credentials are not allowed for $label" >&2; return 1 ;;
    esac
    case "$authority" in
        *:*) host=${authority%:*}; port=${authority##*:} ;;
        *) host=$authority; port=5432 ;;
    esac

    case "$host" in
        ''|postgres-db|localhost|127.0.0.1|0.0.0.0)
            echo "External development host is not allowed for $label" >&2
            return 1
            ;;
    esac
    case "$port" in ''|*[!0-9]*) return 1 ;; esac
    case "$database" in ''|*[!a-z0-9_]*) return 1 ;; esac

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
    test "$result" = "1"
}

: "${ACCOUNT_MART_DB_URL:?ACCOUNT_MART_DB_URL is required}"
: "${ACCOUNT_MART_DB_USER:?ACCOUNT_MART_DB_USER is required}"
: "${ACCOUNT_MART_DB_PASSWORD:?ACCOUNT_MART_DB_PASSWORD is required}"

check_database account-mart "$ACCOUNT_MART_DB_URL" "$ACCOUNT_MART_DB_USER" "$ACCOUNT_MART_DB_PASSWORD"
check_database asset-lease "$ASSET_LEASE_DB_URL" "$ASSET_LEASE_DB_USER" "$ASSET_LEASE_DB_PASSWORD"
check_database auth "$AUTH_DB_URL" "$AUTH_DB_USER" "$AUTH_DB_PASSWORD"
check_database budget "$BUDGET_DB_URL" "$BUDGET_DB_USER" "$BUDGET_DB_PASSWORD"
check_database closing "$CLOSING_DB_URL" "$CLOSING_DB_USER" "$CLOSING_DB_PASSWORD"
check_database deposit "$DEPOSIT_DB_URL" "$DEPOSIT_DB_USER" "$DEPOSIT_DB_PASSWORD"
check_database ecl "$ECL_DB_URL" "$ECL_DB_USER" "$ECL_DB_PASSWORD"
check_database expenditure-resolution "$EXPENDITURE_RESOLUTION_DB_URL" "$EXPENDITURE_RESOLUTION_DB_USER" "$EXPENDITURE_RESOLUTION_DB_PASSWORD"
check_database internal-audit "$INTERNAL_AUDIT_DB_URL" "$INTERNAL_AUDIT_DB_USER" "$INTERNAL_AUDIT_DB_PASSWORD"
check_database journal-ledger "$JOURNAL_LEDGER_DB_URL" "$JOURNAL_LEDGER_DB_USER" "$JOURNAL_LEDGER_DB_PASSWORD"
check_database loan "$LOAN_DB_URL" "$LOAN_DB_USER" "$LOAN_DB_PASSWORD"
check_database master-data "$MASTER_DATA_DB_URL" "$MASTER_DATA_DB_USER" "$MASTER_DATA_DB_PASSWORD"
check_database payable "$PAYABLE_DB_URL" "$PAYABLE_DB_USER" "$PAYABLE_DB_PASSWORD"
check_database receivable "$RECEIVABLE_DB_URL" "$RECEIVABLE_DB_USER" "$RECEIVABLE_DB_PASSWORD"
check_database reconciliation "$RECONCILIATION_DB_URL" "$RECONCILIATION_DB_USER" "$RECONCILIATION_DB_PASSWORD"
check_database reporting "$REPORTING_DB_URL" "$REPORTING_DB_USER" "$REPORTING_DB_PASSWORD"
check_database tax "$TAX_DB_URL" "$TAX_DB_USER" "$TAX_DB_PASSWORD"

echo "External development database gate PASS (17 contexts)"
