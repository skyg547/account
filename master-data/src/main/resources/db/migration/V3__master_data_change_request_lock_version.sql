-- 동일 변경 요청을 여러 인스턴스가 동시에 승인/반영할 때 JPA 낙관적 잠금으로 충돌을 감지합니다.
ALTER TABLE master_data_change_requests
    ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 0;