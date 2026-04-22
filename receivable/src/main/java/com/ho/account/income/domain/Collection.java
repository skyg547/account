package com.ho.account.income.domain;

import com.ho.account.basic.domain.BusinessPartner;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner customer; // 입금 고객 (거래처)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // 수금액

    @Column(length = 100)
    private String bankAccount; // 입금된 은행 계좌 (이름 또는 번호)

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

    public BusinessPartner getCustomer() {
        return customer;
    }

    public void setCustomer(BusinessPartner customer) {
        this.customer = customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
