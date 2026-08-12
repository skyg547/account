package com.ho.account.receivable.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수금(입금) 엔티티 (Pure Java POJO).
 * 고객으로부터 입금된 금액 정보를 관리합니다.
 *
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * Collection 도메인 객체는 JPA 기술 어노테이션에 독립적인 Pure Java POJO입니다.
 * 수금 배분(applyAllocation) 및 매칭 가능 판단(canMatch) 로직을 순수 자바 객체 내부로 캡슐화합니다.
 */
public class Collection {

    private Long id;
    private LocalDate collectionDate;
    private String customerCode;
    private BigDecimal amount;
    private BigDecimal matchedAmount = BigDecimal.ZERO;
    private String bankAccount;
    private String virtualAccount;
    private String referenceNo;
    private CollectionStatus status = CollectionStatus.RECEIVED;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Collection() {
    }

    public void markAsMatched() {
        this.status = CollectionStatus.MATCHED;
    }

    public void markAsPartialMatched() {
        this.status = CollectionStatus.PARTIAL_MATCHED;
    }

    public void markAsUnmatched() {
        this.status = CollectionStatus.UNMATCHED;
    }

    public boolean canMatch() {
        return status == CollectionStatus.RECEIVED || status == CollectionStatus.UNMATCHED || status == CollectionStatus.PARTIAL_MATCHED;
    }

    /**
     * 채권에 금액을 배분하고 남은 수금액에 맞춰 상태를 변경합니다.
     */
    public void applyAllocation(BigDecimal allocationAmount) {
        if (allocationAmount == null || allocationAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("매칭 금액은 0보다 커야 합니다.");
        }
        if (allocationAmount.compareTo(getUnallocatedAmount()) > 0) {
            throw new IllegalArgumentException("매칭 금액이 남은 수금액보다 클 수 없습니다.");
        }
        matchedAmount = effectiveMatchedAmount().add(allocationAmount);
        if (getUnallocatedAmount().compareTo(BigDecimal.ZERO) == 0) {
            markAsMatched();
        } else {
            markAsPartialMatched();
        }
    }

    public BigDecimal getUnallocatedAmount() {
        return amount.subtract(effectiveMatchedAmount());
    }

    private BigDecimal effectiveMatchedAmount() {
        return matchedAmount == null ? BigDecimal.ZERO : matchedAmount;
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getMatchedAmount() {
        return effectiveMatchedAmount();
    }

    public void setMatchedAmount(BigDecimal matchedAmount) {
        this.matchedAmount = matchedAmount;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getVirtualAccount() {
        return virtualAccount;
    }

    public void setVirtualAccount(String virtualAccount) {
        this.virtualAccount = virtualAccount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public CollectionStatus getStatus() {
        return status;
    }

    public void setStatus(CollectionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
