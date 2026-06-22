package com.ho.account.receivable.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수금(입금) 엔티티.
 * 고객으로부터 입금된 금액 정보를 관리합니다.
 */
@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate collectionDate; // 수금일

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode; // 입금 고객 코드 (거래처)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 수금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount = BigDecimal.ZERO; // 지금까지 채권에 배분한 누적 금액

    @Column(length = 100)
    private String bankAccount; // 입금된 당행 계좌 (이름 또는 번호)

    @Column(length = 100)
    private String virtualAccount; // 가상 계좌 정보 (사용하는 경우)

    @Column(length = 100)
    private String referenceNo; // 매칭을 위한 참조 번호 (예: 인보이스 번호, 주문 번호)

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private CollectionStatus status; // 수금 상태 (RECEIVED, MATCHED, PARTIAL_MATCHED, UNMATCHED, CANCELLED)


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CollectionStatus.RECEIVED; // 초기 상태는 RECEIVED (수신됨)
        }
        if (matchedAmount == null) {
            matchedAmount = BigDecimal.ZERO;
        }
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
