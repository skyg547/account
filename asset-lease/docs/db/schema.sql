-- 고정자산 마스터 (Index 최적화)
CREATE TABLE fixed_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_code VARCHAR(20) NOT NULL UNIQUE,
    asset_name VARCHAR(100) NOT NULL,
    account_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    acquisition_date DATE NOT NULL,
    last_depreciation_date DATE,
    -- ... 생략
    INDEX idx_fixed_asset_depr (status, last_depreciation_date) -- 배치 성능을 위한 인덱스
);

-- 자산 이력 테이블 (Audit Trail)
CREATE TABLE asset_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    history_type VARCHAR(50) NOT NULL,
    old_dept_code VARCHAR(20),
    new_dept_code VARCHAR(20),
    event_at DATETIME NOT NULL,
    description TEXT,
    audit_user VARCHAR(50) NOT NULL,
    FOREIGN KEY (asset_id) REFERENCES fixed_assets(id),
    INDEX idx_asset_history_lookup (asset_id, event_at)
);
