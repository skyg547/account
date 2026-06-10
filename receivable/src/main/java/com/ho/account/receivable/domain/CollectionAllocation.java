package com.ho.account.receivable.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 한 수금액이 어떤 채권에 얼마만큼 배분되었는지 기록하는 감사 이력입니다.
 *
 * <p>부분 매칭 후 양쪽 잔액도 함께 저장하여 "입금액 중 얼마가 남았고 채권은 얼마가
 * 남았는지"를 당시 상태 그대로 추적할 수 있습니다.</p>
 */
@Entity
@Table(name = "collection_allocations")
public class CollectionAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal residualCollectionAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal residualReceivableAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CollectionAllocation() {
    }

    public static CollectionAllocation record(
            Collection collection,
            Receivable receivable,
            BigDecimal matchedAmount) {
        CollectionAllocation allocation = new CollectionAllocation();
        allocation.collection = collection;
        allocation.receivable = receivable;
        allocation.matchedAmount = matchedAmount;
        allocation.residualCollectionAmount = collection.getUnallocatedAmount();
        allocation.residualReceivableAmount = receivable.getOutstandingAmount();
        return allocation;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Collection getCollection() { return collection; }
    public Receivable getReceivable() { return receivable; }
    public BigDecimal getMatchedAmount() { return matchedAmount; }
    public BigDecimal getResidualCollectionAmount() { return residualCollectionAmount; }
    public BigDecimal getResidualReceivableAmount() { return residualReceivableAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
