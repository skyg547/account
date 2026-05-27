package com.risk.common.entity;

import com.risk.common.enums.CrStaging;
import com.risk.common.enums.CurrencyCode;
import com.risk.common.enums.CustomerType;
import com.risk.common.enums.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dim_integrated_position_master")
@IdClass(IntegratedRiskPositionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegratedRiskPosition {

    @Id
    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDt;

    @Id
    @Column(name = "acc_no", nullable = false, length = 50)
    private String accNo;

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "cust_type", length = 30)
    private CustomerType customerType;

    @Column(name = "is_sme")
    private Boolean isSme;

    @Column(name = "country_cd", length = 10)
    private String countryCode;

    @Column(name = "prod_cd", length = 20)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "prod_category", length = 30)
    private ProductCategory productCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 3)
    private CurrencyCode currency;

    @Column(name = "bs_class", length = 10)
    private String bsClass;

    @Column(name = "cur_bal", precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "outstd_amt", precision = 19, scale = 4)
    private BigDecimal outstandingAmount;

    @Column(name = "limit_amt", precision = 19, scale = 4)
    private BigDecimal limitAmount;

    @Column(name = "market_val", precision = 19, scale = 4)
    private BigDecimal marketValue;

    @Column(name = "int_rate", precision = 10, scale = 6)
    private BigDecimal interestRate;

    @Column(name = "coupon_rate", precision = 10, scale = 6)
    private BigDecimal couponRate;

    @Column(name = "rate_type", length = 10)
    private String rateType;

    @Column(name = "base_rate_cd", length = 20)
    private String baseRateCode;

    @Column(name = "spread", precision = 10, scale = 6)
    private BigDecimal spread;

    @Column(name = "payment_freq")
    private Integer paymentFreq;

    @Column(name = "next_reset_dt")
    private LocalDate nextResetDate;

    @Column(name = "open_dt")
    private LocalDate openDate;

    @Column(name = "maturity_dt")
    private LocalDate maturityDate;

    @Column(name = "repayment_method", length = 20)
    private String repaymentMethod;

    @Column(name = "grace_period")
    private Integer gracePeriod;

    @Column(name = "repayment_freq")
    private Integer repaymentFreq;

    @Column(name = "int_rate_floor", precision = 10, scale = 6)
    private BigDecimal interestRateFloor;

    @Column(name = "int_rate_cap", precision = 10, scale = 6)
    private BigDecimal interestRateCap;

    @Column(name = "ref_index_cd", length = 20)
    private String refIndexCode;

    @Column(name = "internal_rating", length = 10)
    private String internalRating;

    @Column(name = "external_rating", length = 10)
    private String externalRating;

    @Column(name = "industry_cd", length = 20)
    private String industryCode;

    @Column(name = "warning_level", length = 20)
    private String warningLevel;

    @Column(name = "is_debt_restructured")
    private Boolean isDebtRestructured;

    @Column(name = "delinquent_days")
    private Integer delinquentDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "staging", length = 20)
    private CrStaging staging;

    @Column(name = "coll_amt", precision = 19, scale = 4)
    private BigDecimal collateralAmount;

    @Column(name = "recognized_coll_amt", precision = 19, scale = 4)
    private BigDecimal recognizedCollateralAmount;

    @Column(name = "coll_type", length = 20)
    private String collateralType;

    @Column(name = "pd", precision = 10, scale = 8)
    private BigDecimal pd;

    @Column(name = "lgd", precision = 10, scale = 8)
    private BigDecimal lgd;

    @Column(name = "expected_loss", precision = 19, scale = 4)
    private BigDecimal expectedLoss;

    @Column(name = "unexpected_loss", precision = 19, scale = 4)
    private BigDecimal unexpectedLoss;

    @Column(name = "rwa_sa", precision = 19, scale = 4)
    private BigDecimal rwaSa;

    @Column(name = "rwa_irb", precision = 19, scale = 4)
    private BigDecimal rwaIrb;

    @Column(name = "branch_cd", length = 10)
    private String branchCd;

    @Column(name = "biz_unit_cd", length = 10)
    private String bizUnitCd;

    @Column(name = "margin_rate", precision = 10, scale = 6)
    private BigDecimal marginRate;

    @Column(name = "repricing_freq")
    private Integer repricingFreq;
}
