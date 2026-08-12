package com.ho.account.receivable.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CollectionAllocation JPA 영속성 엔티티.
 * DB `collection_allocations` 테이블과 매핑되며, 도메인 POJO(CollectionAllocation)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "collection_allocations")
public class CollectionAllocationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id", nullable = false)
    private CollectionJpaEntity collection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receivable_id", nullable = false)
    private ReceivableJpaEntity receivable;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal residualCollectionAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal residualReceivableAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CollectionJpaEntity getCollection() { return collection; }
    public void setCollection(CollectionJpaEntity collection) { this.collection = collection; }

    public ReceivableJpaEntity getReceivable() { return receivable; }
    public void setReceivable(ReceivableJpaEntity receivable) { this.receivable = receivable; }

    public BigDecimal getMatchedAmount() { return matchedAmount; }
    public void setMatchedAmount(BigDecimal matchedAmount) { this.matchedAmount = matchedAmount; }

    public BigDecimal getResidualCollectionAmount() { return residualCollectionAmount; }
    public void setResidualCollectionAmount(BigDecimal residualCollectionAmount) { this.residualCollectionAmount = residualCollectionAmount; }

    public BigDecimal getResidualReceivableAmount() { return residualReceivableAmount; }
    public void setResidualReceivableAmount(BigDecimal residualReceivableAmount) { this.residualReceivableAmount = residualReceivableAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
