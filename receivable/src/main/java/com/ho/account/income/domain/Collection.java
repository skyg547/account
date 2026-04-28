package com.ho.account.income.domain;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?섍툑(?낃툑) ?뷀떚??
 * 怨좉컼?쇰줈遺???낃툑??湲덉븸 ?뺣낫瑜?愿由ы빀?덈떎.
 */
@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate collectionDate; // ?섍툑??

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "businessPartnerCode", nullable = false)
    private BusinessPartner customer; // ?낃툑 怨좉컼 (嫄곕옒泥?

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // ?섍툑??

    @Column(length = 100)
    private String bankAccount; // ?낃툑?????怨꾩쥖 (?대쫫 ?먮뒗 踰덊샇)

    @Column(length = 100)
    private String virtualAccount; // 媛??怨꾩쥖 ?뺣낫 (?ъ슜?섎뒗 寃쎌슦)

    @Column(length = 100)
    private String referenceNo; // 留ㅼ묶???꾪븳 李몄“ 踰덊샇 (?? ?몃낫?댁뒪 踰덊샇, 二쇰Ц 踰덊샇)

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private CollectionStatus status; // ?섍툑 ?곹깭 (RECEIVED, MATCHED, PARTIAL_MATCHED, UNMATCHED, CANCELLED)


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CollectionStatus.RECEIVED; // 珥덇린 ?곹깭??RECEIVED (?섏떊??
        }
    }

    // Getter 諛?Setter
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
