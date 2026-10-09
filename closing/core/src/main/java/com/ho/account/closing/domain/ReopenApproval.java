package com.ho.account.closing.domain;

import java.time.LocalDateTime;

/**
 * Approval request for reopening a fiscal period.
 * Stores only the fiscal period ID to avoid cross-module entity coupling.
 */

public class ReopenApproval {

    private Long id;

    private Long fiscalPeriodId;

    private String fiscalYear;

    private String fiscalPeriod;

    private String requestedBy;

    private LocalDateTime requestedAt;

    private String reason;

    private ReopenApprovalStatus status; // PENDING, APPROVED, REJECTED

    private String approvedBy;

    private LocalDateTime approvedAt;

    private String impactAnalysisReport;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String auditUser;

    public enum ReopenApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    public void setFiscalPeriodId(Long fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }

    public String getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(String fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public void assignFiscalPeriod(Long fiscalPeriodId, String fiscalYear, String fiscalPeriod) {
        this.fiscalPeriodId = fiscalPeriodId;
        this.fiscalYear = fiscalYear;
        this.fiscalPeriod = fiscalPeriod;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ReopenApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ReopenApprovalStatus status) {
        this.status = status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getImpactAnalysisReport() {
        return impactAnalysisReport;
    }

    public void setImpactAnalysisReport(String impactAnalysisReport) {
        this.impactAnalysisReport = impactAnalysisReport;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }

    public void request(String requestedBy, String reason) {
        String actor = requireActor(requestedBy, "requestedBy");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reopen reason must not be blank");
        }
        this.requestedBy = actor;
        this.requestedAt = LocalDateTime.now();
        this.reason = reason.trim();
        this.status = ReopenApprovalStatus.PENDING;
        this.auditUser = actor;
    }

    public void approve(String approvedBy) {
        decide(ReopenApprovalStatus.APPROVED, approvedBy);
    }

    public void reject(String approvedBy) {
        decide(ReopenApprovalStatus.REJECTED, approvedBy);
    }

    private void decide(ReopenApprovalStatus decision, String approvedBy) {
        String actor = requireActor(approvedBy, "approvedBy");
        if (this.status != ReopenApprovalStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING reopen request can be decided.");
        }
        if (actor.equals(this.requestedBy)) {
            throw new IllegalStateException("Reopen requester cannot approve or reject their own request.");
        }
        this.status = decision;
        this.approvedBy = actor;
        this.approvedAt = LocalDateTime.now();
        this.auditUser = actor;
    }

    private String requireActor(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
