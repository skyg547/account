package com.ho.account.asset.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IFRS 16 리스 계약에 대한 상환 스케줄 엔티티.
 * 각 리스료 지급 건별로 이자 비용, 원금 상환액, 잔여 리스부채를 기록합니다.
 * 리스 계약과 N:1 관계를 가집니다.
 */
@Entity
@Table(name = "lease_payment_schedules")
public class LeasePaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_contract_id", nullable = false)
    private LeaseContract leaseContract; // 관련 리스 계약

    @Column(nullable = false)
    private LocalDate paymentDate; // 리스료 지급 예정일

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal scheduledPaymentAmount; // 예정된 리스료 지급액

    @Column(precision = 19, scale = 2)
    private BigDecimal actualPaymentAmount; // 실제 지급된 리스료 (변경/재측정 시 달라질 수 있음)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestPortion; // 이자 비용 부분

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalPortion; // 원금 상환 부분

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingLeaseLiability; // 해당 회차 지급 후 잔여 리스부채

    @Column(length = 20)
    private String status; // SCHEDULED, PAID, CANCELLED, ADJUSTED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = "SCHEDULED";
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

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getScheduledPaymentAmount() {
        return scheduledPaymentAmount;
    }

    public void setScheduledPaymentAmount(BigDecimal scheduledPaymentAmount) {
        this.scheduledPaymentAmount = scheduledPaymentAmount;
    }

    public BigDecimal getActualPaymentAmount() {
        return actualPaymentAmount;
    }

    public void setActualPaymentAmount(BigDecimal actualPaymentAmount) {
        this.actualPaymentAmount = actualPaymentAmount;
    }

    public BigDecimal getInterestPortion() {
        return interestPortion;
    }

    public void setInterestPortion(BigDecimal interestPortion) {
        this.interestPortion = interestPortion;
    }

    public BigDecimal getPrincipalPortion() {
        return principalPortion;
    }

    public void setPrincipalPortion(BigDecimal principalPortion) {
        this.principalPortion = principalPortion;
    }

    public BigDecimal getRemainingLeaseLiability() {
        return remainingLeaseLiability;
    }

    public void setRemainingLeaseLiability(BigDecimal remainingLeaseLiability) {
        this.remainingLeaseLiability = remainingLeaseLiability;
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
