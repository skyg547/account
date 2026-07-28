CREATE TABLE IF NOT EXISTS ods_apart_coll_detail (
    coll_id VARCHAR(50) PRIMARY KEY,
    district_cd VARCHAR(10),
    kb_market_price NUMERIC(19,4),
    house_type VARCHAR(20),
    exclusive_area NUMERIC(10,2),
    floor_no INTEGER,
    is_speculative_area BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_ods_apart_coll_detail_district
    ON ods_apart_coll_detail(district_cd);
