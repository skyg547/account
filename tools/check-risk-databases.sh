#!/bin/sh
set -eu

# Resolve the shared, read-only ACL gate without sourcing any dotenv file.
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
. "$script_dir/../postgres/runtime/check-products-risk-database.sh"

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
