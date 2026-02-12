package com.ho.account.report.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "RPT_LINE_MAPPING")
public class ReportLineMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "REPORT_TYPE", nullable = false, length = 50)
    private String reportType; // BS, IS, CF, REGULATORY

    @Column(name = "LINE_CODE", nullable = false, length = 50)
    private String lineCode;

    @Column(name = "LINE_NAME", nullable = false, length = 200)
    private String lineName;

    @Column(name = "ACCOUNT_CODE", length = 20)
    private String accountCode;

    @Column(name = "AGGREGATION_TYPE", nullable = false, length = 20)
    private String aggregationType; // SUM, DIFF, FORMULA

    @Column(name = "FORMULA_EXPRESSION", length = 500)
    private String formulaExpression;

    @Column(name = "DISPLAY_ORDER", nullable = false)
    private Integer displayOrder;

    @Column(name = "PARENT_LINE_CODE", length = 50)
    private String parentLineCode;

    @Column(name = "VERSION", nullable = false)
    private Integer version = 1;

    @Column(name = "VALID_FROM_DATE", nullable = false)
    private LocalDate validFromDate;

    @Column(name = "VALID_TO_DATE")
    private LocalDate validToDate;

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
        if (this.validFromDate == null)
            this.validFromDate = LocalDate.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getFormulaExpression() {
        return formulaExpression;
    }

    public void setFormulaExpression(String formulaExpression) {
        this.formulaExpression = formulaExpression;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getParentLineCode() {
        return parentLineCode;
    }

    public void setParentLineCode(String parentLineCode) {
        this.parentLineCode = parentLineCode;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDate getValidFromDate() {
        return validFromDate;
    }

    public void setValidFromDate(LocalDate validFromDate) {
        this.validFromDate = validFromDate;
    }

    public LocalDate getValidToDate() {
        return validToDate;
    }

    public void setValidToDate(LocalDate validToDate) {
        this.validToDate = validToDate;
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
