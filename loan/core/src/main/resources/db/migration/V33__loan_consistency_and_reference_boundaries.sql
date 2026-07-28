-- V33: Loan-owned reference values, optimistic locking, and deterministic idempotency indexes.
-- V30~V32 are already published migrations and remain checksum-stable.

ALTER TABLE loans
    ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE deferred_item_types
    ADD COLUMN IF NOT EXISTS deferred_asset_account_ref VARCHAR(20);

ALTER TABLE deferred_item_types
    ADD COLUMN IF NOT EXISTS recognized_income_account_ref VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uq_loan_disbursal_loan
    ON loan_disbursals (loan_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_eir_schedule_loan_date
    ON eir_amortization_schedules (loan_id, schedule_date);

CREATE UNIQUE INDEX IF NOT EXISTS uq_loan_accrual_log_loan_date
    ON loan_accrual_log (loan_id, accrual_date);

CREATE INDEX IF NOT EXISTS ix_loans_status_id
    ON loans (status, id);

-- @todo loan_amortization_schedule_entries는 EIR 스케줄과 중복된 레거시 테이블입니다.
-- 완료 조건은 운영 데이터 대조/이관 보고서, 소비처 0건 확인, 백업·복구 리허설 후 별도 migration으로
-- 제거하는 것입니다. 이 forward migration에서는 기존 데이터를 파괴하지 않습니다.
