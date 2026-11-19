package com.ho.account.asset.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Immutable original financial event; raw request identity is checked before replaying its amounts. */
public final class LeaseRemeasurementReceipt {
    private final String contractNo;
    private final String operationId;
    private final String requestFingerprint;
    private final Long contractId;
    private final LocalDate remeasureDate;
    private final BigDecimal adjustmentAmount;
    private final BigDecimal oldLiabilityAmount;
    private final BigDecimal newLiabilityAmount;
    private final String departmentCode;
    private final String actor;
    private final int fingerprintVersion;

    public LeaseRemeasurementReceipt(String contractNo, String operationId, String requestFingerprint, Long contractId, LocalDate remeasureDate, BigDecimal adjustmentAmount, BigDecimal oldLiabilityAmount, BigDecimal newLiabilityAmount, String departmentCode, String actor) {
        this(contractNo, operationId, requestFingerprint, contractId, remeasureDate, adjustmentAmount, oldLiabilityAmount, newLiabilityAmount, departmentCode, actor, 1);
    }

    public LeaseRemeasurementReceipt(String contractNo, String operationId, String requestFingerprint, Long contractId, LocalDate remeasureDate, BigDecimal adjustmentAmount, BigDecimal oldLiabilityAmount, BigDecimal newLiabilityAmount, String departmentCode, String actor, int fingerprintVersion) {
        this.contractNo = contractNo;
        this.operationId = operationId;
        this.requestFingerprint = requestFingerprint;
        this.contractId = contractId;
        this.remeasureDate = remeasureDate;
        this.adjustmentAmount = adjustmentAmount;
        this.oldLiabilityAmount = oldLiabilityAmount;
        this.newLiabilityAmount = newLiabilityAmount;
        this.departmentCode = departmentCode;
        this.actor = actor;
        this.fingerprintVersion = fingerprintVersion;
    }

    public void requireMatchingRequest(String fingerprint) {
        if (fingerprintVersion != 1 || !requestFingerprint.equals(fingerprint)) {
            throw new LeaseRemeasurementConflictException();
        }
    }

    public String getContractNo() { return contractNo; }
    public String getOperationId() { return operationId; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public Long getContractId() { return contractId; }
    public LocalDate getRemeasureDate() { return remeasureDate; }
    public BigDecimal getAdjustmentAmount() { return adjustmentAmount; }
    public BigDecimal getOldLiabilityAmount() { return oldLiabilityAmount; }
    public BigDecimal getNewLiabilityAmount() { return newLiabilityAmount; }
    public String getDepartmentCode() { return departmentCode; }
    public String getActor() { return actor; }
    public int getFingerprintVersion() { return fingerprintVersion; }
}
