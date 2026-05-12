package com.ho.account.reconciliation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "RECON_EXTERNAL_STAGE_RECORD")
public class ExternalReconStageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "UNIT_ID", nullable = false, length = 50)
    private String unitId;

    @Column(name = "STAGE_CODE", nullable = false, length = 20)
    private String stageCode;

    @Column(name = "RECONCILIATION_DATE", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "SOURCE_SYSTEM_CODE", length = 50)
    private String sourceSystemCode;

    @Column(name = "ITEM_REFERENCE", length = 100)
    private String itemReference;

    @Column(name = "PRODUCT_CODE", length = 20)
    private String productCode;

    @Column(name = "CURRENCY_CODE", length = 3)
    private String currencyCode;

    @Column(name = "LEGAL_ENTITY_CODE", length = 20)
    private String legalEntityCode;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "AUDIT_USER", nullable = false, length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
        updateDate = LocalDateTime.now();
        if (auditUser == null) {
            auditUser = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public String getSourceSystemCode() {
        return sourceSystemCode;
    }

    public void setSourceSystemCode(String sourceSystemCode) {
        this.sourceSystemCode = sourceSystemCode;
    }

    public String getItemReference() {
        return itemReference;
    }

    public void setItemReference(String itemReference) {
        this.itemReference = itemReference;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
