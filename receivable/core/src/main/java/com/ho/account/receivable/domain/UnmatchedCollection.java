package com.ho.account.receivable.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 자동 매칭에 실패하여 수동 처리가 필요한 수금 엔티티.
 * 미매칭 큐의 항목으로 사용됩니다.
 */
@Entity
@Table(name = "unmatched_collections")
public class UnmatchedCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false, unique = true)
    private Collection collection; // 매칭되지 않은 수금 정보

    @Column(length = 500)
    private String reason; // 매칭 실패 사유

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private UnmatchedCollectionStatus status; // 미매칭 상태 (PENDING, RESOLVED, IGNORED)

    @Column(length = 50)
    private String resolvedBy; // 해결한 사용자 ID

    private LocalDateTime resolvedAt; // 해결 일시

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = UnmatchedCollectionStatus.PENDING; // 초기 상태는 PENDING
        }
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
