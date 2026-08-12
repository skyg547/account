package com.ho.account.expenditure.infrastructure.persistence.entity;

import com.ho.account.expenditure.domain.PaymentRunStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PaymentRun JPA 영속성 엔티티.
 * DB `payment_runs` 테이블과 매핑되며, 도메인 POJO(PaymentRun)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "payment_runs")
public class PaymentRunJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate runDate;

    @Column(length = 500)
    private String description;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentRunStatus status;

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentRunStatus.INITIATED;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getRunDate() { return runDate; }
    public void setRunDate(LocalDate runDate) { this.runDate = runDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PaymentRunStatus getStatus() { return status; }
    public void setStatus(PaymentRunStatus status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
