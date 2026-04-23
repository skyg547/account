package com.ho.account.asset.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IFRS 16 에 따라 인식되는 사용권자산 엔티티.
 * 리스 계약과 1:1 관계를 가집니다.
 */
@Entity
@Table(name = "right_of_use_assets")
public class RightOfUseAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_contract_id", nullable = false, unique = true)
    private LeaseContract leaseContract; // 관련 리스 계약

    @Column(nullable = false, length = 100)
    private String assetName; // 사용권자산 명칭

    @Column(nullable = false)
    private LocalDate recognitionDate; // 사용권자산 인식일

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal initialValue; // 최초 인식 가액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBookValue; // 현재 장부가액 (상각 후)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciation; // 누적 상각액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal depreciationAmountPerPeriod; // 기간별 상각액

    @Column(length = 20)
    private String status; // ACTIVE, DISPOSED, TERMINATED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = "ACTIVE";
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LeaseContract getLeaseContract() {
        return leaseContract;
    }

    public void setLeaseContract(LeaseContract leaseContract) {
        this.leaseContract = leaseContract;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public LocalDate getRecognitionDate() {
        return recognitionDate;
    }

    public void setRecognitionDate(LocalDate recognitionDate) {
        this.recognitionDate = recognitionDate;
    }

    public BigDecimal getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(BigDecimal initialValue) {
        this.initialValue = initialValue;
    }

    public BigDecimal getCurrentBookValue() {
        return currentBookValue;
    }

    public void setCurrentBookValue(BigDecimal currentBookValue) {
        this.currentBookValue = currentBookValue;
    }

    public BigDecimal getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) {
        this.accumulatedDepreciation = accumulatedDepreciation;
    }

    public BigDecimal getDepreciationAmountPerPeriod() {
        return depreciationAmountPerPeriod;
    }

    public void setDepreciationAmountPerPeriod(BigDecimal depreciationAmountPerPeriod) {
        this.depreciationAmountPerPeriod = depreciationAmountPerPeriod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
