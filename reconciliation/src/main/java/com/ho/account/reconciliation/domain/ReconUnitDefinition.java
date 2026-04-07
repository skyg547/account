package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "RECON_UNIT_DEFINITION")
public class ReconUnitDefinition {

    @Id
    @Column(name = "UNIT_ID", length = 50)
    private String unitId;

    @Column(name = "UNIT_NAME", nullable = false, length = 100)
    private String unitName;

    @Enumerated(EnumType.STRING)
    @Column(name = "RECON_TYPE", nullable = false, length = 50)
    private ReconciliationType reconType;

    @Column(name = "PRODUCT_CODE", length = 20)
    private String productCode;

    @Column(name = "CURRENCY_CODE", length = 3)
    private String currencyCode;

    @Column(name = "LEGAL_ENTITY_CODE", length = 20)
    private String legalEntityCode;

    @Column(name = "TOLERANCE_AMOUNT", precision = 19, scale = 2)
    private BigDecimal toleranceAmount = BigDecimal.ZERO;

    @Column(name = "SLA_DAYS")
    private Integer slaDays = 3;

    @Column(name = "ASSIGNED_DEPT", length = 20)
    private String assignedDept;

    @Lob
    @Column(name = "MATCHING_RULES_JSON")
    private String matchingRulesJson;

    @Column(name = "IS_ACTIVE", nullable = false, length = 1)
    private String isActive = "Y";

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDateTime.now();
        this.updateDate = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }

    // Getter 및 Setter
    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public ReconciliationType getReconType() {
        return reconType;
    }

    public void setReconType(ReconciliationType reconType) {
        this.reconType = reconType;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getLegalEntityCode() {
        return legalEntityCode;
    }

    public void setLegalEntityCode(String legalEntityCode) {
        this.legalEntityCode = legalEntityCode;
    }

    public BigDecimal getToleranceAmount() {
        return toleranceAmount;
    }

    public void setToleranceAmount(BigDecimal toleranceAmount) {
        this.toleranceAmount = toleranceAmount;
    }

    public Integer getSlaDays() {
        return slaDays;
    }

    public void setSlaDays(Integer slaDays) {
        this.slaDays = slaDays;
    }

    public String getAssignedDept() {
        return assignedDept;
    }

    public void setAssignedDept(String assignedDept) {
        this.assignedDept = assignedDept;
    }

    public String getMatchingRulesJson() {
        return matchingRulesJson;
    }

    public void setMatchingRulesJson(String matchingRulesJson) {
        this.matchingRulesJson = matchingRulesJson;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
