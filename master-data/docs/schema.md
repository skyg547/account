# master-data schema

## Entity Overview

```mermaid
erDiagram
    ACCOUNT_SUBJECTS }o--o| ACCOUNT_SUBJECTS : parent
    DEPARTMENTS }o--o| DEPARTMENTS : parent
    BUSINESS_PARTNERS ||--o{ BUSINESS_PARTNER_ACCOUNTS : has
    CURRENCIES ||--o{ EXCHANGE_RATES : from
    CURRENCIES ||--o{ EXCHANGE_RATES : to
    MASTER_DATA_CHANGE_REQUESTS }o--|| MASTER_DATA_TARGET : controls
```

## Core Tables

### `account_subjects`

Chart of accounts reference data.

Key fields:

- `code`
- `name`
- `parent_code`
- `category`
- `account_type`
- `balance_type`
- `report_line`
- `regulatory_mapping_code`
- `unsettled`
- `fixed_asset`
- `valid_from`
- `valid_to`
- `audit_user`

### `business_partners`

Customers, vendors, banks, and other counterparties.

Key fields:

- `id`
- `business_partner_code`
- `business_partner_name`
- `registration_number`
- `ceo_name`
- `business_type`
- `business_item`
- `partner_type`
- `use_yn`
- `kyc_status`
- `risk_rating`
- `valid_from`
- `valid_to`

### `business_partner_accounts`

Payment or settlement account information for a business partner.

### `departments`

Organization, cost-center, and profit-center reference data.

Key fields:

- `code`
- `name`
- `parent_code`
- `type`
- `valid_from`
- `valid_to`

### `currencies`

Currency master data.

### `exchange_rates`

Effective exchange rates. The logical uniqueness is `from_currency_code + to_currency_code + effective_date`.

### `fiscal_periods`

Accounting period status such as `OPEN`, `CLOSED`, or `PERMANENTLY_CLOSED`.

### `products`

Product or service reference data.

### `master_data_change_requests`

Approval and audit control for master-data changes.

Key fields:

- `id`
- `target_type`
- `target_key`
- `change_type`
- `status`
- `effective_date`
- `requested_version`
- `requested_by`
- `approved_by`
- `requested_at`
- `approved_at`
- `reason`
- `payload_json`

Useful indexes:

- `status + requested_at` for pending approval queues.
- `target_type + target_key + requested_version` for change history lookup.
