# Loan Accounting Module Documentation

## Overview
The Loan Accounting Module is designed to manage the financial aspects of loan contracts, including disbursement, deferred fees/costs, Effective Interest Rate (EIR) amortization, and recalculations due to events like early repayments or condition changes. It adheres to the provided DoD: "실행→이연→3개월 상각→중도상환 재계산까지 재현" (Reproduce execution -> deferral -> 3 months amortization -> recalculation for early repayment).

## Key Concepts

*   **Loan (대출):** Represents the core loan contract with details such as principal, interest rate, disbursal/maturity dates, and current status. It is the central entity for all loan-related accounting.
*   **Loan Disbursal (대출 실행):** Records the actual disbursement of loan funds, linking to a generated journal entry.
*   **Loan Event (대출 이벤트):** Tracks significant events affecting a loan's accounting, such as early repayments, changes in loan conditions, or rescheduling.
*   **Deferred Item Type (이연 항목 유형):** Master data defining types of deferred income/expenses (e.g., loan origination fees) and mapping them to relevant accounting accounts (deferred asset, recognized income).
*   **Deferred Item (이연 항목):** A specific instance of a deferred fee or cost associated with a loan, which will be amortized over the loan's life.
*   **EIR Amortization Schedule (EIR 상각 스케줄):** A detailed schedule of interest income, principal repayment, and deferred item amortization calculated using the Effective Interest Rate (EIR) method. Each entry typically links to a journal entry.
*   **Recalculation Run (재계산 실행):** Records the history of EIR recalculations due to loan events, capturing old/new EIRs, maturity dates, and impact analysis.

## Architecture

The loan accounting module follows the layered architecture of the existing Spring Boot application:

*   **`domain` package:** Contains the JPA entities for `Loan`, `LoanDisbursal`, `LoanEvent`, `DeferredItemType`, `DeferredItem`, `EIRAmortizationSchedule`, and `RecalculationRun`.
*   **`repository` package:** Contains Spring Data JPA repositories for the domain entities, including custom finder methods.
*   **`service` package:** Contains the core business logic in `LoanService`. This includes CRUD operations for loans and deferred item types, methods for disbursing loans, creating deferred items, generating/recalculating EIR amortization schedules, processing loan events, and a dedicated method (`reproduceDoDScenario`) to demonstrate the end-to-end DoD flow.
*   **`dto` package:** Contains Data Transfer Objects for request and response payloads for the REST API.
*   **`web` package:** Contains `LoanController` which exposes REST endpoints for interacting with the loan accounting functionalities.

## Entities

### `Loan`
Core loan contract details.
*   `id`: Primary Key
*   `loanNumber`: Unique identifier for the loan
*   `businessPartner`: Borrower (Many-to-one to `BusinessPartner`)
*   `loanType`: `TERM_LOAN`, `REVOLVING_LOAN`, etc.
*   `currency`: Loan currency (Many-to-one to `Currency`)
*   `principalAmount`: Original principal
*   `interestRate`: Nominal interest rate
*   `disbursalDate`, `maturityDate`: Key dates
*   `paymentFrequency`: Monthly, Quarterly, etc.
*   `initialEIR`, `currentEIR`: Effective interest rates
*   `status`: `ACTIVE`, `REPAID`, `DEFAULTED`

### `LoanDisbursal`
Record of loan fund disbursement.
*   `id`: Primary Key
*   `loan`: Many-to-one to `Loan`
*   `disbursalDate`, `disbursedAmount`
*   `journalEntry`: Related `JournalEntry` for disbursement

### `LoanEvent`
Significant events affecting the loan.
*   `id`: Primary Key
*   `loan`: Many-to-one to `Loan`
*   `eventType`: `EARLY_REPAYMENT`, `CONDITION_CHANGE`, `RESCHEDULE`, etc.
*   `eventDate`, `description`
*   `relatedJournalEntry`: Related `JournalEntry` if applicable
*   `recalculationRun`: Related `RecalculationRun` if triggered

### `DeferredItemType`
Master data for types of deferred income/expenses.
*   `id`: Primary Key
*   `code`, `name`, `description`
*   `deferralMethod`: `STRAIGHT_LINE`, `EIR_METHOD`
*   `deferredAssetAccount`: Account for deferred asset (Many-to-one to `AccountSubject`)
*   `recognizedIncomeAccount`: Account for recognized income (Many-to-one to `AccountSubject`)
*   `isActive`

### `DeferredItem`
Specific deferred amount for a loan.
*   `id`: Primary Key
*   `loan`: Many-to-one to `Loan`
*   `deferredItemType`: Many-to-one to `DeferredItemType`
*   `amount`: Total deferred amount
*   `deferralDate`, `amortizationStartDate`, `amortizationEndDate`
*   `remainingAmount`
*   `initialJournalEntry`: Related `JournalEntry` for initial deferral
*   `status`: `DEFERRED`, `AMORTIZING`, `FULLY_AMORTIZED`

### `EIRAmortizationSchedule`
Detailed EIR amortization schedule entries.
*   `id`: Primary Key
*   `loan`: Many-to-one to `Loan`
*   `scheduleDate`: Date for the schedule entry
*   `beginningBalance`, `interestIncome`, `principalRepayment`, `endingBalance`
*   `deferredItemAmortization`: Amortized portion of deferred items
*   `cashFlow`
*   `amortizationJournalEntry`: Related `JournalEntry` for amortization
*   `isRecalculated`: Flag if this entry is part of a recalculated schedule

### `RecalculationRun`
History of EIR recalculations.
*   `id`: Primary Key
*   `loan`: Many-to-one to `Loan`
*   `recalculationDate`, `reason`: `EARLY_REPAYMENT`, `CONDITION_CHANGE`
*   `oldEIR`, `newEIR`
*   `oldMaturityDate`, `newMaturityDate`
*   `recalculatedAmortizationScheduleStart`: First entry of the new schedule (Many-to-one to `EIRAmortizationSchedule`)
*   `impactAnalysis`: Details of impact (JSON/text)
*   `adjustmentJournalEntry`: Related `JournalEntry` for adjustments due to recalculation

## Workflows

### 1. Loan Origination
1.  **Create Loan:** A new `Loan` contract is created, and its initial EIR is calculated.
2.  **Disburse Loan:** `LoanDisbursal` is recorded, and an initial `JournalEntry` (debit Loan Receivable, credit Cash/Bank) is created.
3.  **Create Deferred Items:** Any deferred fees or costs (e.g., origination fees) are created as `DeferredItem`s, linking to their `DeferredItemType`. An initial `JournalEntry` (debit Deferred Asset, credit Cash/Bank or Revenue) is created.

### 2. EIR Amortization
1.  **Generate Amortization Schedule:** Based on the loan terms, EIR, and deferred items, a detailed `EIRAmortizationSchedule` is generated for the entire loan term.
2.  **Periodic Amortization:** Periodically (e.g., monthly), the system creates `JournalEntry` records based on the amortization schedule entries. This typically involves recognizing interest income and amortizing deferred items (e.g., debit Cash/Bank, credit Loan Receivable, credit Interest Income; debit Interest Income, credit Deferred Asset for deferred item amortization).

### 3. Loan Events and Recalculation
1.  **Record Loan Event:** An event like `EARLY_REPAYMENT` or `CONDITION_CHANGE` occurs and is recorded as a `LoanEvent`.
2.  **Trigger Recalculation:** For events that impact cash flows or terms, the system triggers an EIR recalculation.
3.  **Create Recalculation Run:** A `RecalculationRun` record is created, capturing the old/new EIR, maturity dates, and other relevant information.
4.  **Generate New Schedule:** A new `EIRAmortizationSchedule` is generated from the event date onwards, reflecting the new EIR and terms.
5.  **Adjustment Journal Entries:** If the recalculation results in a material change requiring immediate accounting adjustment, a `JournalEntry` is created and linked to the `RecalculationRun`.

### 4. DoD Scenario (`reproduceDoDScenario`)
This method orchestrates the following sequence:
1.  **Execution:** A `Loan` is created and its initial EIR determined.
2.  **Deferral:** A `DeferredItem` (e.g., origination fee) is created for the loan, and its initial journal entry is posted.
3.  **3 Months Amortization:** The `EIRAmortizationSchedule` is generated. The system then simulates 3 periods of amortization, posting corresponding journal entries.
4.  **Early Repayment Recalculation:** A `LoanEvent` for early repayment is created after 3 months. This triggers a `RecalculationRun`, which recalculates the EIR and generates a new amortization schedule for the remaining term.

## API Endpoints

The `LoanController` (`/api/loan`) provides the following endpoints:

*   **`POST /loans`**: Create a new loan.
*   **`GET /loans/{id}`**: Retrieve a loan by ID.
*   **`POST /disbursals`**: Disburse a loan.

*   **`POST /deferred-item-types`**: Create a new deferred item type.
*   **`GET /deferred-item-types/by-code/{code}`**: Retrieve a deferred item type by code.
*   **`POST /deferred-items`**: Create a new deferred item for a loan.

*   **`POST /amortization-schedules/generate`**: Generate a new EIR amortization schedule for a loan.

*   **`POST /events`**: Process a loan event (e.g., early repayment, condition change) which may trigger recalculation.

*   **`POST /dod-scenario`**: Reproduce the DoD scenario for loan accounting.

## Future Enhancements

*   **Robust EIR Calculation Engine:** Implement a more sophisticated and configurable EIR calculation logic that can handle various loan terms, payment structures, and deferred item treatments.
*   **Dynamic Journal Detail Generation:** Enhance `createAutomatedJournalEntry` to dynamically create appropriate `JournalDetail` entries based on the specific transaction (e.g., interest income, principal repayment, deferred item amortization).
*   **Loan Payment Processing:** Integrate with a payment processing system to record actual loan repayments and update balances.
*   **Reporting:** Develop comprehensive reports for loan portfolios, amortization schedules, deferred item balances, and recalculation impacts.
*   **Rounding and Period Allocation Policies:** Implement detailed policies for rounding in financial calculations and allocating amounts across periods.
*   **Impact Analysis for Reopen:** Detail the logic for "재오픈 영향도 자동 산출 규칙(변경된 전표/원장/보고 라인)" for period reopening.
