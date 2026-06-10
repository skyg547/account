-- 신규 설치에서는 Hibernate가 이 테이블을 생성하고, 기존 설치에서는 아래 컬럼만 보강합니다.
ALTER TABLE IF EXISTS unsettled_items ADD COLUMN IF NOT EXISTS last_settled_by VARCHAR(50);
ALTER TABLE IF EXISTS unsettled_items ADD COLUMN IF NOT EXISTS last_settlement_reference VARCHAR(100);
ALTER TABLE IF EXISTS unsettled_items ADD COLUMN IF NOT EXISTS last_settled_at TIMESTAMP;
