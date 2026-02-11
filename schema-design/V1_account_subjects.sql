DROP TABLE IF EXISTS account_subjects;

CREATE TABLE account_subjects (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_code VARCHAR(20),
    category VARCHAR(30),
    balance_type VARCHAR(10) NOT NULL,
    report_line VARCHAR(100),
    unsettled BOOLEAN NOT NULL DEFAULT FALSE,
    fixed_asset BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    FOREIGN KEY (parent_code) REFERENCES account_subjects(code)
);

COMMENT ON TABLE account_subjects IS '계정과목 마스터 테이블';
COMMENT ON COLUMN account_subjects.code IS '계정 코드 (PK)';
COMMENT ON COLUMN account_subjects.name IS '계정명';
COMMENT ON COLUMN account_subjects.parent_code IS '상위 계정 코드 (계층 구조)';
COMMENT ON COLUMN account_subjects.category IS '계정 대분류 (ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES)';
COMMENT ON COLUMN account_subjects.balance_type IS '계정 잔액 타입 (DEBIT, CREDIT)';
COMMENT ON COLUMN account_subjects.report_line IS '재무제표 표시 라인';
COMMENT ON COLUMN account_subjects.unsettled IS '미결(채권/채무) 관리 여부';
COMMENT ON COLUMN account_subjects.fixed_asset IS '고정자산 계정 여부';
COMMENT ON COLUMN account_subjects.valid_from IS '유효 시작일 (SCD2)';
COMMENT ON COLUMN account_subjects.valid_to IS '유효 종료일 (SCD2)';
