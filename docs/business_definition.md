# Business Process Definition - Accounting System

This document defines the core business processes and entities within the `account` system.

## 1. Master Data Management (MDM)

### 1.1 Chart of Accounts (계정과목)
- **Entity**: `AccountSubject`
- **Definition**: Represents the fundamental units for accounting classification.
- **Key Features**: Supports hierarchical structure (parent-child) and SCD2 for historical tracking.

### 1.2 Business Partners (거래처)
- **Entity**: `BusinessPartner`
- **Definition**: Consolidates all external entities including Customers, Vendors, and Banks.
- **Key Features**: KYC status management and risk rating support.

### 1.3 Organizations (부서/유닛)
- **Entity**: `Department`
- **Definition**: Manages internal organizational structure as Cost Centers or Profit Centers.

## 2. Transaction Processing (Journaling)

### 2.1 Journal Entry Creation
- **Process**: Recording economic events as double-entry journal lines.
- **Lifecycle**: `DRAFT` (Initial) -> `REQUESTED` -> `APPROVED` (Review Complete) -> `POSTED` (Finalized).

### 2.2 Automated Journaling (Rule Engine)
- **Process**: Automatically generating journal entries from transaction events based on predefined `JournalRule`s.

## 3. General Ledger (Posting)

### 3.1 Ledger Posting
- **Process**: Transferring approved journal entries to the General Ledger.
- **Impact**: Generates `GlEntry` and updates persistent `GlBalance` for real-time reporting.

## 4. Multi-Currency Operations
- **Process**: Transactions recorded in foreign currencies are converted to the functional currency using `ExchangeRate` and stored as `BaseAmount`.
