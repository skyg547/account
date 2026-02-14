package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 지급 실행 (Payment Run) 엔티티.
 * 여러 지급을 묶어 일괄적으로 처리하고 관리하는 단위입니다.
 */
@Entity
@Table(name = "payment_runs")
public class PaymentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate runDate; // 지급 실행일

    @Column(length = 500)
    private String description; // 설명

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentRunStatus status; // 지급 실행 상태 (INITIATED, PROCESSING, COMPLETED, FAILED)

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentRunStatus.INITIATED;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PaymentRunStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentRunStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
