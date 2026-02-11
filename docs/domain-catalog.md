# K-Bank 차세대 재무 시스템 도메인 카탈로그 (3뎁스)

## 1. 자산 (Assets)
### 1.1. 고정자산 (Fixed Assets)
- **자산코드 (Asset Code)**: 고정자산 식별자. `(고유)`
- **자산명 (Asset Name)**: 고정자산의 이름.
- **계정 (Account Subject)**: 자산이 속한 계정 과목 (예: 차량운반구, 비품).
- **취득일 (Acquisition Date)**: 자산 취득일.
- **취득원가 (Acquisition Cost)**: 자산 취득에 소요된 비용. `(BigDecimal)`
- **내용연수 (Useful Life)**: 자산의 예상 사용 기간 (년).
- **감가상각방법 (Depreciation Method)**: 정액법, 정률법 등.
- **잔존가치 (Residual Value)**: 내용연수 종료 후 자산의 예상 가치. `(BigDecimal)`
- **감가상각누계액 (Accumulated Depreciation)**: 현재까지 누적된 감가상각액. `(BigDecimal)`
- **관리부서 (Department)**: 자산을 관리하는 부서.
- **상태 (Status)**: 사용중, 처분, 상각완료.
- **생성일 (Created At)**: 자산 정보 생성일.

### 1.2. 리스 (Lease) - IFRS16 준수
- **계약번호 (Contract No)**: 리스 계약 식별자. `(고유)`
- **계약명 (Contract Name)**: 리스 계약의 이름.
- **리스제공자 (Lessor)**: 리스를 제공하는 거래처 (고객).
- **시작일 (Start Date)**: 리스 계약 시작일.
- **종료일 (End Date)**: 리스 계약 종료일.
- **월리스료 (Monthly Payment)**: 매월 지급되는 리스료. `(BigDecimal)`
- **지급일 (Payment Day)**: 매월 리스료가 지급되는 일자 (예: 25일).
- **관리부서 (Department)**: 리스 계약을 관리하는 부서.
- **비용계정 (Expense Account)**: 리스료가 처리되는 비용 계정 (예: 지급임차료).
- **상태 (Status)**: ACTIVE, TERMINATED, EXPIRED.
- **생성일 (Created At)**: 리스 계약 정보 생성일.

## 2. 부채 (Liabilities)
### 2.1. 미지급금/미수금 (Unsettled Items)
- **항목번호 (Item No)**: 미지급/미수 항목 식별자. `(고유)`
- **거래처 (Customer)**: 미지급/미수 대상 거래처.
- **금액 (Amount)**: 미지급/미수 금액. `(BigDecimal)`
- **발생일 (Occurrence Date)**: 미지급/미수 발생일.
- **예정일 (Due Date)**: 미지급/미수 예정일.
- **처리상태 (Status)**: PENDING, COMPLETED, OVERDUE.
- **관련전표 (Related Journal Entry)**: 미지급/미수 발생 원인이 된 전표.
- **생성일 (Created At)**: 미지급/미수 항목 생성일.

## 3. 회계기초 (Accounting Basic)
### 3.1. 계정과목 (Account Subjects)
- **계정코드 (Account Code)**: 계정과목 식별자. `(고유)`
- **계정명 (Account Name)**: 계정과목의 이름.
- **계정유형 (Account Type)**: 자산, 부채, 자본, 수익, 비용.
- **부모계정 (Parent Account)**: 상위 계정 (계층 구조).
- **사용여부 (Is Used)**: 계정 사용 여부.
- **생성일 (Created At)**: 계정과목 생성일.

### 3.2. 부서 (Departments)
- **부서코드 (Dept Code)**: 부서 식별자. `(고유)`
- **부서명 (Dept Name)**: 부서의 이름.
- **상위부서 (Parent Department)**: 상위 부서 (계층 구조).
- **사용여부 (Is Used)**: 부서 사용 여부.
- **생성일 (Created At)**: 부서 정보 생성일.

### 3.3. 거래처 (Customers)
- **거래처코드 (Customer Code)**: 거래처 식별자. `(고유)`
- **거래처명 (Customer Name)**: 거래처의 이름.
- **사업자등록번호 (Business Registration No)**: 사업자등록번호.
- **담당자 (Contact Person)**: 거래처 담당자.
- **연락처 (Contact No)**: 거래처 연락처.
- **생성일 (Created At)**: 거래처 정보 생성일.

## 4. 전표/장부 (Journal/Ledger)
### 4.1. 전표 (Journal Entry)
- **전표번호 (Journal No)**: 전표 식별자. `(고유)`
- **전표일자 (Journal Date)**: 전표 발생일.
- **유형 (Type)**: 일반전표, 결산전표 등.
- **생성부서 (Department)**: 전표를 생성한 부서.
- **생성자 (Created By)**: 전표를 생성한 사용자.
- **상태 (Status)**: DRAFT, POSTED, CANCELED.
- **차변총액 (Total Debit)**: 전표의 차변 총액. `(BigDecimal)`
- **대변총액 (Total Credit)**: 전표의 대변 총액. `(BigDecimal)`
- **생성일 (Created At)**: 전표 생성일.

### 4.2. 전표 상세 (Journal Detail)
- **상세번호 (Detail No)**: 전표 상세 식별자.
- **전표번호 (Journal No)**: 상위 전표.
- **계정 (Account Subject)**: 해당 상세 항목의 계정.
- **차변금액 (Debit Amount)**: 차변 금액. `(BigDecimal)`
- **대변금액 (Credit Amount)**: 대변 금액. `(BigDecimal)`
- **적요 (Description)**: 상세 설명.
- **생성일 (Created At)**: 상세 항목 생성일.

## 5. 예산/지출 (Budget/Expenditure)
### 5.1. 예산 (Budget)
- **예산코드 (Budget Code)**: 예산 식별자. `(고유)`
- **예산명 (Budget Name)**: 예산의 이름.
- **회계연도 (Fiscal Year)**: 예산이 속한 회계연도.
- **부서 (Department)**: 예산이 할당된 부서.
- **계정 (Account Subject)**: 예산이 할당된 계정.
- **할당금액 (Allocated Amount)**: 초기 할당 예산 금액. `(BigDecimal)`
- **집행금액 (Executed Amount)**: 현재까지 집행된 금액. `(BigDecimal)`
- **잔여금액 (Remaining Amount)**: 잔여 예산 금액. `(BigDecimal)`
- **상태 (Status)**: ACTIVE, CLOSED.
- **생성일 (Created At)**: 예산 생성일.

### 5.2. 지출결의 (Expenditure Resolution)
- **결의번호 (Resolution No)**: 지출결의 식별자. `(고유)`
- **결의일자 (Resolution Date)**: 지출결의일.
- **신청부서 (Requesting Department)**: 지출을 신청한 부서.
- **신청자 (Requester)**: 지출을 신청한 사용자.
- **총금액 (Total Amount)**: 지출 결의 총 금액. `(BigDecimal)`
- **승인상태 (Approval Status)**: PENDING, APPROVED, REJECTED.
- **생성일 (Created At)**: 지출결의 생성일.

### 5.3. 지출 상세 (Expenditure Detail)
- **상세번호 (Detail No)**: 지출 상세 식별자.
- **결의번호 (Resolution No)**: 상위 지출결의.
- **계정 (Account Subject)**: 지출 항목의 계정.
- **금액 (Amount)**: 지출 금액. `(BigDecimal)`
- **적요 (Description)**: 상세 설명.
- **생성일 (Created At)**: 상세 항목 생성일.

### 5.4. 지급 (Payment)
- **지급번호 (Payment No)**: 지급 식별자. `(고유)`
- **지급일자 (Payment Date)**: 지급일.
- **거래처 (Customer)**: 지급 대상 거래처.
- **지급금액 (Payment Amount)**: 실제 지급된 금액. `(BigDecimal)`
- **지급수단 (Payment Method)**: 현금, 계좌이체 등.
- **관련결의 (Related Resolution)**: 관련 지출결의.
- **생성일 (Created At)**: 지급 정보 생성일.

## 6. 결산/마감 (Closing)
### 6.1. 일별 마감 (Daily Closing Status)
- **마감일자 (Closing Date)**: 마감된 일자. `(고유)`
- **상태 (Status)**: OPEN, CLOSED, REOPENED.
- **생성일 (Created At)**: 마감 정보 생성일.

### 6.2. 월별/연도별 마감 (Closing Status)
- **마감연월 (Closing Year Month)**: 마감된 연도 및 월 (YYYYMM). `(고유)`
- **상태 (Status)**: OPEN, CLOSED, REOPENED.
- **생성일 (Created At)**: 마감 정보 생성일.

### 6.3. 대출회계 (Loan Accounting)
- **대출계약번호 (Loan Contract No)**: 대출 계약 식별자. `(고유)`
- **고객 (Customer)**: 대출을 받은 고객.
- **대출상품 (Loan Product)**: 대출 상품명.
- **대출원금 (Principal Amount)**: 대출 원금. `(BigDecimal)`
- **실행일 (Disbursement Date)**: 대출 실행일.
- **만기일 (Maturity Date)**: 대출 만기일.
- **이자율 (Interest Rate)**: 연간 이자율. `(BigDecimal)`
- **상환방법 (Repayment Method)**: 원리금균등, 만기일시 등.
- **상태 (Status)**: ACTIVE, PAID_OFF, DEFAULT.
- **현재원금잔액 (Current Principal Balance)**: 현재 남은 원금 잔액. `(BigDecimal)`
- **이연대출부대손익 (Deferred Loan Fee Income/Expense)**: 대출 발생 시점의 부대비용 및 수익 (가감) `(BigDecimal)`
- **유효이자율 (Effective Interest Rate - EIR)**: 이연대출부대손익을 반영한 실질 이자율. `(BigDecimal)`
- **생성일 (Created At)**: 대출 계약 정보 생성일.

## 7. 세금계산서 (Tax Invoice)
### 7.1. 세금계산서 (Tax Invoice)
- **세금계산서번호 (Invoice No)**: 세금계산서 식별자. `(고유)`
- **발행일자 (Issue Date)**: 세금계산서 발행일.
- **공급자 (Supplier)**: 공급자 거래처.
- **공급받는자 (Recipient)**: 공급받는자 거래처.
- **공급가액 (Supply Amount)**: 공급가액. `(BigDecimal)`
- **세액 (Tax Amount)**: 세액. `(BigDecimal)`
- **총액 (Total Amount)**: 총액 (공급가액 + 세액). `(BigDecimal)`
- **유형 (Type)**: 매입, 매출.
- **상태 (Status)**: ISSUED, CANCELED.
- **생성일 (Created At)**: 세금계산서 생성일.

## 8. 재무보고 (Financial Report)
### 8.1. 재무보고서 (Financial Report)
- **보고서번호 (Report No)**: 재무보고서 식별자. `(고유)`
- **보고서명 (Report Name)**: 재무보고서의 이름.
- **보고서유형 (Report Type)**: 재무상태표, 손익계산서, 현금흐름표 등.
- **기준일자 (Base Date)**: 보고서 기준일자.
- **생성일 (Created At)**: 보고서 생성일.

### 8.2. 재무보고서 주석 (Financial Note)
- **주석번호 (Note No)**: 주석 식별자.
- **보고서번호 (Report No)**: 상위 재무보고서.
- **주석내용 (Note Content)**: 주석 내용.
- **생성일 (Created At)**: 주석 생성일.