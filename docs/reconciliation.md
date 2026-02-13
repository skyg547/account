# Reconciliation Module Documentation

## Overview
The Reconciliation Module provides functionality to automate and manage the process of reconciling financial data from various sources within the `account` Spring Boot application. It supports defining reconciliation criteria, applying matching rules, identifying differences, and managing the resolution of these differences, adhering to the "Definition of Done" (DoD) where differences must be resolved with a "Reason Code" and a "link to an Adjustment Journal Entry".

## Key Concepts

*   **Reconciliation Unit (대사 단위):** Defines the scope and criteria for a specific reconciliation process (e.g., "Bank vs Book Reconciliation", "GL vs Sub-ledger Reconciliation").
*   **Reconciliation Rule (대사 규칙):** Specifies how items within a reconciliation unit should be matched, including tolerance levels and priority.
*   **Reconciliation Run (대사 실행):** Records a single execution of a reconciliation unit, capturing statistics, status, and associated differences.
*   **Reconciliation Difference (대사 차이):** Represents a discrepancy found during a reconciliation run. It is linked to a `DifferenceReasonCode` and can be linked to an `Adjustment Journal Entry`.
*   **Difference Reason Code (차이 사유 코드):** Categorizes the reason for a reconciliation difference, indicating whether an adjustment entry is typically required.
*   **Adjustment Journal Entry (조정 전표):** A standard `JournalEntry` of type `ADJUSTMENT` created to resolve a `ReconciliationDifference`.

## Architecture

The reconciliation module follows the layered architecture of the existing Spring Boot application:

*   **`domain` package:** Contains the JPA entities for `ReconciliationUnit`, `ReconciliationRule`, `ReconciliationRun`, `ReconciliationDifference`, and `DifferenceReasonCode`.
*   **`repository` package:** Contains Spring Data JPA repositories for the domain entities.
*   **`service` package:** Contains the core business logic in `ReconciliationService`, including CRUD operations for master data, the `performReconciliation` logic, and methods for assigning differences.
*   **`dto` package:** Contains Data Transfer Objects for request and response payloads for the REST API.
*   **`web` package:** Contains `ReconciliationController` which exposes REST endpoints for interacting with the reconciliation functionalities.

## Entities

### `ReconciliationUnit`
Defines what is being reconciled.
*   `id`: Primary Key
*   `name`: Unique name (e.g., "은행계좌 대사")
*   `description`: Description of the unit
*   `frequency`: `DAILY`, `MONTHLY`, etc.
*   `reconciliationType`: `BANK_BOOK`, `GL_SUBLEDGER`, etc.
*   `criteriaJson`: JSON string defining dynamic criteria (e.g., `{"bankAccount":"123-456", "currency":"KRW"}`)
*   `isActive`: Boolean

### `ReconciliationRule`
Defines matching logic for a `ReconciliationUnit`.
*   `id`: Primary Key
*   `reconciliationUnit`: Many-to-one relationship with `ReconciliationUnit`
*   `name`: Rule name
*   `ruleDefinitionJson`: JSON string defining matching fields, conditions, aggregation (e.g., `{"matchFields":["transactionId", "amount"], "tolerance":"absolute"}`)
*   `toleranceType`: `NONE`, `ABSOLUTE`, `PERCENTAGE`
*   `toleranceValue`: Value for tolerance
*   `priority`: Order of rule application
*   `isActive`: Boolean

### `DifferenceReasonCode`
Categorizes reasons for discrepancies.
*   `id`: Primary Key
*   `code`: Unique code (e.g., "BANK_FEE")
*   `name`: Display name
*   `description`: Detailed description
*   `isAdjustable`: Boolean, indicates if an adjustment entry is typically needed
*   `isActive`: Boolean

### `ReconciliationRun`
Records each execution of a reconciliation.
*   `id`: Primary Key
*   `reconciliationUnit`: Many-to-one relationship with `ReconciliationUnit`
*   `reconciliationDate`: Date of reconciliation
*   `runStartTime`, `runEndTime`: Timestamps
*   `status`: `RUNNING`, `SUCCESS`, `FAILED`, `PARTIAL`
*   Statistics: `totalItemsSource`, `totalAmountSource`, `totalItemsTarget`, `totalAmountTarget`, `matchedItemsCount`, `matchedAmount`, `unmatchedItemsCount`, `unmatchedAmount`
*   `runBy`: User who initiated the run

### `ReconciliationDifference`
Details specific discrepancies.
*   `id`: Primary Key
*   `reconciliationRun`: Many-to-one relationship with `ReconciliationRun`
*   `differenceType`: `AMOUNT_MISMATCH`, `MISSING_SOURCE`, etc.
*   `amountExpected`, `amountActual`, `differenceAmount`: Financial details of the difference
*   `description`: Textual explanation
*   `sourceItemRef`, `targetItemRef`: JSON strings referencing the original items causing the difference (e.g., `{"type":"BANK_TRANSACTION", "id":"TXN123"}`)
*   `reasonCode`: Many-to-one relationship with `DifferenceReasonCode` (DoD met)
*   `adjustmentJournalEntry`: Many-to-one relationship with `JournalEntry` (DoD met)
*   `status`: `PENDING`, `ASSIGNED`, `RESOLVED`, `IGNORED`
*   `assignedToUser`: User assigned to resolve
*   `slaDueDate`: Service Level Agreement due date
*   `resolvedAt`, `resolvedBy`: Resolution details

## Workflows

### 1. Configuration
1.  **Define Reconciliation Units:** Create `ReconciliationUnit`s specifying what data needs to be reconciled and under what criteria.
2.  **Define Reconciliation Rules:** For each `ReconciliationUnit`, create `ReconciliationRule`s to specify automated matching logic and acceptable tolerance levels.
3.  **Define Difference Reason Codes:** Set up `DifferenceReasonCode`s to categorize discrepancies, indicating if they typically require adjustment.

### 2. Reconciliation Execution
1.  An automated process or a user initiates a reconciliation run for a specific `ReconciliationUnit` and `reconciliationDate` via the `/api/reconciliation/run` endpoint.
2.  The `ReconciliationService` fetches the relevant `ReconciliationUnit` and its associated `ReconciliationRule`s.
3.  Based on the `criteriaJson` of the `ReconciliationUnit`, the service fetches (or simulates fetching in current mock) source and target financial data.
4.  It applies the `ReconciliationRule`s in order of priority to match items between the source and target data.
5.  Any items that remain unmatched or have differences beyond tolerance are recorded as `ReconciliationDifference` entities.
6.  For each `ReconciliationDifference`, a `DifferenceReasonCode` is assigned. If the `DifferenceReasonCode` is marked as `isAdjustable`, an `Adjustment Journal Entry` (of `ADJUSTMENT` type) is automatically created and linked to the `ReconciliationDifference`.
7.  A `ReconciliationRun` record is updated with statistics and its final status (`SUCCESS`, `FAILED`, `PARTIAL`).

### 3. Difference Resolution
1.  Detected `ReconciliationDifference`s are initially in `PENDING` status.
2.  Users or automated processes can assign `ReconciliationDifference`s to specific users and set an `slaDueDate` via `/api/reconciliation/differences/assign`. The status changes to `ASSIGNED`.
3.  Assigned users investigate the difference, leveraging the linked `Adjustment Journal Entry` if one was automatically created, or creating a new one manually if needed.
4.  Once resolved, the `ReconciliationDifference` status is updated (e.g., `RESOLVED`, `IGNORED`).

## API Endpoints

The `ReconciliationController` (`/api/reconciliation`) provides the following endpoints:

*   **`POST /units`**: Create a new reconciliation unit.
*   **`GET /units`**: Retrieve all reconciliation units.
*   **`GET /units/{id}`**: Retrieve a reconciliation unit by ID.
*   **`PUT /units/{id}`**: Update an existing reconciliation unit.
*   **`DELETE /units/{id}`**: Delete a reconciliation unit.

*   **`POST /rules`**: Create a new reconciliation rule.
*   **`GET /units/{unitId}/rules`**: Retrieve all rules for a specific reconciliation unit.
*   **`PUT /rules/{id}`**: Update an existing reconciliation rule.
*   **`DELETE /rules/{id}`**: Delete a reconciliation rule.

*   **`POST /reason-codes`**: Create a new difference reason code.
*   **`GET /reason-codes`**: Retrieve all difference reason codes.
*   **`GET /reason-codes/{id}`**: Retrieve a difference reason code by ID.
*   **`PUT /reason-codes/{id}`**: Update an existing difference reason code.
*   **`DELETE /reason-codes/{id}`**: Delete a difference reason code.

*   **`POST /run`**: Initiate a reconciliation run for a given unit and date.
*   **`POST /differences/assign`**: Assign a reconciliation difference to a user and set SLA.
*   **`GET /runs/{runId}/differences`**: Retrieve all differences for a specific reconciliation run.

## Future Enhancements

*   **Dynamic Data Fetching:** Implement actual data retrieval mechanisms based on `criteriaJson` for various sources (e.g., external banking APIs, internal ledger queries).
*   **Sophisticated Matching Engine:** Develop a flexible matching engine that interprets `ruleDefinitionJson` to perform complex matching logic beyond simple equality (e.g., fuzzy matching, aggregation, sequence matching).
*   **User Management Integration:** Integrate `assignedToUser` with a proper User entity for better access control and notifications.
*   **Notification System:** Implement alerts for overdue SLA or high-value differences.
*   **Reporting Dashboard:** Develop UI for monitoring reconciliation runs and differences.
