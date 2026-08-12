package com.ho.account.expenditure.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Payment] 도메인 엔티티 (Pure Java POJO).
 * 공급업체에게 실제로 지급된 내역을 관리합니다.
 * 타 모듈과는 ID/Code 기반으로 참조하여 결합도를 낮춥니다.
 *
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 1. 도메인 계층의 기술/영속성 프레임워크 독립성:
 *    Payment 클래스는 JPA 기술 어노테이션에 의존하지 않는 pure Java 객체입니다.
 *    지급 처리(beginExecutionAttempt, markAsCompleted, markAsFailed) 등의 핵심 비즈니스 캡슐화 로직을
 *    프레임워크 종속성 없이 순수하게 유지합니다.
 *
 * 2. Data Mapper 패턴의 아키텍처적 이점:
 *    DB 매핑 및 영속화는 PaymentJpaEntity와 PaymentMapper가 담당하여 도메인 모델과 데이터베이스 스키마 간 격리를 보장합니다.
 */
public class Payment {

    private Long id;
    private LocalDate paymentDate;
    private String vendorCode;
    private Long payableId;
    private BigDecimal amount;
    private String bankAccount;
    private String referenceNo;
    private int executionAttempts;
    private String failureReason;
    private PaymentStatus status = PaymentStatus.INITIATED;
    private Long journalEntryId;
    private PaymentRun paymentRun;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Payment() {
    }

    public boolean canExecute() {
        return status == PaymentStatus.INITIATED
                || status == PaymentStatus.APPROVED
                || status == PaymentStatus.FAILED;
    }

    public void beginExecutionAttempt() {
        if (!canExecute()) {
            throw new IllegalStateException("현재 상태에서는 지급을 실행할 수 없습니다: " + status);
        }
        this.executionAttempts++;
        this.failureReason = null;
    }

    public void markAsCompleted(String bankAccount, String referenceNo) {
        if (!canExecute()) {
            throw new IllegalStateException("현재 상태에서는 지급 완료 처리를 할 수 없습니다: " + status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.bankAccount = bankAccount;
        this.referenceNo = referenceNo;
        this.failureReason = null;
    }

    public void markAsFailed(String failureReason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
    }

    // Getter 및 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public int getExecutionAttempts() { return executionAttempts; }
    public void setExecutionAttempts(int executionAttempts) { this.executionAttempts = executionAttempts; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Long getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(Long journalEntryId) { this.journalEntryId = journalEntryId; }

    public PaymentRun getPaymentRun() { return paymentRun; }
    public void setPaymentRun(PaymentRun paymentRun) { this.paymentRun = paymentRun; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
