# 차세대 재무 시스템 테이블 정의서

## 1. `ACCOUNT_SUBJECT` (계정과목)
| 컬럼명              | 데이터 타입   | 제약 조건          | 설명              |
|-------------------|-------------|--------------------|-------------------|
| `ACCOUNT_CODE`    | `VARCHAR2(20)` | `PK, NOT NULL`     | 계정코드          |
| `ACCOUNT_NAME`    | `VARCHAR2(100)`| `NOT NULL`         | 계정명            |
| `ACCOUNT_TYPE`    | `VARCHAR2(20)` | `NOT NULL`         | 계정유형 (자산, 부채, 자본, 수익, 비용) |
| `PARENT_ACCOUNT_CODE`| `VARCHAR2(20)` | `FK`               | 상위 계정코드     |
| `VALID_FROM_DATE` | `DATE`      | `NOT NULL`         | 유효 시작일 (SCD2) |
| `VALID_TO_DATE`   | `DATE`      | `NULL`             | 유효 종료일 (SCD2) |
| `IS_USED`         | `CHAR(1)`   | `NOT NULL, DEFAULT 'Y'`| 사용여부 (Y/N)    |
| `CREATE_DATE`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`      | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_ACC_SUB_PARENT_ACC_CODE`: `PARENT_ACCOUNT_CODE` (계층 구조 조회 성능 향상)
- `IDX_ACC_SUB_VALIDITY`: `VALID_FROM_DATE`, `VALID_TO_DATE` (유효 기간별 조회 성능 향상)

## 2. `DEPARTMENT` (부서)
| 컬럼명              | 데이터 타입   | 제약 조건          | 설명              |
|-------------------|-------------|--------------------|-------------------|
| `DEPT_CODE`       | `VARCHAR2(20)` | `PK, NOT NULL`     | 부서코드          |
| `DEPT_NAME`       | `VARCHAR2(100)`| `NOT NULL`         | 부서명            |
| `PARENT_DEPT_CODE`| `VARCHAR2(20)` | `FK`               | 상위 부서코드     |
| `VALID_FROM_DATE` | `DATE`      | `NOT NULL`         | 유효 시작일 (SCD2) |
| `VALID_TO_DATE`   | `DATE`      | `NULL`             | 유효 종료일 (SCD2) |
| `IS_USED`         | `CHAR(1)`   | `NOT NULL, DEFAULT 'Y'`| 사용여부 (Y/N)    |
| `CREATE_DATE`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`      | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_DEPT_PARENT_DEPT_CODE`: `PARENT_DEPT_CODE` (계층 구조 조회 성능 향상)
- `IDX_DEPT_VALIDITY`: `VALID_FROM_DATE`, `VALID_TO_DATE` (유효 기간별 조회 성능 향상)

## 3. `BUSINESS_PARTNERS` (거래처)
| 컬럼명                 | 데이터 타입   | 제약 조건             | 설명              |
|----------------------|-------------|-----------------------|-------------------|
| `CUSTOMER_CODE`      | `VARCHAR2(20)` | `PK, NOT NULL`        | 거래처코드        |
| `CUSTOMER_NAME`      | `VARCHAR2(100)`| `NOT NULL`            | 거래처명          |
| `BUSINESS_REG_NO`    | `VARCHAR2(20)` | `UNIQUE NULL`         | 사업자등록번호    |
| `CONTACT_PERSON`     | `VARCHAR2(100)`| `NULL`                | 담당자            |
| `CONTACT_NO`         | `VARCHAR2(20)` | `NULL`                | 연락처            |
| `CUSTOMER_ACCOUNT_NO`| `VARCHAR2(100)`| `NULL`                | 거래처 계좌번호     |
| `KYC_STATUS`         | `VARCHAR2(20)` | `DEFAULT 'PENDING'`   | KYC 상태          |
| `VALID_FROM_DATE`    | `DATE`      | `NOT NULL`            | 유효 시작일 (SCD2) |
| `VALID_TO_DATE`      | `DATE`      | `NULL`                | 유효 종료일 (SCD2) |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `UDX_BP_BUSINESS_REG_NO`: `BUSINESS_REG_NO` (사업자등록번호 조회 성능 향상 및 고유성 보장)
- `IDX_BP_VALIDITY`: `VALID_FROM_DATE`, `VALID_TO_DATE` (유효 기간별 조회 성능 향상)

## 4. `PRODUCT_MASTER` (상품 마스터)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `PRODUCT_CODE`        | `VARCHAR2(20)` | `PK, NOT NULL`              | 상품 코드         |
| `PRODUCT_NAME`        | `VARCHAR2(100)`| `NOT NULL`                  | 상품명            |
| `PRODUCT_GROUP`       | `VARCHAR2(50)` | `NOT NULL`                  | 상품군 (예: 대출, 예금, 파생) |
| `ACCOUNT_CLASS_CODE`  | `VARCHAR2(20)` | `FK, NOT NULL`              | 회계 분류 계정 (상품군별 회계분류) |
| `VALID_FROM_DATE`     | `DATE`      | `NOT NULL`                  | 유효 시작일         |
| `VALID_TO_DATE`       | `DATE`      | `NULL`                      | 유효 종료일         |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_PM_PRODUCT_GROUP`: `PRODUCT_GROUP` (상품군별 조회 성능 향상)
- `IDX_PM_VALIDITY`: `VALID_FROM_DATE`, `VALID_TO_DATE` (유효 기간별 조회 성능 향상)

## 5. `CURRENCY` (통화)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `CURRENCY_CODE`       | `VARCHAR2(3)` | `PK, NOT NULL`              | 통화 코드 (ISO 4217) |
| `CURRENCY_NAME`       | `VARCHAR2(50)` | `NOT NULL`                  | 통화명            |
| `SYMBOL`              | `VARCHAR2(10)` | `NULL`                      | 통화 심볼 (예: ₩, $) |
| `VALID_FROM_DATE`     | `DATE`      | `NOT NULL`                  | 유효 시작일         |
| `VALID_TO_DATE`       | `DATE`      | `NULL`                      | 유효 종료일         |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_CURRENCY_VALIDITY`: `VALID_FROM_DATE`, `VALID_TO_DATE` (유효 기간별 조회 성능 향상)

## 6. `EXCHANGE_RATE` (환율)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `BASE_CURRENCY_CODE`  | `VARCHAR2(3)` | `PK, FK, NOT NULL`          | 기준 통화 코드    |
| `TARGET_CURRENCY_CODE`| `VARCHAR2(3)` | `PK, FK, NOT NULL`          | 대상 통화 코드    |
| `APPLY_DATE`          | `DATE`      | `PK, NOT NULL`              | 적용 일자         |
| `RATE`                | `NUMBER(19, 8)`| `NOT NULL`                  | 환율 (소수점 8자리) |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `UDX_EXCH_RATE_UNIQUE`: `BASE_CURRENCY_CODE`, `TARGET_CURRENCY_CODE`, `APPLY_DATE` (환율 고유성)

## 7. `FISCAL_CALENDAR` (회계 캘린더)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `FISCAL_YEAR`         | `VARCHAR2(4)` | `PK, NOT NULL`              | 회계 연도         |
| `FISCAL_PERIOD`       | `VARCHAR2(2)` | `PK, NOT NULL`              | 회계 기간 (월)    |
| `START_DATE`          | `DATE`      | `NOT NULL`                  | 기간 시작일         |
| `END_DATE`            | `DATE`      | `NOT NULL`                  | 기간 종료일         |
| `CLOSING_STATUS`      | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'OPEN'` | 마감 상태 (OPEN, CLOSED) |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `UDX_FISCAL_CAL_UNIQUE`: `FISCAL_YEAR`, `FISCAL_PERIOD` (회계 기간 고유성)

## 8. `TAX_MASTER` (세무 마스터)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `TAX_CODE`            | `VARCHAR2(20)` | `PK, NOT NULL`              | 세목 코드         |
| `TAX_NAME`            | `VARCHAR2(100)`| `NOT NULL`                  | 세목명            |
| `TAX_TYPE`            | `VARCHAR2(20)` | `NOT NULL`                  | 세금 유형 (예: 부가세, 원천세) |
| `RATE`                | `NUMBER(5, 4)` | `NOT NULL`                  | 세율 (소수점 4자리) |
| `APPLY_START_DATE`    | `DATE`      | `NOT NULL`                  | 적용 시작일         |
| `APPLY_END_DATE`      | `DATE`      | `NULL`                      | 적용 종료일         |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_TAX_MASTER_APPLY_DATE`: `APPLY_START_DATE`, `APPLY_END_DATE` (세율 적용 기간 조회 성능 향상)

## 9. `MASTER_APPROVAL` (마스터 승인 이력)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `APPROVAL_ID`         | `NUMBER`    | `PK, IDENTITY`              | 승인 ID           |
| `MASTER_TYPE`         | `VARCHAR2(50)` | `NOT NULL`                  | 마스터 유형 (예: ACCOUNT_SUBJECT, PRODUCT_MASTER) |
| `MASTER_KEY`          | `VARCHAR2(100)`| `NOT NULL`                  | 마스터 고유 키 (예: ACCOUNT_CODE) |
| `CHANGE_REQUEST_TYPE` | `VARCHAR2(20)` | `NOT NULL`                  | 변경 요청 유형 (CREATE, UPDATE, DELETE) |
| `REQUEST_USER`        | `VARCHAR2(50)` | `NOT NULL`                  | 요청 사용자         |
| `REQUEST_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 요청일            |
| `APPROVER_USER`       | `VARCHAR2(50)` | `NULL`                      | 승인 사용자         |
| `APPROVAL_DATE`       | `TIMESTAMP` | `NULL`                      | 승인일            |
| `APPROVAL_STATUS`     | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'PENDING'`| 승인 상태 (PENDING, APPROVED, REJECTED) |
| `REMARKS`             | `VARCHAR2(500)`| `NULL`                      | 비고              |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_MA_MASTER_KEY`: `MASTER_TYPE`, `MASTER_KEY` (마스터별 승인 이력 조회 성능 향상)
- `IDX_MA_REQUEST_USER`: `REQUEST_USER` (요청 사용자별 승인 이력 조회 성능 향상)

---

## 기존 테이블들 (마스터/기준정보와 연관성 고려)

## 10. `FIXED_ASSETS` (고정자산)
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

## 11. `LEASE_CONTRACTS` (리스 계약)
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

## 12. `UNSETTLED_ITEMS` (미지급금/미수금)
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

## 13. `JOURNAL_ENTRY` (전표)
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

## 14. `JOURNAL_DETAIL` (전표 상세)
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

## 15. `BUDGET` (예산)
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

## 16. `EXPENDITURE_RESOLUTION` (지출결의)
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

## 17. `EXPENDITURE_DETAIL` (지출 상세)
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

## 18. `PAYMENT` (지급)
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

## 19. `DAILY_CLOSING_STATUS` (일별 마감 상태)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `CLOSING_DATE`       | `DATE`      | `PK, NOT NULL`              | 마감일자          |
| `STATUS`             | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'OPEN'`  | 상태 (OPEN, CLOSED, REOPENED) |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- 없음 (PK가 날짜이므로 단일 조회 및 범위 조회에 효율적)

## 20. `CLOSING_STATUS` (월별/연도별 마감 상태)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `CLOSING_YEAR_MONTH` | `VARCHAR2(6)` | `PK, NOT NULL`              | 마감연월 (YYYYMM) |
| `STATUS`             | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'OPEN'`  | 상태 (OPEN, CLOSED, REOPENED) |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- 없음 (PK가 연월이므로 단일 조회 및 범위 조회에 효율적)

## 21. `LOAN_CONTRACTS` (대출 계약)
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

## 22. `TAX_INVOICE` (세금계산서)
| 컬럼명                   | 데이터 타입   | 제약 조건                   | 설명              |
|------------------------|-------------|-----------------------------|-------------------|
| `ID`                     | `NUMBER`    | `PK, IDENTITY`              | 세금계산서 ID     |
| `INVOICE_NO`             | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 세금계산서번호    |
| `ISSUE_DATE`             | `DATE`      | `NOT NULL`                  | 발행일자          |
| `SUPPLIER_CUSTOMER_CODE` | `VARCHAR2(20)` | `FK, NOT NULL`              | 공급자 거래처코드 |
| `RECIPIENT_CUSTOMER_CODE`| `VARCHAR2(20)` | `FK, NOT NULL`              | 공급받는자 거래처코드 |
| `SUPPLY_AMOUNT`          | `NUMBER(19, 2)`| `NOT NULL`                  | 공급가액          |
| `TAX_AMOUNT`             | `NUMBER(19, 2)`| `NOT NULL`                  | 세액              |
| `TOTAL_AMOUNT`           | `NUMBER(19, 2)`| `NOT NULL`                  | 총액 (공급가액 + 세액) |
| `TYPE`                   | `VARCHAR2(20)` | `NOT NULL`                  | 유형 (PURCHASE, SALES) |
| `STATUS`                 | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'ISSUED'`| 상태 (ISSUED, CANCELED) |
| `CREATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`            | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`             | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_TI_INVOICE_NO`: `INVOICE_NO` (세금계산서번호 조회 성능 향상)
- `IDX_TI_ISSUE_DATE`: `ISSUE_DATE` (발행일자별 조회 성능 향상)
- `IDX_TI_SUPPLIER_CUSTOMER_CODE`: `SUPPLIER_CUSTOMER_CODE` (공급자별 조회 성능 향상)
- `IDX_TI_RECIPIENT_CUSTOMER_CODE`: `RECIPIENT_CUSTOMER_CODE` (공급받는자별 조회 성능 향상)

## 23. `FINANCIAL_REPORT` (재무보고서)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`                 | `NUMBER`    | `PK, IDENTITY`              | 재무보고서 ID     |
| `REPORT_NO`          | `VARCHAR2(20)` | `UNIQUE, NOT NULL`          | 보고서번호        |
| `REPORT_NAME`        | `VARCHAR2(100)`| `NOT NULL`                  | 보고서명            |
| `REPORT_TYPE`        | `VARCHAR2(50)` | `NOT NULL`                  | 보고서유형 (FS, IS, CFS) |
| `BASE_DATE`          | `DATE`      | `NOT NULL`                  | 기준일자          |
| `CREATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`        | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`         | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_FR_REPORT_NO`: `REPORT_NO` (보고서번호 조회 성능 향상)
- `IDX_FR_REPORT_TYPE`: `REPORT_TYPE` (보고서유형별 조회 성능 향상)
- `IDX_FR_BASE_DATE`: `BASE_DATE` (기준일자별 조회 성능 향상)

## 24. `FINANCIAL_NOTE` (재무보고서 주석)
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

## 25. `RULE_MASTER` (룰 마스터)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `RULE_ID`             | `VARCHAR2(20)` | `PK, NOT NULL`              | 룰 ID             |
| `RULE_NAME`           | `VARCHAR2(100)`| `NOT NULL`                  | 룰명              |
| `RULE_TYPE`           | `VARCHAR2(20)` | `NOT NULL`                  | 룰 유형 (예: 분개룰, 검증룰, 상각룰) |
| `DESCRIPTION`         | `VARCHAR2(500)`| `NULL`                      | 룰 설명           |
| `PRIORITY`            | `NUMBER(3)` | `NOT NULL, DEFAULT 100`     | 룰 우선순위 (낮은 숫자 = 높은 우선순위) |
| `APPLY_START_DATE`    | `DATE`      | `NOT NULL`                  | 적용 시작일         |
| `APPLY_END_DATE`      | `DATE`      | `NULL`                      | 적용 종료일         |
| `APPROVAL_STATUS`     | `VARCHAR2(20)` | `NOT NULL, DEFAULT 'PENDING'`| 승인 상태 (PENDING, APPROVED, REJECTED) |
| `VERSION`             | `NUMBER(5)` | `NOT NULL, DEFAULT 1`       | 룰 버전           |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_RM_RULE_TYPE`: `RULE_TYPE` (룰 유형별 조회 성능 향상)
- `IDX_RM_APPLY_DATE`: `APPLY_START_DATE`, `APPLY_END_DATE` (룰 적용 기간 조회 성능 향상)

## 26. `RULE_CONDITION` (룰 조건)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `CONDITION_ID`        | `NUMBER`    | `PK, IDENTITY`              | 조건 ID           |
| `RULE_ID`             | `VARCHAR2(20)` | `FK, NOT NULL`              | 룰 ID             |
| `SEQUENCE`            | `NUMBER(3)` | `NOT NULL`                  | 조건 순번         |
| `FIELD_NAME`          | `VARCHAR2(100)`| `NOT NULL`                  | 필드명 (예: TRANSACTION_TYPE, CUSTOMER_GROUP) |
| `OPERATOR`            | `VARCHAR2(20)` | `NOT NULL`                  | 연산자 (예: EQ, NE, GT, LT, LIKE) |
| `VALUE`               | `VARCHAR2(500)`| `NOT NULL`                  | 비교 값           |
| `LOGICAL_OPERATOR`    | `VARCHAR2(10)` | `NULL`                      | 논리 연산자 (AND, OR) |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `UDX_RC_UNIQUE`: `RULE_ID`, `SEQUENCE` (룰 내 조건 순번 고유성)

## 27. `RULE_ACTION` (룰 액션)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `ACTION_ID`           | `NUMBER`    | `PK, IDENTITY`              | 액션 ID           |
| `RULE_ID`             | `VARCHAR2(20)` | `FK, NOT NULL`              | 룰 ID             |
| `SEQUENCE`            | `NUMBER(3)` | `NOT NULL`                  | 액션 순번         |
| `ACTION_TYPE`         | `VARCHAR2(50)` | `NOT NULL`                  | 액션 유형 (예: 분개 생성, 필드 값 설정, 에러 발생) |
| `TARGET_FIELD_NAME`   | `VARCHAR2(100)`| `NULL`                      | 대상 필드명 (액션 유형에 따라) |
| `VALUE_EXPRESSION`    | `VARCHAR2(500)`| `NULL`                      | 값 또는 표현식 (예: 계정코드, 금액 산식) |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `UDX_RA_UNIQUE`: `RULE_ID`, `SEQUENCE` (룰 내 액션 순번 고유성)

## 28. `RULE_APPLICATION_LOG` (룰 적용 이력)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `LOG_ID`              | `NUMBER`    | `PK, IDENTITY`              | 로그 ID           |
| `RULE_ID`             | `VARCHAR2(20)` | `FK, NOT NULL`              | 적용된 룰 ID      |
| `RULE_VERSION`        | `NUMBER(5)` | `NOT NULL`                  | 적용된 룰 버전    |
| `APPLY_DATE_TIME`     | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 적용 일시         |
| `SOURCE_TRANSACTION_ID`| `VARCHAR2(100)`| `NOT NULL`                  | 원천 거래 ID      |
| `RESULT_CODE`         | `VARCHAR2(20)` | `NOT NULL`                  | 결과 코드 (SUCCESS, FAILED, NO_MATCH) |
| `RESULT_MESSAGE`      | `VARCHAR2(500)`| `NULL`                      | 결과 메시지         |
| `GENERATED_JOURNAL_NO`| `VARCHAR2(20)` | `FK`                        | 생성된 전표 번호 (분개룰의 경우) |
| `CREATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`         | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`          | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

### 인덱스 전략
- `IDX_RAL_RULE_ID`: `RULE_ID` (룰별 적용 이력 조회 성능 향상)
- `IDX_RAL_SOURCE_TRAN_ID`: `SOURCE_TRANSACTION_ID` (원천 거래별 적용 이력 조회 성능 향상)
- `IDX_RAL_GENERATED_JE`: `GENERATED_JOURNAL_NO` (생성된 전표별 적용 이력 조회 성능 향상)

---

## 29. `BANK_STATEMENTS` (은행 거래 내역)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `ID`                  | `NUMBER`    | `PK, IDENTITY`              | ID                |
| `BANK_CODE`           | `VARCHAR2(20)`| `NOT NULL`                  | 은행 코드          |
| `ACCOUNT_NO`          | `VARCHAR2(50)`| `NOT NULL`                  | 계좌 번호          |
| `TRANSACTION_DATE`    | `DATE`      | `NOT NULL`                  | 거래 일자          |
| `DESCRIPTION`         | `VARCHAR2(500)`| `NULL`                      | 거래 내용 (적요)    |
| `WITHDRAWAL_AMOUNT`   | `NUMBER(19, 2)`| `NOT NULL, DEFAULT 0.00`    | 출금 금액          |
| `DEPOSIT_AMOUNT`      | `NUMBER(19, 2)`| `NOT NULL, DEFAULT 0.00`    | 입금 금액          |
| `RECONCILIATION_STATUS`| `VARCHAR2(20)`| `NOT NULL, DEFAULT 'UNMATCHED'`| 대사 상태         |

## 30. `LOAN_AMORTIZATION_SCHEDULE_ENTRIES` (대출 상각 스케줄 항목)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `ID`                  | `NUMBER`    | `PK, IDENTITY`              | ID                |
| `LOAN_CONTRACT_ID`    | `NUMBER`    | `FK, NOT NULL`              | 대출 계약 ID       |
| `PAYMENT_DATE`        | `DATE`      | `NOT NULL`                  | 납입 예정일         |
| `PERIOD_NUMBER`       | `NUMBER(5)` | `NOT NULL`                  | 회차               |
| `INTEREST_AMOUNT`     | `NUMBER(19, 2)`| `NOT NULL`                  | 이자 금액          |
| `PRINCIPAL_AMOUNT`    | `NUMBER(19, 2)`| `NOT NULL`                  | 원금 상환액         |
| `ENDING_BALANCE`      | `NUMBER(19, 2)`| `NOT NULL`                  | 기말 잔액          |
| `ENTRY_TYPE`          | `VARCHAR2(50)`| `NULL`                      | 항목 유형          |

## 31. `LOAN_ACCRUAL_LOG` (대출 이자 발생 로그)
| 컬럼명                  | 데이터 타입   | 제약 조건                   | 설명              |
|-----------------------|-------------|-----------------------------|-------------------|
| `ID`                  | `NUMBER`    | `PK, IDENTITY`              | ID                |
| `ACCRUAL_DATE`        | `DATE`      | `NOT NULL`                  | 발생 일자          |
| `LOAN_CONTRACT_ID`    | `NUMBER`    | `FK, NOT NULL`              | 대출 계약 ID       |
| `ACCRUED_AMOUNT`      | `NUMBER(19, 2)`| `NOT NULL`                  | 발생 이자 금액      |
| `JOURNAL_NO`          | `VARCHAR2(20)`| `NULL`                      | 관련 전표 번호      |
| `STATUS`              | `VARCHAR2(20)`| `NOT NULL`                  | 상태 (SUCCESS, FAILED)|

---

## 32. `SYSTEM_USERS` (사용자)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `USER_ID`          | `VARCHAR2(50)` | `PK, NOT NULL`              | 사용자 ID         |
| `USER_NAME`        | `VARCHAR2(100)`| `NOT NULL`                  | 사용자명          |
| `PASSWORD`         | `VARCHAR2(255)`| `NULL`                      | 비밀번호 (암호화)  |
| `DEPT_CODE`        | `VARCHAR2(20)` | `FK`                        | 부서코드          |
| `EMAIL`            | `VARCHAR2(100)`| `NULL`                      | 이메일            |
| `STATUS`           | `VARCHAR2(20)` | `DEFAULT 'ACTIVE'`          | 상태 (ACTIVE, INACTIVE) |
| `IS_LOCKED`        | `CHAR(1)`   | `DEFAULT 'N'`               | 잠금 여부 (Y/N)    |
| `LAST_LOGIN_DATE`  | `TIMESTAMP` | `NULL`                      | 최종 로그인 일시   |
| `CREATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`       | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

## 33. `SYSTEM_ROLES` (역할)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ROLE_CODE`        | `VARCHAR2(20)` | `PK, NOT NULL`              | 역할 코드         |
| `ROLE_NAME`        | `VARCHAR2(100)`| `NOT NULL`                  | 역할명            |
| `DESCRIPTION`      | `VARCHAR2(500)`| `NULL`                      | 설명              |
| `IS_USED`          | `CHAR(1)`   | `NOT NULL, DEFAULT 'Y'`     | 사용여부 (Y/N)    |

## 34. `USER_ROLES` (사용자-역할)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `USER_ID`          | `VARCHAR2(50)` | `PK, FK, NOT NULL`          | 사용자 ID         |
| `ROLE_CODE`        | `VARCHAR2(20)` | `PK, FK, NOT NULL`          | 역할 코드         |

## 35. `SYSTEM_FUNCTIONS` (기능 목록)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `FUNC_CODE`        | `VARCHAR2(50)` | `PK, NOT NULL`              | 기능 코드         |
| `FUNC_NAME`        | `VARCHAR2(100)`| `NOT NULL`                  | 기능명            |
| `FUNC_TYPE`        | `VARCHAR2(20)` | `NULL`                      | 기능 유형 (MENU, BTN)|

## 36. `ROLE_FUNC_PERMISSIONS` (역할별 기능 권한)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ROLE_CODE`        | `VARCHAR2(20)` | `PK, FK, NOT NULL`          | 역할 코드         |
| `FUNC_CODE`        | `VARCHAR2(50)` | `PK, FK, NOT NULL`          | 기능 코드         |
| `CAN_READ`         | `CHAR(1)`   | `DEFAULT 'Y'`               | 조회 권한 (Y/N)    |
| `CAN_WRITE`        | `CHAR(1)`   | `DEFAULT 'N'`               | 작성 권한 (Y/N)    |
| `CAN_APPROVE`      | `CHAR(1)`   | `DEFAULT 'N'`               | 승인 권한 (Y/N)    |

## 37. `AUDIT_LOG` (중요 이벤트 로그)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`               | `NUMBER`    | `PK, IDENTITY`              | 로그 ID           |
| `EVENT_TYPE`       | `VARCHAR2(50)` | `NOT NULL`                  | 이벤트 유형 (LOGIN, MASTER, SLIP)|
| `EVENT_NAME`       | `VARCHAR2(200)`| `NULL`                      | 이벤트명          |
| `TARGET_TABLE`     | `VARCHAR2(50)` | `NULL`                      | 대상 테이블       |
| `TARGET_ID`        | `VARCHAR2(100)`| `NULL`                      | 대상 PK           |
| `USER_ID`          | `VARCHAR2(50)` | `NULL`                      | 수행 사용자 ID    |
| `BEFORE_DATA`      | `CLOB`      | `NULL`                      | 변경 전 데이터 (JSON)|
| `AFTER_DATA`       | `CLOB`      | `NULL`                      | 변경 후 데이터 (JSON)|
| `CREATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 발생 일시         |

## 38. `DATA_MASKING_POLICY` (마스킹 정책)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `POLICY_ID`        | `NUMBER`    | `PK, IDENTITY`              | 정책 ID           |
| `TARGET_TABLE`     | `VARCHAR2(50)` | `NOT NULL`                  | 대상 테이블       |
| `TARGET_COLUMN`    | `VARCHAR2(50)` | `NOT NULL`                  | 대상 컬럼         |
| `MASKING_PATTERN`  | `VARCHAR2(100)`| `NULL`                      | 마스킹 패턴 (예: 3-2-5)|
| `ROLE_CODE`        | `VARCHAR2(20)` | `NULL`                      | 특정 역할용 (NULL일 경우 공통)|
## 39. `RPT_LINE_MAPPING` (보고서 라인 매핑)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`               | `NUMBER`    | `PK, IDENTITY`              | ID                |
| `REPORT_TYPE`      | `VARCHAR2(50)` | `NOT NULL`                  | 보고서 유형 (BS, IS, CF, REGULATORY) |
| `LINE_CODE`        | `VARCHAR2(50)` | `NOT NULL`                  | 보고 라인 코드    |
| `LINE_NAME`        | `VARCHAR2(200)`| `NOT NULL`                  | 보고 라인 명      |
| `ACCOUNT_CODE`     | `VARCHAR2(20)` | `NULL`                      | 계정코드 (Leaf node) |
| `AGGREGATION_TYPE` | `VARCHAR2(20)` | `NOT NULL`                  | 집계 유형 (SUM, FORMULA) |
| `FORMULA_EXPRESSION`| `VARCHAR2(500)`| `NULL`                      | 계산 공식         |
| `DISPLAY_ORDER`    | `NUMBER(5)` | `NOT NULL`                  | 출력 순서         |
| `PARENT_LINE_CODE` | `VARCHAR2(50)` | `NULL`                      | 상위 라인 코드    |
| `VERSION`          | `NUMBER(5)` | `DEFAULT 1, NOT NULL`       | 버전              |
| `VALID_FROM_DATE`  | `DATE`      | `NOT NULL`                  | 유효 시작일       |
| `VALID_TO_DATE`    | `DATE`      | `DEFAULT '99991231'`        | 유효 종료일       |
| `CREATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `UPDATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 수정일            |
| `AUDIT_USER`       | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

## 40. `RPT_SNAPSHOT_HEADER` (보고서 스냅샷 헤더)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `SNAPSHOT_ID`      | `NUMBER`    | `PK, IDENTITY`              | 스냅샷 ID         |
| `REPORT_TYPE`      | `VARCHAR2(50)` | `NOT NULL`                  | 보고서 유형       |
| `BASE_DATE`        | `DATE`      | `NOT NULL`                  | 기준 일자         |
| `VERSION`          | `NUMBER(5)` | `NOT NULL`                  | 제출/저장 버전    |
| `STATUS`           | `VARCHAR2(20)` | `NOT NULL`                  | 상태 (DRAFT, FINAL, SUBMITTED) |
| `DESCRIPTION`      | `VARCHAR2(500)`| `NULL`                      | 비고              |
| `CREATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `AUDIT_USER`       | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

## 41. `RPT_SNAPSHOT_DETAIL` (보고서 스냅샷 상세)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`               | `NUMBER`    | `PK, IDENTITY`              | 상세 ID           |
| `SNAPSHOT_ID`      | `NUMBER`    | `FK, NOT NULL`              | 스냅샷 ID         |
| `LINE_CODE`        | `VARCHAR2(50)` | `NOT NULL`                  | 보고 라인 코드    |
| `AMOUNT`           | `NUMBER(19, 2)`| `NOT NULL`                  | 금액              |
| `CREATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `AUDIT_USER`       | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

## 42. `DISCLOSURE_MART` (공시 마트)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`               | `NUMBER`    | `PK, IDENTITY`              | 마트 데이터 ID     |
| `MART_TYPE`        | `VARCHAR2(50)` | `NOT NULL`                  | 마트 유형 (MATURITY, INTEREST, CURRENCY) |
| `BASE_DATE`        | `DATE`      | `NOT NULL`                  | 기준 일자         |
| `CATEGORY_1`       | `VARCHAR2(100)`| `NULL`                      | 대분류            |
| `CATEGORY_2`       | `VARCHAR2(100)`| `NULL`                      | 중분류            |
| `AMOUNT`           | `NUMBER(19, 2)`| `NOT NULL`                  | 금액              |
| `COUNT`            | `NUMBER(10)` | `NULL`                      | 건수              |
| `CREATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `AUDIT_USER`       | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

## 43. `REGULATORY_SUBMISSION` (감독보고 제출 이력)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`               | `NUMBER`    | `PK, IDENTITY`              | 제출 이력 ID      |
| `REPORT_CODE`      | `VARCHAR2(50)` | `NOT NULL`                  | 보고서 코드       |
| `SNAPSHOT_ID`      | `NUMBER`    | `FK, NOT NULL`              | 관련 스냅샷 ID    |
| `SUBMISSION_DATE`  | `DATE`      | `NOT NULL`                  | 제출 일자         |
| `SUBMITTER`        | `VARCHAR2(50)` | `NOT NULL`                  | 제출자            |
| `SUBMISSION_CHANNEL`| `VARCHAR2(50)` | `NULL`                      | 제출 채널         |
| `RESPONSE_STATUS`  | `VARCHAR2(50)` | `NULL`                      | 수신 결과         |
| `CREATE_DATE`      | `TIMESTAMP` | `NOT NULL, DEFAULT SYSTIMESTAMP` | 생성일            |
| `AUDIT_USER`       | `VARCHAR2(50)` | `NOT NULL, DEFAULT 'SYSTEM'` | 감사 사용자       |

## 44. `RECONCILIATION_RESULTS` (대사 결과)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`               | `NUMBER`    | `PK, IDENTITY`              | 결과 ID           |
| `RECONCILIATION_DATE`| `DATE`      | `NOT NULL`                  | 대사 일자         |
| `RECONCILIATION_TYPE`| `VARCHAR2(50)`| `NOT NULL`                  | 대사 유형         |
| `STATUS`           | `VARCHAR2(50)`| `NOT NULL`                  | 상태              |
| `TOTAL_AMOUNT_SOURCE`| `NUMBER(19, 2)`| `NOT NULL`                  | 원천 총액         |
| `TOTAL_AMOUNT_TARGET`| `NUMBER(19, 2)`| `NOT NULL`                  | 대상 총액         |
| `VARIANCE_AMOUNT`  | `NUMBER(19, 2)`| `NOT NULL`                  | 차이액           |

## 45. `RECONCILIATION_VARIANCES` (대사 차이 및 관리)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`               | `NUMBER`    | `PK, IDENTITY`              | 차이 ID           |
| `RECONCILIATION_RESULT_ID`| `NUMBER` | `FK, NOT NULL`              | 대사 결과 ID      |
| `VARIANCE_CODE`    | `VARCHAR2(50)` | `NOT NULL`                  | 차이 코드         |
| `CAUSE_CODE`       | `VARCHAR2(20)` | `NULL`                      | 원인 코드         |
| `AMOUNT`           | `NUMBER(19, 2)`| `NOT NULL`                  | 차이 금액         |
| `ADJUSTMENT_JOURNAL_ENTRY_ID`| `NUMBER`| `FK, NULL`                 | 조정 전표 ID      |
| `STATUS`           | `VARCHAR2(50)` | `NOT NULL`                  | 상태 (OPEN, ADJUSTED, etc.) |
| `SLA_DUE_DATE`     | `DATE`      | `NULL`                      | SLA 해결 기한     |
| `ASSIGNED_USER_ID` | `VARCHAR2(50)` | `NULL`                      | 담당자 ID         |

## 46. `RECON_UNIT_DEFINITION` (대사 단위 정의)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `UNIT_ID`          | `VARCHAR2(50)` | `PK`                        | 대사 단위 ID      |
| `UNIT_NAME`        | `VARCHAR2(100)`| `NOT NULL`                  | 대사 단위 명      |
| `RECON_TYPE`       | `VARCHAR2(50)` | `NOT NULL`                  | 대사 유형         |
| `TOLERANCE_AMOUNT` | `NUMBER(19, 2)`| `DEFAULT 0.00`              | 허용 오차         |
| `SLA_DAYS`         | `NUMBER(3)` | `DEFAULT 3`                 | 해결 기한 (일)    |
| `MATCHING_RULES_JSON`| `CLOB`      | `NULL`                      | 자동 매칭 룰 (JSON)|

## 47. `RECON_STAGE_RESULT` (대사 단계별 결과)
| 컬럼명               | 데이터 타입   | 제약 조건                   | 설명              |
|--------------------|-------------|-----------------------------|-------------------|
| `ID`               | `NUMBER`    | `PK, IDENTITY`              | ID                |
| `RECON_RESULT_ID`  | `NUMBER`    | `FK, NOT NULL`              | 대사 결과 ID (FK) |
| `STAGE_CODE`       | `VARCHAR2(20)` | `NOT NULL`                  | 단계 (SOURCE, INTERFACE, etc.) |
| `TOTAL_AMOUNT`     | `NUMBER(19, 2)`| `NOT NULL`                  | 단계별 총액       |
