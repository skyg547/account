package com.ho.account.income.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?�금(?�금) ?�티??
 * 고객?�로부???�금??금액 ?�보�?관리합?�다.
 */
@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate collectionDate; // ?�금??

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner customer; // ?�금 고객 (거래�?

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // ?�금??

    @Column(length = 100)
    private String bankAccount; // ?�금???�??계좌 (?�름 ?�는 번호)

    @Column(length = 100)
    private String virtualAccount; // 가??계좌 ?�보 (?�용?�는 경우)

    @Column(length = 100)
    private String referenceNo; // 매칭???�한 참조 번호 (?? ?�보?�스 번호, 주문 번호)

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private CollectionStatus status; // ?�금 ?�태 (RECEIVED, MATCHED, PARTIAL_MATCHED, UNMATCHED, CANCELLED)


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CollectionStatus.RECEIVED; // 초기 ?�태??RECEIVED (?�신??
        }
    }

    // Getter �?Setter
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
