package com.ho.account.receivable.domain;

import java.time.LocalDateTime;

/**
 * 자동 매칭에 실패하여 수동 처리가 필요한 수금 엔티티 (Pure Java POJO).
 * 미매칭 큐의 항목으로 사용됩니다.
 *
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 도메인 모델(UnmatchedCollection)은 프레임워크 독립성을 가집니다.
 */
public class UnmatchedCollection {

    private Long id;
    private Collection collection;
    private String reason;
    private UnmatchedCollectionStatus status = UnmatchedCollectionStatus.PENDING;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt = LocalDateTime.now();

    public UnmatchedCollection() {
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UnmatchedCollectionStatus getStatus() {
        return status;
    }

    public void setStatus(UnmatchedCollectionStatus status) {
        this.status = status;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
