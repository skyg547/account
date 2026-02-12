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
*   **Changes Made:**
    1.  Created `Product` entity (Product.java) with `productCode`, `name`, `description`, `unitOfMeasure`, `price`, `productType`, and SCD2 fields (`validFrom`, `validTo`).
    2.  Created `ProductDto.java` for response payloads and `ProductRequestDto.java` for request payloads, including validation.
    3.  Created `ProductRepository.java` interface extending `JpaRepository` with custom methods for `findByProductCode` and `existsByProductCode`.
    4.  Created `ProductService.java` for business logic, including create, get, update, and deactivate operations, with SCD2 handling.
    5.  Created `ProductController.java` for REST API endpoints for Product management.

### 1.5 `JournalEntry` & `JournalDetail`(전표/분개) Entities
*   **DoD Requirement:** DoD 05 & 06 (룰 엔진, 전표 상태모델(DRAFT/APPROVED/POSTED/REVERSED)).
*   **Plan:**
    *   ...

## Phase 2: ERP Modules (AP/AR/FA/Lease)

**Objective:** Implement ERP-specific modules based on DoD sections 07-10.

### 2.1 P2P/AP (지출/매입) Module
*   **DoD Requirement:** DoD 07 (구매-지급: 매입세금계산서, 매입전표, 지급, 미지급금 관리).
*   **Analysis & Design Decisions:**
    *   **AP Invoices:** The existing `TaxInvoice` entity (`com.ho.account.tax.domain.TaxInvoice`) was chosen to represent AP invoices (specifically, `type = PURCHASE`). No new dedicated `PurchaseInvoice` entity was created.
        *   Created `src/main/java/com/ho/account/tax/dto/TaxInvoiceDto.java` and `TaxInvoiceRequestDto.java` for API communication.
        *   Added `findByIssueId(String issueId)` to `TaxInvoiceRepository.java` for improved lookup.
        *   Created `APInvoiceService.java` in `src/main/java/com/ho/account/expenditure/service` to manage `PURCHASE` type `TaxInvoice`s within the AP context, including CRUD operations and validation.
        *   Created `APInvoiceController.java` in `src/main/java/com/ho/account/expenditure/web` to expose REST endpoints for AP invoices.
    *   **AP Payments:** The existing `Payment` entity was refactored and enhanced.
        *   Renamed `Payment.java` to `APPayment.java` (file and class) in `src/main/java/com/ho/account/expenditure/domain`.
        *   Renamed `PaymentRepository.java` to `APPaymentRepository.java` (file and interface) in `src/main/java/com/ho/account/expenditure/repository`.
        *   Modified `APPayment` entity to include a nullable `@ManyToOne` relationship to `TaxInvoice` and an `unappliedAmount` field to handle partial payments/application.
        *   Created `src/main/java/com/ho/account/expenditure/dto/APPaymentDto.java` and `APPaymentRequestDto.java` for API communication.
        *   Created `APPaymentService.java` in `src/main/java/com/ho/account/expenditure/service` to manage `APPayment` business logic, linking to `ExpenditureResolution` and `TaxInvoice`.
        *   Created `APPaymentController.java` in `src/main/java/com/ho/account/expenditure/web` to expose REST endpoints for AP payments.
    *   **Integration (ExpenditureResolution & TaxInvoice/APPayment):**
        *   Added a nullable `@ManyToOne` relationship from `ExpenditureResolution` to `TaxInvoice` in `src/main/java/com/ho/account/expenditure/domain/ExpenditureResolution.java`, allowing expenditure resolutions to be linked to purchase tax invoices.
        *   Created `src/main/java/com/ho/account/expenditure/dto/ExpenditureResolutionRequestDto.java` to handle incoming requests for `ExpenditureResolution`, including the optional `taxInvoiceId`.
        *   Modified `ExpenditureService.java`:
            *   Updated constructor to inject `TaxInvoiceRepository` and `APInvoiceService`.
            *   Refactored `createResolution` to accept `ExpenditureResolutionRequestDto`, perform DTO-to-entity mapping, and fetch/validate `TaxInvoice` if linked.
            *   Removed the old `validateResolution` method.
            *   Added `updateResolution` method to handle updates to expenditure resolutions.
        *   Modified `ExpenditureController.java`:
            *   Updated `createResolution` to accept `ExpenditureResolutionRequestDto`.
            *   Added `updateResolution` endpoint.

*   **Further Considerations:** Advanced payment application logic (e.g., partial application of `APPayment` to multiple invoices/resolutions) will be handled in future iterations or specific payment application services.

