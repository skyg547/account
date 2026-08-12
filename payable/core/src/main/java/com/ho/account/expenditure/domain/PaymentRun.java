package com.ho.account.expenditure.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 지급 실행 (Payment Run) 엔티티 (Pure Java POJO).
 * 여러 지급을 묶어 일괄적으로 처리하고 관리하는 단위입니다.
 *
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 지급 실행 도메인 모델 객체로서 JPA 의존성을 제거하여 기술 프레임워크로부터 완벽히 분리됩니다.
 */
public class PaymentRun {

    private Long id;
    private LocalDate runDate;
    private String description;
    private PaymentRunStatus status = PaymentRunStatus.INITIATED;
    private String createdBy = "SYSTEM";
    private LocalDateTime createdAt = LocalDateTime.now();

    public PaymentRun() {
    }

    // Getter 및 Setter
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
