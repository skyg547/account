-- Issue #43: 날짜별 EOD/BOD 상태 이력을 운영 가능한 상태 머신으로 승격합니다.
--
-- 기존 Hibernate 생성 테이블(date/is_closed/closed_at/closed_by)이 있으면 상태와 감사
-- 컬럼을 보강하고 Boolean 값을 OPEN/CLOSED로 안전하게 이관합니다. 신규 설치에서도 같은
-- 문장이 동작하도록 먼저 최소 레거시 모양을 만든 뒤, 마지막에 is_closed를 제거합니다.
CREATE TABLE IF NOT EXISTS daily_closing_status (
    date DATE PRIMARY KEY,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP,
    closed_by VARCHAR(80)
);

ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS state VARCHAR(30);
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS created_by VARCHAR(80);
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS updated_by VARCHAR(80);
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS prepared_at TIMESTAMP;
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS prepared_by VARCHAR(80);
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS closing_started_at TIMESTAMP;
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS closing_started_by VARCHAR(80);
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS bod_started_at TIMESTAMP;
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS bod_started_by VARCHAR(80);
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS opened_at TIMESTAMP;
ALTER TABLE daily_closing_status ADD COLUMN IF NOT EXISTS opened_by VARCHAR(80);

UPDATE daily_closing_status
SET state = CASE WHEN is_closed THEN 'CLOSED' ELSE 'OPEN' END
WHERE state IS NULL;

UPDATE daily_closing_status
SET version = COALESCE(version, 0),
    created_at = COALESCE(created_at, closed_at, CURRENT_TIMESTAMP),
    created_by = COALESCE(created_by, closed_by, 'MIGRATION'),
    updated_at = COALESCE(updated_at, closed_at, CURRENT_TIMESTAMP),
    updated_by = COALESCE(updated_by, closed_by, 'MIGRATION'),
    opened_at = CASE
        WHEN state = 'OPEN' THEN COALESCE(opened_at, created_at, CURRENT_TIMESTAMP)
        ELSE opened_at
    END,
    opened_by = CASE
        WHEN state = 'OPEN' THEN COALESCE(opened_by, created_by, 'MIGRATION')
        ELSE opened_by
    END;

ALTER TABLE daily_closing_status ALTER COLUMN state SET NOT NULL;
ALTER TABLE daily_closing_status ALTER COLUMN version SET NOT NULL;
ALTER TABLE daily_closing_status ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE daily_closing_status ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE daily_closing_status ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE daily_closing_status ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE daily_closing_status DROP COLUMN IF EXISTS is_closed;

CREATE INDEX IF NOT EXISTS idx_daily_closing_status_state_date
    ON daily_closing_status (state, date);
