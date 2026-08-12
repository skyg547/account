package com.ho.account.receivable.infrastructure.persistence.entity;

import com.ho.account.receivable.domain.UnmatchedCollectionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UnmatchedCollection JPA 영속성 엔티티.
 * DB `unmatched_collections` 테이블과 매핑되며, 도메인 POJO(UnmatchedCollection)와 분리하여 관리합니다.
 */
@Entity
@Table(name = "unmatched_collections")
public class UnmatchedCollectionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false, unique = true)
    private CollectionJpaEntity collection;

    @Column(length = 500)
    private String reason;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private UnmatchedCollectionStatus status;

    @Column(length = 50)
    private String resolvedBy;

    private LocalDateTime resolvedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = UnmatchedCollectionStatus.PENDING;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CollectionJpaEntity getCollection() { return collection; }
    public void setCollection(CollectionJpaEntity collection) { this.collection = collection; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public UnmatchedCollectionStatus getStatus() { return status; }
    public void setStatus(UnmatchedCollectionStatus status) { this.status = status; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
