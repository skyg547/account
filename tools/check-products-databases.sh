#!/bin/sh
set -eu

# Resolve the shared, read-only ACL gate without sourcing any dotenv file.
script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
. "$script_dir/../postgres/runtime/check-products-risk-database.sh"

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
