package com.ho.account.asset.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IFRS 16 에 따라 인식되는 리스부채 엔티티.
 * 리스 계약과 1:1 관계를 가집니다.
 */
@Entity
@Table(name = "lease_liabilities")
public class LeaseLiability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_contract_id", nullable = false, unique = true)
    private LeaseContract leaseContract; // 관련 리스 계약

    @Column(nullable = false)
    private LocalDate recognitionDate; // 리스부채 인식일

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal initialValue; // 최초 인식 가액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentValue; // 현재 리스부채 잔액 (원금)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal accumulatedInterestExpense; // 누적 이자 비용

    @Column(length = 20)
    private String status; // ACTIVE, SETTLED, TERMINATED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = "ACTIVE";
    }

    // Getters and Setters
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

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getAccumulatedInterestExpense() {
        return accumulatedInterestExpense;
    }

    public void setAccumulatedInterestExpense(BigDecimal accumulatedInterestExpense) {
        this.accumulatedInterestExpense = accumulatedInterestExpense;
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
