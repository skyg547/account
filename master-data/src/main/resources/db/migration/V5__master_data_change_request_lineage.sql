-- 외부 승인 재시도의 멱등키와 실제 반영 완료 시각을 변경 요청 계보로 보존합니다.
ALTER TABLE master_data_change_requests
    ADD COLUMN IF NOT EXISTS source_reference VARCHAR(120);

ALTER TABLE master_data_change_requests
    ADD COLUMN IF NOT EXISTS applied_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_mdc_source_reference
    ON master_data_change_requests (source_reference);
