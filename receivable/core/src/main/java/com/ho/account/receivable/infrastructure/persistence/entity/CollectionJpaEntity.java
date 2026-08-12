package com.ho.account.receivable.infrastructure.persistence.entity;

import com.ho.account.receivable.domain.CollectionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Collection JPA 영속성 엔티티.
 * DB `collections` 테이블과 매핑되며, 도메인 POJO(Collection)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "collections")
public class CollectionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate collectionDate;

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount = BigDecimal.ZERO;

    @Column(length = 100)
    private String bankAccount;

    @Column(length = 100)
    private String virtualAccount;

    @Column(length = 100)
    private String referenceNo;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private CollectionStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = CollectionStatus.RECEIVED;
        }
        if (matchedAmount == null) {
            matchedAmount = BigDecimal.ZERO;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDate collectionDate) { this.collectionDate = collectionDate; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getMatchedAmount() { return matchedAmount; }
    public void setMatchedAmount(BigDecimal matchedAmount) { this.matchedAmount = matchedAmount; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getVirtualAccount() { return virtualAccount; }
    public void setVirtualAccount(String virtualAccount) { this.virtualAccount = virtualAccount; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public CollectionStatus getStatus() { return status; }
    public void setStatus(CollectionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
