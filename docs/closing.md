# Closing Module Documentation

## Overview
The Closing Module is designed to manage the period-end closing process for the accounting system. It provides tools for defining closing schedules, tracking tasks and gates, managing period locks and re-opening approvals, automating valuation and provision batches, and recording closing adjustments. A key feature is the system's ability to automatically determine the completion or failure of the closing process based on predefined conditions (DoD).

## Key Concepts

*   **Closing Calendar (결산 캘린더):** Defines the overall closing schedule for a specific fiscal year and period, tracking its status (OPEN, IN_PROGRESS, CLOSED).
*   **Closing Task (결산 태스크):** Individual checklist items or steps within a closing calendar (e.g., "Complete Bank Reconciliation", "Run FX Valuation"). Tasks can be mandatory and have completion conditions.
*   **Closing Gate (결산 게이트):** Critical checkpoints in the closing process that must be passed before proceeding. Gates have conditions that, when met, signify successful passage.
*   **Period Lock (기간 잠금):** Prevents further transactions or modifications to a fiscal period once it has been closed, ensuring data integrity.
*   **Reopen Approval (기간 재오픈 승인):** Manages the process of requesting, approving, and tracking the re-opening of a closed fiscal period, including impact analysis.
*   **Valuation Batch (평가 배치):** Records the execution of automated valuation processes, such as foreign currency revaluation or financial instrument fair value adjustments, linking to generated journal entries.
*   **Provision Batch (충당/손상 배치):** Records the execution of automated provision or impairment calculation processes (e.g., Expected Credit Loss - ECL), linking to generated journal entries.
*   **Closing Adjustment (결산 조정):** Tracks specific journal entries made during the closing process for adjustments or reclassifications.
*   **Fiscal Period:** An existing entity that this module heavily interacts with, providing the basic definition of accounting periods and their high-level closing status.

## Architecture

The closing module follows the layered architecture of the existing Spring Boot application:

*   **`domain` package:** Contains the JPA entities for `ClosingCalendar`, `ClosingTask`, `ClosingGate`, `PeriodLock`, `ReopenApproval`, `ValuationBatch`, `ProvisionBatch`, and `ClosingAdjustment`.
*   **`repository` package:** Contains Spring Data JPA repositories for the domain entities, including custom finder methods.
*   **`service` package:** Contains the core business logic in `ClosingService`, including CRUD operations for master data, management of closing tasks and gates, period lock/reopen flows, batch execution, and the crucial `determineClosingStatus` method (DoD implementation).
*   **`dto` package:** Contains Data Transfer Objects for request and response payloads for the REST API.
*   **`web` package:** Contains `ClosingController` which exposes REST endpoints for interacting with the closing functionalities.

## Entities

### `ClosingCalendar`
Manages the closing schedule for a fiscal period.
*   `id`: Primary Key
*   `fiscalYear`, `fiscalPeriod`: Identifies the period (e.g., "2023", "12")
*   `status`: `OPEN`, `IN_PROGRESS`, `CLOSED`, `PERMANENTLY_CLOSED`
*   `closeInitiatedBy`, `closeInitiatedAt`: Audit info for initiation
*   `closedBy`, `closedAt`: Audit info for closing
*   `reopenedBy`, `reopenedAt`: Audit info for reopening
*   `isCurrentPeriod`: Flag for the actively managed period

### `ClosingTask`
Individual steps in the closing process (checklist).
*   `id`: Primary Key
*   `closingCalendar`: Many-to-one to `ClosingCalendar`
*   `name`, `description`: Task details
*   `category`: `PRE_CLOSING`, `CLOSING_ENTRY`, `POST_CLOSING`, `REPORTING`
*   `dueDate`: Expected completion date
*   `assignedTo`: User responsible
*   `status`: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `SKIPPED`
*   `completionConditionJson`: JSON string defining dynamic completion conditions (e.g., `{ "reconciliationUnitId": 1, "status": "SUCCESS" }`)
*   `isMandatory`: If the task must be completed for closing
*   `taskOrder`: Order of execution

### `ClosingGate`
Critical checkpoints in the closing process.
*   `id`: Primary Key
*   `closingCalendar`: Many-to-one to `ClosingCalendar`
*   `name`, `description`: Gate details
*   `status`: `PENDING`, `PASSED`, `FAILED`
*   `checkConditionJson`: JSON string defining conditions for passing the gate (e.g., `{ "allTasksCompleted": true, "category": "PRE_CLOSING" }`)
*   `passedBy`, `passedAt`: Audit info for gate passage

### `PeriodLock`
Records when and how a fiscal period is locked.
*   `id`: Primary Key
*   `fiscalPeriod`: Many-to-one to `FiscalPeriod`
*   `lockType`: `ALL_TRANSACTIONS`, `NON_ADJUSTMENT_ENTRIES`, `PARTIAL_LOCK`
*   `lockedBy`, `lockedAt`: Audit info
*   `reason`: Reason for the lock

### `ReopenApproval`
Manages requests to reopen closed periods.
*   `id`: Primary Key
*   `fiscalPeriod`: Many-to-one to `FiscalPeriod`
*   `requestedBy`, `requestedAt`: Requestor info
*   `reason`: Reason for reopening request
*   `status`: `PENDING`, `APPROVED`, `REJECTED`
*   `approvedBy`, `approvedAt`: Approver info
*   `impactAnalysisReport`: Result of automatic impact analysis (e.g., changed journal entries)

### `ValuationBatch`
Records automated valuation runs.
*   `id`: Primary Key
*   `fiscalPeriod`: Many-to-one to `FiscalPeriod`
*   `valuationType`: `FX_RATE`, `FINANCIAL_INSTRUMENT`
*   `runDateTime`: When the batch was run
*   `status`: `RUNNING`, `COMPLETED`, `FAILED`
*   `generatedJournalEntry`: Many-to-one to `JournalEntry` (the entry created by valuation)
*   `reportLink`: Link to generated report
*   `runBy`: User/System who ran the batch

### `ProvisionBatch`
Records automated provision/impairment runs.
*   `id`: Primary Key
*   `fiscalPeriod`: Many-to-one to `FiscalPeriod`
*   `provisionType`: `BAD_DEBT`, `IMPAIRMENT`, `ECL`
*   `runDateTime`: When the batch was run
*   `status`: `RUNNING`, `COMPLETED`, `FAILED`
*   `generatedJournalEntry`: Many-to-one to `JournalEntry` (the entry created by provision)
*   `reportLink`: Link to generated report
*   `runBy`: User/System who ran the batch

### `ClosingAdjustment`
Records specific closing-related journal entries.
*   `id`: Primary Key
*   `fiscalPeriod`: Many-to-one to `FiscalPeriod`
*   `journalEntry`: Many-to-one to `JournalEntry` (the actual adjustment entry)
*   `adjustmentType`: `ACCRUAL`, `DEFERRAL`, `RECLASSIFICATION`
*   `description`: Description of adjustment
*   `approvedBy`, `approvedAt`: Audit info

## Workflows

### 1. Closing Setup
1.  **Create Closing Calendar:** For a given fiscal year and period.
2.  **Define Closing Tasks:** Add individual tasks to the calendar, mark mandatory tasks, set due dates, and define completion conditions.
3.  **Define Closing Gates:** Set up critical gates with conditions that must be met (e.g., all pre-closing tasks completed) before the closing process can proceed.

### 2. Period Closing Process
1.  **Initiate Closing:** Change `ClosingCalendar` status to `IN_PROGRESS`. This might automatically lock the period for certain transaction types.
2.  **Execute Tasks:** Users or automated processes complete `ClosingTask`s. The system validates `completionConditionJson` to update task status.
3.  **Pass Gates:** As tasks are completed, `ClosingGate` conditions are checked. If met, the gate status is updated to `PASSED`.
4.  **Run Batches:** Execute `ValuationBatch` and `ProvisionBatch` for the period. These generate `JournalEntry` records.
5.  **Record Adjustments:** Create `ClosingAdjustment` records for manual closing entries.
6.  **Determine Final Status (DoD):** The system's `determineClosingStatus` logic is triggered. It verifies:
    *   All mandatory `ClosingTask`s are `COMPLETED`.
    *   All `ClosingGate`s are `PASSED`.
    *   (Optionally) Other conditions like reconciliation completion (e.g., `ReconciliationRun` status) are met.
    *   If all conditions are met, the `ClosingCalendar` status changes to `CLOSED`, and the `FiscalPeriod` status is updated accordingly. Otherwise, the system flags failure and provides details.

### 3. Period Reopening
1.  A user requests to re-open a `CLOSED` period via `requestPeriodReopen`.
2.  An `ReopenApproval` request is created in `PENDING` status. The system might generate an `impactAnalysisReport` detailing potential effects of reopening.
3.  An authorized user reviews and `APPROVES` or `REJECTS` the request.
4.  If `APPROVED`, the `FiscalPeriod` and `ClosingCalendar` statuses are reverted to `OPEN`, and any existing `PeriodLock` is released.

## API Endpoints

The `ClosingController` (`/api/closing`) provides the following endpoints:

*   **`POST /calendars`**: Create a new closing calendar.
*   **`GET /calendars/{id}`**: Retrieve a closing calendar by ID.
*   **`GET /calendars/by-period?fiscalYear={year}&fiscalPeriod={period}`**: Retrieve a closing calendar by fiscal year and period.
*   **`PUT /calendars/{id}/status`**: Update the status of a closing calendar.

*   **`POST /tasks`**: Create a new closing task.
*   **`PUT /tasks/{id}/status`**: Update the status of a closing task.

*   **`POST /gates`**: Create a new closing gate.
*   **`PUT /gates/{id}/check`**: Check conditions and update status of a closing gate.

*   **`POST /period-locks`**: Lock a fiscal period.
*   **`DELETE /period-locks/{fiscalPeriodId}`**: Unlock a fiscal period.

*   **`POST /reopen-approvals`**: Request to re-open a fiscal period.
*   **`PUT /reopen-approvals/{id}/status`**: Approve or reject a re-open request.

*   **`POST /valuation-batches/run`**: Initiate a valuation batch.
*   **`POST /provision-batches/run`**: Initiate a provision batch.

*   **`POST /adjustments`**: Record a closing adjustment.

*   **`POST /calendars/determine-status`**: Trigger the system to determine the final closing status for a calendar.

## Future Enhancements

*   **Dynamic Condition Evaluation:** Implement a robust engine for dynamically evaluating `completionConditionJson` for tasks and `checkConditionJson` for gates, potentially integrating with other modules (e.g., Reconciliation).
*   **Automated Journal Detail Generation:** Enhance `createAutomatedJournalEntry` to generate appropriate `JournalDetail` entries based on valuation/provision results.
*   **Notification System:** Implement alerts for overdue tasks, failed gates, or pending re-open approvals.
*   **Comprehensive Reporting:** Develop detailed closing reports, including error queues, unsettled items, and reconciliation summaries, as indicated in the requirements.
*   **UI Integration:** Develop a user interface for managing closing calendars, tasks, and approvals.
