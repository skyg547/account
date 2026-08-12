package com.ho.account.receivable.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 한 수금액이 어떤 채권에 얼마만큼 배분되었는지 기록하는 감사 이력 엔티티 (Pure Java POJO).
 *
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 도메인 모델(CollectionAllocation)은 JPA 의존성을 제거하여 기술 프레임워크 독립성을 확보합니다.
 */
public class CollectionAllocation {

    private Long id;
    private Collection collection;
    private Receivable receivable;
    private BigDecimal matchedAmount;
    private BigDecimal residualCollectionAmount;
    private BigDecimal residualReceivableAmount;
    private LocalDateTime createdAt = LocalDateTime.now();

    public CollectionAllocation() {
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Collection getCollection() { return collection; }
    public void setCollection(Collection collection) { this.collection = collection; }

    public Receivable getReceivable() { return receivable; }
    public void setReceivable(Receivable receivable) { this.receivable = receivable; }

    public BigDecimal getMatchedAmount() { return matchedAmount; }
    public void setMatchedAmount(BigDecimal matchedAmount) { this.matchedAmount = matchedAmount; }

    public BigDecimal getResidualCollectionAmount() { return residualCollectionAmount; }
    public void setResidualCollectionAmount(BigDecimal residualCollectionAmount) { this.residualCollectionAmount = residualCollectionAmount; }

    public BigDecimal getResidualReceivableAmount() { return residualReceivableAmount; }
    public void setResidualReceivableAmount(BigDecimal residualReceivableAmount) { this.residualReceivableAmount = residualReceivableAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
