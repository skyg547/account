package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [여신 도메인] 계정계 원장(Ledger) 엔티티
 */
@Entity
@Table(name = "ods_acc_ledger")
public class OdsAccountLedgerEntity {

    @Id 
    @Column(name = "acc_no", nullable = false, length = 50)
    @Comment("계좌번호")
    private String accountNo;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "prod_cd", length = 20) 
    private String productCode;

    @Column(name = "currency", length = 3) 
    private String currency;

    @Column(name = "outstd_amt", precision = 19, scale = 4)
    private BigDecimal outstandingAmount;

    @Column(name = "limit_amt", precision = 19, scale = 4)
    private BigDecimal limitAmount;

    @Column(name = "int_rate", precision = 10, scale = 6)
    private BigDecimal interestRate;

    @Column(name = "base_rate_cd", length = 20)
    private String baseRateCode;

    @Column(name = "spread", precision = 10, scale = 6)
    private BigDecimal spread;

    @Column(name = "next_reset_dt") 
    private LocalDate nextResetDate;

    @Column(name = "open_dt") 
    private LocalDate openDate;

    @Column(name = "maturity_dt") 
    private LocalDate maturityDate;

    @Column(name = "delinquent_days") 
    private Integer delinquentDays;

    @Column(name = "repayment_method", length = 20)
    private String repaymentMethod;

    @Column(name = "grace_period")
    private Integer gracePeriod = 0;

    @Column(name = "repayment_freq")
    private Integer repaymentFreq = 1;

    @Column(name = "branch_cd") 
    private String branchCd;

    @Column(name = "biz_unit_cd") 
    private String bizUnitCd;

    @Column(name = "is_active") 
    private Boolean isActive = true;

    public OdsAccountLedgerEntity() {}
    
    // Getters and Setters
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String val) { this.accountNo = val; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String val) { this.customerCode = val; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String val) { this.productCode = val; }
    public String getCurrency() { return currency; }
    public void setCurrency(String val) { this.currency = val; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal val) { this.outstandingAmount = val; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal val) { this.limitAmount = val; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal val) { this.interestRate = val; }
    public String getBaseRateCode() { return baseRateCode; }
    public void setBaseRateCode(String val) { this.baseRateCode = val; }
    public BigDecimal getSpread() { return spread; }
    public void setSpread(BigDecimal val) { this.spread = val; }
    public LocalDate getNextResetDate() { return nextResetDate; }
    public void setNextResetDate(LocalDate val) { this.nextResetDate = val; }
    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate val) { this.openDate = val; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate val) { this.maturityDate = val; }
    public Integer getDelinquentDays() { return delinquentDays; }
    public void setDelinquentDays(Integer val) { this.delinquentDays = val; }
    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String val) { this.repaymentMethod = val; }
    public Integer getGracePeriod() { return gracePeriod; }
    public void setGracePeriod(Integer val) { this.gracePeriod = val; }
    public Integer getRepaymentFreq() { return repaymentFreq; }
    public void setRepaymentFreq(Integer val) { this.repaymentFreq = val; }
    public String getBranchCd() { return branchCd; }
    public void setBranchCd(String val) { this.branchCd = val; }
    public String getBizUnitCd() { return bizUnitCd; }
    public void setBizUnitCd(String val) { this.bizUnitCd = val; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean val) { this.isActive = val; }

    public static class Builder {
        private final OdsAccountLedgerEntity entity = new OdsAccountLedgerEntity();
        public Builder accountNo(String val) { entity.accountNo = val; return this; }
        public Builder customerCode(String val) { entity.customerCode = val; return this; }
        public Builder productCode(String val) { entity.productCode = val; return this; }
        public Builder currency(String val) { entity.currency = val; return this; }
        public Builder outstandingAmount(BigDecimal val) { entity.outstandingAmount = val; return this; }
        public Builder limitAmount(BigDecimal val) { entity.limitAmount = val; return this; }
        public Builder interestRate(BigDecimal val) { entity.interestRate = val; return this; }
        public Builder baseRateCode(String val) { entity.baseRateCode = val; return this; }
        public Builder spread(BigDecimal val) { entity.spread = val; return this; }
        public Builder nextResetDate(LocalDate val) { entity.nextResetDate = val; return this; }
        public Builder openDate(LocalDate val) { entity.openDate = val; return this; }
        public Builder maturityDate(LocalDate val) { entity.maturityDate = val; return this; }
        public Builder delinquentDays(Integer val) { entity.delinquentDays = val; return this; }
        public Builder repaymentMethod(String val) { entity.repaymentMethod = val; return this; }
        public Builder gracePeriod(Integer val) { entity.gracePeriod = val; return this; }
        public Builder repaymentFreq(Integer val) { entity.repaymentFreq = val; return this; }
        public Builder branchCd(String val) { entity.branchCd = val; return this; }
        public Builder bizUnitCd(String val) { entity.bizUnitCd = val; return this; }
        public Builder isActive(boolean val) { entity.isActive = val; return this; }
        public OdsAccountLedgerEntity build() { return entity; }
    }
    public static Builder builder() { return new Builder(); }
}
