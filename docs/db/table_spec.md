# K-Bank 차세대 재무 시스템 테이블 정의서

## 1. `ACCOUNT_SUBJECT` (계정과목)
| 컬럼명              | 데이터 타입   | 제약 조건          | 설명              |
|-------------------|-------------|--------------------|-------------------|
| `ACCOUNT_CODE`    | `VARCHAR2(20)` | `PK, NOT NULL`     | 계정코드          |
| `ACCOUNT_NAME`    | `VARCHAR2(100)`| `NOT NULL`         | 계정명            |
| `ACCOUNT_TYPE`    | `VARCHAR2(20)` | `NOT NULL`         | 계정유형 (자산, 부채, 자본, 수익, 비용) |
| `PARENT_ACCOUNT_CODE`| `VARCHAR2(20)` | `FK`               | 상위 계정코드     |
| `IS_USED`         | `CHAR(1)`   | `NOT NULL, DEFAULT 'Y'`| 사용여부 (Y/N)    |
| `CREATE_DATE`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`      | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_ACC_SUB_PARENT_ACC_CODE`: `PARENT_ACCOUNT_CODE` (계층 구조 조회 성능 향상)

## 2. `DEPARTMENT` (부서)
| 컬럼명              | 데이터 타입   | 제약 조건          | 설명              |
|-------------------|-------------|--------------------|-------------------|
| `DEPT_CODE`       | `VARCHAR2(20)` | `PK, NOT NULL`     | 부서코드          |
| `DEPT_NAME`       | `VARCHAR2(100)`| `NOT NULL`         | 부서명            |
| `PARENT_DEPT_CODE`| `VARCHAR2(20)` | `FK`               | 상위 부서코드     |
| `IS_USED`         | `CHAR(1)`   | `NOT NULL, DEFAULT 'Y'`| 사용여부 (Y/N)    |
| `CREATE_DATE`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`      | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_DEPT_PARENT_DEPT_CODE`: `PARENT_DEPT_CODE` (계층 구조 조회 성능 향상)

## 3. `CUSTOMER` (거래처)
| 컬럼명                 | 데이터 타입   | 제약 조건             | 설명              |
|----------------------|-------------|-----------------------|-------------------|
| `CUSTOMER_CODE`      | `VARCHAR2(20)` | `PK, NOT NULL`        | 거래처코드        |
| `CUSTOMER_NAME`      | `VARCHAR2(100)`| `NOT NULL`            | 거래처명          |
| `BUSINESS_REG_NO`    | `VARCHAR2(20)` | `UNIQUE NULL`         | 사업자등록번호    |
| `CONTACT_PERSON`     | `VARCHAR2(100)`| `NULL`                | 담당자            |
| `CONTACT_NO`         | `VARCHAR2(20)` | `NULL`                | 연락처            |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `UDX_CUSTOMER_BUSINESS_REG_NO`: `BUSINESS_REG_NO` (사업자등록번호 조회 성능 향상 및 고유성 보장)

## 4. `FIXED_ASSETS` (고정자산)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `ID`                    | `NUMBER`    | `PK, IDENTITY`              | 고정자산 ID       |
| `ASSET_CODE`            | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 자산코드          |
| `ASSET_NAME`            | `VARCHAR2(100)`| `NOT NULL`                  | 자산명            |
| `ACCOUNT_CODE`          | `VARCHAR2(20)` | `FK, NOT NULL`              | 자산 계정코드     |
| `ACCUMULATED_ACCOUNT_CODE`| `VARCHAR2(20)` | `FK`                        | 감가상각누계액 계정코드 |
| `EXPENSE_ACCOUNT_CODE`  | `VARCHAR2(20)` | `FK`                        | 감가상각비 계정코드 |
| `ACQUISITION_DATE`      | `DATE`      | `NOT NULL`                  | 취득일            |
| `ACQUISITION_COST`      | `NUMBER(19, 2)`| `NOT NULL`                  | 취득원가          |
| `USEFUL_LIFE`           | `NUMBER(3)` | `NOT NULL`                  | 내용연수 (년)     |
| `DEPRECIATION_METHOD`   | `VARCHAR2(20)` | `NULL`                      | 감가상각방법 (정액법, 정률법) |
| `RESIDUAL_VALUE`        | `NUMBER(19, 2)`| `NOT NULL, DEFAULT 0.00`    | 잔존가치          |
| `ACCUMULATED_DEPRECIATION`| `NUMBER(19, 2)`| `NOT NULL, DEFAULT 0.00`    | 현재까지의 감가상각누계액 |
| `DEPT_CODE`             | `VARCHAR2(20)` | `FK`                        | 관리 부서코드     |
| `STATUS`                | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'ACTIVE'`| 상태 (사용중, 처분, 상각완료) |
| `CREATE_DATE`           | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`           | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`            | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_FA_ASSET_CODE`: `ASSET_CODE` (자산코드 조회 성능 향상)
- `IDX_FA_ACCOUNT_CODE`: `ACCOUNT_CODE` (계정과목별 자산 조회 성능 향상)
- `IDX_FA_DEPT_CODE`: `DEPT_CODE` (부서별 자산 조회 성능 향상)

## 5. `LEASE_CONTRACTS` (리스 계약)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `ID`                    | `NUMBER`    | `PK, IDENTITY`              | 리스 계약 ID      |
| `CONTRACT_NO`           | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 계약번호          |
| `CONTRACT_NAME`         | `VARCHAR2(100)`| `NOT NULL`                  | 계약명            |
| `LESSOR_CUSTOMER_CODE`  | `VARCHAR2(20)` | `FK, NOT NULL`              | 리스 제공자 (거래처) 코드 |
| `START_DATE`            | `DATE`      | `NOT NULL`                  | 시작일            |
| `END_DATE`              | `DATE`      | `NOT NULL`                  | 종료일            |
| `MONTHLY_PAYMENT`       | `NUMBER(19, 2)`| `NOT NULL`                  | 월 리스료         |
| `PAYMENT_DAY`           | `NUMBER(2)` | `NOT NULL`                  | 매월 지급일       |
| `DEPT_CODE`             | `VARCHAR2(20)` | `FK`                        | 관리 부서코드     |
| `EXPENSE_ACCOUNT_CODE`  | `VARCHAR2(20)` | `FK`                        | 리스료 비용 계정코드 |
| `STATUS`                | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'ACTIVE'`| 상태 (ACTIVE, TERMINATED, EXPIRED) |
| `CREATE_DATE`           | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`           | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`            | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_LC_CONTRACT_NO`: `CONTRACT_NO` (계약번호 조회 성능 향상)
- `IDX_LC_LESSOR_CUSTOMER_CODE`: `LESSOR_CUSTOMER_CODE` (리스 제공자별 계약 조회 성능 향상)
- `IDX_LC_DEPT_CODE`: `DEPT_CODE` (부서별 계약 조회 성능 향상)
- `IDX_LC_EXPENSE_ACCOUNT_CODE`: `EXPENSE_ACCOUNT_CODE` (비용 계정별 계약 조회 성능 향상)

## 6. `UNSETTLED_ITEMS` (미지급금/미수금)
| 컬럼명                   | 데이터 타입   | 제약 조건                   | 설명              |
|------------------------|-------------|-----------------------------|-------------------|
| `ID`                     | `NUMBER`    | `PK, IDENTITY`              | 미지급/미수 항목 ID |
| `ITEM_NO`                | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 항목번호          |
| `CUSTOMER_CODE`          | `VARCHAR2(20)` | `FK, NOT NULL`              | 거래처코드        |
| `AMOUNT`                 | `NUMBER(19, 2)`| `NOT NULL`                  | 미지급/미수 금액  |
| `OCCURRENCE_DATE`        | `DATE`      | `NOT NULL`                  | 발생일            |
| `DUE_DATE`               | `DATE`      | `NOT NULL`                  | 예정일            |
| `STATUS`                 | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'PENDING'`| 처리상태 (PENDING, COMPLETED, OVERDUE) |
| `RELATED_JOURNAL_NO`     | `VARCHAR2(20)` | `FK`                        | 관련 전표번호     |
| `CREATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`             | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_UI_ITEM_NO`: `ITEM_NO` (항목번호 조회 성능 향상)
- `IDX_UI_CUSTOMER_CODE`: `CUSTOMER_CODE` (거래처별 미지급/미수 항목 조회 성능 향상)
- `IDX_UI_RELATED_JOURNAL_NO`: `RELATED_JOURNAL_NO` (관련 전표 조회 성능 향상)

## 7. `JOURNAL_ENTRY` (전표)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`                 | `NUMBER`    | `PK, IDENTITY`              | 전표 ID           |
| `JOURNAL_NO`         | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 전표번호          |
| `JOURNAL_DATE`       | `DATE`      | `NOT NULL`                  | 전표일자          |
| `TYPE`               | `VARCHAR2(20)` | `NOT NULL`                  | 유형 (GENERAL, CLOSING) |
| `DEPT_CODE`          | `VARCHAR2(20)` | `FK`                        | 생성 부서코드     |
| `CREATED_BY`         | `VARCHAR2(50)` | `NOT NULL`                  | 생성자            |
| `STATUS`             | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'DRAFT'` | 상태 (DRAFT, POSTED, CANCELED) |
| `TOTAL_DEBIT`        | `NUMBER(19, 2)`| `NOT NULL`                  | 차변 총액         |
| `TOTAL_CREDIT`       | `NUMBER(19, 2)`| `NOT NULL`                  | 대변 총액         |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_JE_JOURNAL_NO`: `JOURNAL_NO` (전표번호 조회 성능 향상)
- `IDX_JE_JOURNAL_DATE`: `JOURNAL_DATE` (전표일자별 조회 성능 향상)
- `IDX_JE_DEPT_CODE`: `DEPT_CODE` (부서별 전표 조회 성능 향상)

## 8. `JOURNAL_DETAIL` (전표 상세)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`                 | `NUMBER`    | `PK, IDENTITY`              | 전표 상세 ID      |
| `JOURNAL_NO`         | `VARCHAR2(20)` | `FK, NOT NULL`              | 전표번호          |
| `DETAIL_NO`          | `NUMBER(5)` | `NOT NULL`                  | 상세번호 (전표 내 순번) |
| `ACCOUNT_CODE`       | `VARCHAR2(20)` | `FK, NOT NULL`              | 계정코드          |
| `DEBIT_AMOUNT`       | `NUMBER(19, 2)`| `NOT NULL, DEFAULT 0.00`    | 차변 금액         |
| `CREDIT_AMOUNT`      | `NUMBER(19, 2)`| `NOT NULL, DEFAULT 0.00`    | 대변 금액         |
| `DESCRIPTION`        | `VARCHAR2(500)`| `NULL`                      | 적요              |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_JD_JOURNAL_NO`: `JOURNAL_NO` (상위 전표 조회 성능 향상)
- `IDX_JD_ACCOUNT_CODE`: `ACCOUNT_CODE` (계정과목별 상세 조회 성능 향상)

## 9. `BUDGET` (예산)
| 컬럼명              | 데이터 타입   | 제약 조건                   | 설명              |
|-------------------|-------------|-----------------------------|-------------------|
| `ID`                | `NUMBER`    | `PK, IDENTITY`              | 예산 ID           |
| `BUDGET_CODE`       | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 예산코드          |
| `BUDGET_NAME`       | `VARCHAR2(100)`| `NOT NULL`                  | 예산명            |
| `FISCAL_YEAR`       | `VARCHAR2(4)` | `NOT NULL`                  | 회계연도          |
| `DEPT_CODE`         | `VARCHAR2(20)` | `FK, NOT NULL`              | 부서코드          |
| `ACCOUNT_CODE`      | `VARCHAR2(20)` | `FK, NOT NULL`              | 계정코드          |
| `ALLOCATED_AMOUNT`  | `NUMBER(19, 2)`| `NOT NULL`                  | 할당금액          |
| `EXECUTED_AMOUNT`   | `NUMBER(19, 2)`| `NOT NULL, DEFAULT 0.00`    | 집행금액          |
| `REMAINING_AMOUNT`  | `NUMBER(19, 2)`| `NOT NULL`                  | 잔여금액          |
| `STATUS`            | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'ACTIVE'`| 상태 (ACTIVE, CLOSED) |
| `CREATE_DATE`       | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`       | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`        | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_BGT_BUDGET_CODE`: `BUDGET_CODE` (예산코드 조회 성능 향상)
- `IDX_BGT_FISCAL_YEAR`: `FISCAL_YEAR` (회계연도별 예산 조회 성능 향상)
- `IDX_BGT_DEPT_CODE`: `DEPT_CODE` (부서별 예산 조회 성능 향상)
- `IDX_BGT_ACCOUNT_CODE`: `ACCOUNT_CODE` (계정과목별 예산 조회 성능 향상)

## 10. `EXPENDITURE_RESOLUTION` (지출결의)
| 컬럼명                   | 데이터 타입   | 제약 조건                   | 설명              |
|------------------------|-------------|-----------------------------|-------------------|
| `ID`                     | `NUMBER`    | `PK, IDENTITY`              | 지출결의 ID       |
| `RESOLUTION_NO`          | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 결의번호          |
| `RESOLUTION_DATE`        | `DATE`      | `NOT NULL`                  | 결의일자          |
| `REQUESTING_DEPT_CODE`   | `VARCHAR2(20)` | `FK`                        | 신청 부서코드     |
| `REQUESTER`              | `VARCHAR2(50)` | `NOT NULL`                  | 신청자            |
| `TOTAL_AMOUNT`           | `NUMBER(19, 2)`| `NOT NULL`                  | 총금액            |
| `APPROVAL_STATUS`        | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'PENDING'`| 승인상태 (PENDING, APPROVED, REJECTED) |
| `CREATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`             | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_ER_RESOLUTION_NO`: `RESOLUTION_NO` (결의번호 조회 성능 향상)
- `IDX_ER_REQUESTING_DEPT_CODE`: `REQUESTING_DEPT_CODE` (신청 부서별 조회 성능 향상)

## 11. `EXPENDITURE_DETAIL` (지출 상세)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`                 | `NUMBER`    | `PK, IDENTITY`              | 지출 상세 ID      |
| `RESOLUTION_NO`      | `VARCHAR2(20)` | `FK, NOT NULL`              | 결의번호          |
| `DETAIL_NO`          | `NUMBER(5)` | `NOT NULL`                  | 상세번호 (결의 내 순번) |
| `ACCOUNT_CODE`       | `VARCHAR2(20)` | `FK, NOT NULL`              | 계정코드          |
| `AMOUNT`             | `NUMBER(19, 2)`| `NOT NULL`                  | 금액              |
| `DESCRIPTION`        | `VARCHAR2(500)`| `NULL`                      | 적요              |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_ED_RESOLUTION_NO`: `RESOLUTION_NO` (상위 결의 조회 성능 향상)
- `IDX_ED_ACCOUNT_CODE`: `ACCOUNT_CODE` (계정과목별 상세 조회 성능 향상)

## 12. `PAYMENT` (지급)
| 컬럼명                   | 데이터 타입   | 제약 조건                   | 설명              |
|------------------------|-------------|-----------------------------|-------------------|
| `ID`                     | `NUMBER`    | `PK, IDENTITY`              | 지급 ID           |
| `PAYMENT_NO`             | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 지급번호          |
| `PAYMENT_DATE`           | `DATE`      | `NOT NULL`                  | 지급일자          |
| `CUSTOMER_CODE`          | `VARCHAR2(20)` | `FK, NOT NULL`              | 거래처코드        |
| `PAYMENT_AMOUNT`         | `NUMBER(19, 2)`| `NOT NULL`                  | 지급금액          |
| `PAYMENT_METHOD`         | `VARCHAR2(50)` | `NOT NULL`                  | 지급수단          |
| `RELATED_RESOLUTION_NO`  | `VARCHAR2(20)` | `FK`                        | 관련 결의번호     |
| `CREATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`             | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_PMT_PAYMENT_NO`: `PAYMENT_NO` (지급번호 조회 성능 향상)
- `IDX_PMT_CUSTOMER_CODE`: `CUSTOMER_CODE` (거래처별 지급 조회 성능 향상)
- `IDX_PMT_RELATED_RESOLUTION_NO`: `RELATED_RESOLUTION_NO` (관련 결의 조회 성능 향상)

## 13. `DAILY_CLOSING_STATUS` (일별 마감 상태)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `CLOSING_DATE`       | `DATE`      | `PK, NOT NULL`              | 마감일자          |
| `STATUS`             | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'OPEN'`  | 상태 (OPEN, CLOSED, REOPENED) |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- 없음 (PK가 날짜이므로 단일 조회 및 범위 조회에 효율적)

## 14. `CLOSING_STATUS` (월별/연도별 마감 상태)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `CLOSING_YEAR_MONTH` | `VARCHAR2(6)` | `PK, NOT NULL`              | 마감연월 (YYYYMM) |
| `STATUS`             | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'OPEN'`  | 상태 (OPEN, CLOSED, REOPENED) |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- 없음 (PK가 연월이므로 단일 조회 및 범위 조회에 효율적)

## 15. `LOAN_CONTRACTS` (대출 계약)
| 컬럼명                      | 데이터 타입   | 제약 조건                   | 설명              |
|---------------------------|-------------|-----------------------------|-------------------|
| `ID`                        | `NUMBER`    | `PK, IDENTITY`              | 대출 계약 ID      |
| `LOAN_CONTRACT_NO`          | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 대출계약번호      |
| `CUSTOMER_CODE`             | `VARCHAR2(20)` | `FK, NOT NULL`              | 고객코드          |
| `LOAN_PRODUCT`              | `VARCHAR2(100)`| `NOT NULL`                  | 대출상품명        |
| `PRINCIPAL_AMOUNT`          | `NUMBER(19, 2)`| `NOT NULL`                  | 대출원금          |
| `DISBURSEMENT_DATE`         | `DATE`      | `NOT NULL`                  | 실행일            |
| `MATURITY_DATE`             | `DATE`      | `NOT NULL`                  | 만기일            |
| `INTEREST_RATE`             | `NUMBER(5, 4)` | `NOT NULL`                  | 이자율            |
| `REPAYMENT_METHOD`          | `VARCHAR2(50)` | `NOT NULL`                  | 상환방법          |
| `STATUS`                    | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'ACTIVE'`| 상태 (ACTIVE, PAID_OFF, DEFAULT) |
| `CURRENT_PRINCIPAL_BALANCE` | `NUMBER(19, 2)`| `NOT NULL`                  | 현재원금잔액      |
| `DEFERRED_LOAN_FEE`         | `NUMBER(19, 2)`| `NOT NULL, DEFAULT 0.00`    | 이연대출부대손익  |
| `EFFECTIVE_INTEREST_RATE`   | `NUMBER(5, 4)` | `NOT NULL`                  | 유효이자율 (EIR)  |
| `CREATE_DATE`               | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`               | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`                | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_LCN_LOAN_CONTRACT_NO`: `LOAN_CONTRACT_NO` (대출계약번호 조회 성능 향상)
- `IDX_LCN_CUSTOMER_CODE`: `CUSTOMER_CODE` (고객별 대출 조회 성능 향상)

## 16. `TAX_INVOICE` (세금계산서)
| 컬럼명                   | 데이터 타입   | 제약 조건                   | 설명              |
|------------------------|-------------|-----------------------------|-------------------|
| `ID`                     | `NUMBER`    | `PK, IDENTITY`              | 세금계산서 ID     |
| `INVOICE_NO`             | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 세금계산서번호    |
| `ISSUE_DATE`             | `DATE`      | `NOT NULL`                  | 발행일자          |
| `SUPPLIER_CUSTOMER_CODE` | `VARCHAR2(20)` | `FK, NOT NULL`              | 공급자 거래처코드 |
| `RECIPIENT_CUSTOMER_CODE`| `VARCHAR2(20)` | `FK, NOT NULL`              | 공급받는자 거래처코드 |
| `SUPPLY_AMOUNT`          | `NUMBER(19, 2)`| `NOT NULL`                  | 공급가액          |
| `TAX_AMOUNT`             | `NUMBER(19, 2)`| `NOT NULL`                  | 세액              |
| `TOTAL_AMOUNT`           | `NUMBER(19, 2)`| `NOT NULL`                  | 총액              |
| `TYPE`                   | `VARCHAR2(20)` | `NOT NULL`                  | 유형 (매입, 매출) |
| `STATUS`                 | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'ISSUED'`| 상태 (ISSUED, CANCELED) |
| `CREATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`             | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_TI_INVOICE_NO`: `INVOICE_NO` (세금계산서번호 조회 성능 향상)
- `IDX_TI_ISSUE_DATE`: `ISSUE_DATE` (발행일자별 조회 성능 향상)
- `IDX_TI_SUPPLIER_CUSTOMER_CODE`: `SUPPLIER_CUSTOMER_CODE` (공급자별 조회 성능 향상)
- `IDX_TI_RECIPIENT_CUSTOMER_CODE`: `RECIPIENT_CUSTOMER_CODE` (공급받는자별 조회 성능 향상)

## 16. `FINANCIAL_REPORT` (재무보고서)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`                 | `NUMBER`    | `PK, IDENTITY`              | 재무보고서 ID     |
| `REPORT_NO`          | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 보고서번호        |
| `REPORT_NAME`        | `VARCHAR2(100)`| `NOT NULL`                  | 보고서명          |
| `REPORT_TYPE`        | `VARCHAR2(50)` | `NOT NULL`                  | 보고서유형 (재무상태표, 손익계산서 등) |
| `BASE_DATE`          | `DATE`      | `NOT NULL`                  | 기준일자          |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_FR_REPORT_NO`: `REPORT_NO` (보고서번호 조회 성능 향상)
- `IDX_FR_REPORT_TYPE`: `REPORT_TYPE` (보고서유형별 조회 성능 향상)
- `IDX_FR_BASE_DATE`: `BASE_DATE` (기준일자별 조회 성능 향상)

## 17. `FINANCIAL_NOTE` (재무보고서 주석)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`                 | `NUMBER`    | `PK, IDENTITY`              | 주석 ID           |
| `REPORT_NO`          | `VARCHAR2(20)` | `FK, NOT NULL`              | 보고서번호        |
| `NOTE_NO`            | `NUMBER(5)` | `NOT NULL`                  | 주석번호 (보고서 내 순번) |
| `NOTE_CONTENT`       | `CLOB`      | `NOT NULL`                  | 주석내용          |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_FN_REPORT_NO`: `REPORT_NO` (상위 보고서 조회 성능 향상)