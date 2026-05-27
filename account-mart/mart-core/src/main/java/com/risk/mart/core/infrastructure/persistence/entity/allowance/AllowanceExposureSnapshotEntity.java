package com.risk.mart.core.infrastructure.persistence.entity.allowance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "allowance_exposure_snapshots",
        indexes = {
                @Index(name = "idx_allowance_exposure_base", columnList = "base_date"),
                @Index(name = "idx_allowance_exposure_customer", columnList = "customer_code"),
                @Index(name = "idx_allowance_exposure_product_currency", columnList = "product_code,currency_code")
        }
)
@IdClass(AllowanceExposureSnapshotId.class)
@Getter
@Setter
public class AllowanceExposureSnapshotEntity {

    @Id
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Id
    @Column(name = "exposure_id", nullable = false, length = 80)
    private String exposureId;

    @Column(name = "source_system", nullable = false, length = 30)
    private String sourceSystem;

    @Column(name = "source_account_no", nullable = false, length = 50)
    private String sourceAccountNo;

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(name = "product_code", length = 20)
    private String productCode;

    @Column(name = "legal_entity_code", nullable = false, length = 20)
    private String legalEntityCode;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "outstanding_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingAmount;

    @Column(name = "undrawn_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal undrawnAmount;

    @Column(name = "interest_rate", precision = 10, scale = 6)
    private BigDecimal interestRate;

    @Column(name = "effective_interest_rate", precision = 10, scale = 6)
    private BigDecimal effectiveInterestRate;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "delinquent_days")
    private Integer delinquentDays;

    @Column(name = "original_rating", length = 20)
    private String originalRating;

    @Column(name = "current_rating", length = 20)
    private String currentRating;

    @Column(name = "warning_level", length = 20)
    private String warningLevel;

    @Column(name = "debt_restructured", nullable = false)
    private Boolean debtRestructured;

    @Column(name = "collateral_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal collateralValue;

    @Column(name = "collateral_type", length = 20)
    private String collateralType;

    @Column(name = "accounting_account_code", length = 20)
    private String accountingAccountCode;

    @Column(name = "allowance_account_code", length = 20)
    private String allowanceAccountCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
