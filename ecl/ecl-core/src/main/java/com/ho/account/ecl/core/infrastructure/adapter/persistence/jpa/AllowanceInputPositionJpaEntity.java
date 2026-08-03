package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.shared.finance.enums.CustomerType;
import com.ho.account.shared.finance.enums.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Account Mart 소유 테이블을 읽기 위한 ECL 전용 persistence read model.
 */
@Entity(name = "EclAllowanceInputPosition")
@Table(name = "allowance_input_positions")
@IdClass(AllowanceInputPositionJpaId.class)
public class AllowanceInputPositionJpaEntity {
    @Id @Column(name = "base_dt", nullable = false) private LocalDate baseDt;
    @Id @Column(name = "acc_no", nullable = false, length = 50) private String accNo;
    @Column(name = "customer_code", nullable = false, length = 50) private String customerCode;
    @Column(name = "customer_name", length = 200) private String customerName;
    @Enumerated(EnumType.STRING) @Column(name = "cust_type", length = 30) private CustomerType customerType;
    @Column(name = "is_sme") private Boolean isSme;
    @Column(name = "country_cd", length = 10) private String countryCode;
    @Column(name = "prod_cd", length = 20) private String productCode;
    @Enumerated(EnumType.STRING) @Column(name = "prod_category", length = 30) private ProductCategory productCategory;
    @Enumerated(EnumType.STRING) @Column(name = "currency", nullable = false, length = 3) private CurrencyCode currency;
    @Column(name = "outstd_amt", precision = 19, scale = 4) private BigDecimal outstandingAmount;
    @Column(name = "limit_amt", precision = 19, scale = 4) private BigDecimal limitAmount;
    @Column(name = "int_rate", precision = 10, scale = 6) private BigDecimal interestRate;
    @Column(name = "open_dt") private LocalDate openDate;
    @Column(name = "maturity_dt") private LocalDate maturityDate;
    @Column(name = "repayment_method", length = 20) private String repaymentMethod;
    @Column(name = "grace_period") private Integer gracePeriod;
    @Column(name = "repayment_freq") private Integer repaymentFreq;
    @Column(name = "internal_rating", length = 10) private String internalRating;
    @Column(name = "industry_cd", length = 20) private String industryCode;
    @Column(name = "warning_level", length = 20) private String warningLevel;
    @Column(name = "is_debt_restructured") private Boolean isDebtRestructured;
    @Column(name = "delinquent_days") private Integer delinquentDays;
    @Enumerated(EnumType.STRING) @Column(name = "staging", length = 20) private CrStaging staging;
    @Column(name = "branch_cd", length = 10) private String branchCode;
    @Column(name = "biz_unit_cd", length = 10) private String businessUnitCode;

    public LocalDate getBaseDt() { return baseDt; }
    public String getAccNo() { return accNo; }
    public String getCustomerCode() { return customerCode; }
    public String getCustomerName() { return customerName; }
    public CustomerType getCustomerType() { return customerType; }
    public Boolean getIsSme() { return isSme; }
    public String getCountryCode() { return countryCode; }
    public String getProductCode() { return productCode; }
    public ProductCategory getProductCategory() { return productCategory; }
    public CurrencyCode getCurrency() { return currency; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public LocalDate getOpenDate() { return openDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public String getRepaymentMethod() { return repaymentMethod; }
    public Integer getGracePeriod() { return gracePeriod; }
    public Integer getRepaymentFreq() { return repaymentFreq; }
    public String getInternalRating() { return internalRating; }
    public String getIndustryCode() { return industryCode; }
    public String getWarningLevel() { return warningLevel; }
    public Boolean getIsDebtRestructured() { return isDebtRestructured; }
    public Integer getDelinquentDays() { return delinquentDays; }
    public CrStaging getStaging() { return staging; }
    public String getBranchCode() { return branchCode; }
    public String getBusinessUnitCode() { return businessUnitCode; }
}
