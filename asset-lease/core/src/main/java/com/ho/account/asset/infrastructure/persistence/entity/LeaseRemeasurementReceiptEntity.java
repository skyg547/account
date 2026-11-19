package com.ho.account.asset.infrastructure.persistence.entity;

import com.ho.account.asset.domain.LeaseRemeasurementReceipt;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Original event amounts are retained with the source transaction, so retries never recalculate them. */
@Entity
@Table(name = "lease_remeasurement_receipts", uniqueConstraints = @UniqueConstraint(
        name = "uk_lease_remeasurement_operation", columnNames = {"contract_no", "operation_id"}))
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LeaseRemeasurementReceiptEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contract_no", nullable = false, length = 100)
    private String contractNo;
    @Column(name = "operation_id", nullable = false, length = 80)
    private String operationId;
    @Column(nullable = false)
    private int fingerprintVersion = 1;
    @Column(nullable = false, length = 64)
    private String requestFingerprint;
    @Column(nullable = false)
    private Long contractId;
    @Column(nullable = false)
    private LocalDate remeasureDate;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal adjustmentAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal oldLiabilityAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal newLiabilityAmount;
    @Column(length = 20)
    private String departmentCode;
    @Column(nullable = false)
    private String actor;

    public LeaseRemeasurementReceiptEntity(String contractNo, String operationId, String requestFingerprint,
            Long contractId, LocalDate remeasureDate, BigDecimal adjustmentAmount, BigDecimal oldLiabilityAmount,
            BigDecimal newLiabilityAmount, String departmentCode, String actor) {
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
    }

    public LeaseRemeasurementReceiptEntity(LeaseRemeasurementReceipt receipt) {
        this(receipt.getContractNo(), receipt.getOperationId(), receipt.getRequestFingerprint(), receipt.getContractId(),
                receipt.getRemeasureDate(), receipt.getAdjustmentAmount(), receipt.getOldLiabilityAmount(),
                receipt.getNewLiabilityAmount(), receipt.getDepartmentCode(), receipt.getActor());
        this.fingerprintVersion = receipt.getFingerprintVersion();
    }

    public LeaseRemeasurementReceipt toDomain() {
        return new LeaseRemeasurementReceipt(contractNo, operationId, requestFingerprint, contractId, remeasureDate,
                adjustmentAmount, oldLiabilityAmount, newLiabilityAmount, departmentCode, actor, fingerprintVersion);
    }
}
