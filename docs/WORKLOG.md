# Accounting System Development Worklog

This document tracks the development progress, design decisions, and architectural choices made during the implementation of the new accounting system based on the provided DoD.

## Phase 1: Project Setup & Core Model

**Objective:** Establish project-wide entities and principles based on DoD sections 01-06.

### 1.1 `AccountSubject`(계정과목) Entity
*   **DoD Requirement:** DoD 04.1 (COA/계정과목 + 보고/감독 매핑), DoD 04.2 (SCD2).
*   **Analysis:** The existing `AccountSubject` entity already implements SCD2 with `validFrom` and `validTo`. It uses a self-referencing `parent` field for hierarchical structure.
*   **Changes Made:**
    1.  Renamed `AccountCategory` enum to `AccountType`.
    2.  Renamed field `category` to `accountType` and updated its getter/setter.
    3.  Renamed field `reportLine` to `financialReportMappingCode` and updated its getter/setter.
    4.  Added new field `regulatoryMappingCode` (String) with its getter/setter.

### 1.2 `Department`(부서) Entity
*   **DoD Requirement:** DoD 04.2 (조직/귀속부서(코스트센터/손익센터) + 조직개편(SCD2)).
*   **Analysis:** The existing `Department` entity already implements SCD2 with `validFrom` and `validTo`. It uses a self-referencing `parent` field for hierarchical structure, and the `DepartmentType` enum covers `COST_CENTER` and `PROFIT_CENTER`.
*   **Changes Made:** No significant changes required, as the existing structure already meets the DoD requirements.

### 1.3 `Customer`(거래처) Entity
*   **DoD Requirement:** DoD 04.3 (거래처(벤더/고객/기관/BP) + 계좌 + KYC/상태).
*   **Analysis:** The existing `Customer` entity was limited. It was generalized to `BusinessPartner` to encompass vendors, customers, and other business partners. It now includes fields for bank accounts and KYC/status information, and SCD2 implementation.
*   **Changes Made:**
    1.  Renamed `Customer` entity to `BusinessPartner` (file and class name).
    2.  Renamed `customerCode` to `businessPartnerCode` and `customerName` to `businessPartnerName`.
    3.  Added `PartnerType` enum (`CUSTOMER`, `VENDOR`, `BANK`, `OTHER_BP`) and `partnerType` field to `BusinessPartner`.
    4.  Created new entity `BusinessPartnerAccount` for bank account information and established a one-to-many relationship with `BusinessPartner`.
    5.  Added `KycStatus` enum (`PENDING`, `APPROVED`, `REJECTED`, `REVIEW_REQUIRED`) and `kycStatus` field to `BusinessPartner`.
    6.  Added `RiskRating` enum (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) and `riskRating` field to `BusinessPartner`.
    7.  Added `validFrom` and `validTo` (LocalDate) fields to `BusinessPartner` for SCD2.

### 1.4 `Product`(상품) Entity
*   **DoD Requirement:** DoD 04.4 (상품/상품군(Product Master)).
*   **Plan:**
    *   ...

### 1.5 `JournalEntry` & `JournalDetail`(전표/분개) Entities
*   **DoD Requirement:** DoD 05 & 06 (룰 엔진, 전표 상태모델(DRAFT/APPROVED/POSTED/REVERSED)).
*   **Plan:**
    *   ...

---

*This log will be updated as tasks are completed.*
