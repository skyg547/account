package com.ho.account.journalledger.domain.journal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 원본 전표와 그 시점의 유효한 역분개 전표를 일대일로 연결하는 작업 Aggregate입니다.
 *
 * <p>원본 전표 ID가 식별자이므로 동일 원본의 순차·동시 요청은 하나의 작업으로 수렴합니다.
 * 취소된 초안만 새 역분개 전표 ID로 재시작할 수 있고, 전기 완료 작업은 최종 상태입니다.</p>
 */
@Entity
@Table(name = "journal_reversal_operations")
public class JournalReversalOperation {

    private static final int MAX_CANCELLATION_REASON_LENGTH = 500;

    /** 작업 식별자이자 역분개 대상 원본 전표 ID입니다. */
    @Id
    @Column(name = "original_journal_entry_id", nullable = false, updatable = false)
    private Long originalJournalEntryId;

    /** 이 작업에서 현재 유효한 역분개 전표 ID입니다. */
    @Column(name = "reversal_journal_entry_id", nullable = false, unique = true)
    private Long reversalJournalEntryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReversalOperationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 50)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = MAX_CANCELLATION_REASON_LENGTH)
    private String cancellationReason;

    protected JournalReversalOperation() {
        // JPA only
    }

    private JournalReversalOperation(Long originalJournalEntryId, Long reversalJournalEntryId) {
        this.originalJournalEntryId = requirePersistentId(originalJournalEntryId, "원본 전표 ID");
        this.reversalJournalEntryId = requirePersistentId(reversalJournalEntryId, "역분개 전표 ID");
        assertDistinctEntries(this.reversalJournalEntryId);
        this.status = ReversalOperationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** 새 원본-역분개 연결을 PENDING 상태로 시작합니다. */
    public static JournalReversalOperation create(Long originalJournalEntryId, Long reversalJournalEntryId) {
        return new JournalReversalOperation(originalJournalEntryId, reversalJournalEntryId);
    }

    /**
     * 취소된 작업을 새 역분개 초안으로 재시작합니다.
     *
     * <p>POSTED 또는 아직 PENDING인 작업의 연결을 바꾸면 중복 경제 효과가 생길 수 있으므로
     * CANCELLED 상태에서만 새 전표 ID를 받을 수 있습니다.</p>
     */
    public void restart(Long newReversalJournalEntryId) {
        if (status != ReversalOperationStatus.CANCELLED) {
            throw new IllegalStateException("취소된 역분개 작업만 다시 시작할 수 있습니다.");
        }
        Long validatedId = requirePersistentId(newReversalJournalEntryId, "새 역분개 전표 ID");
        assertDistinctEntries(validatedId);
        if (Objects.equals(reversalJournalEntryId, validatedId)) {
            throw new IllegalArgumentException("취소된 전표와 다른 새 역분개 전표가 필요합니다.");
        }
        this.reversalJournalEntryId = validatedId;
        this.status = ReversalOperationStatus.PENDING;
        this.postedAt = null;
        this.cancelledAt = null;
        this.cancelledBy = null;
        this.cancellationReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** 현재 연결된 PENDING 역분개가 전기되었음을 확정합니다. */
    public void markPosted(Long postedReversalJournalEntryId) {
        assertCurrentReversal(postedReversalJournalEntryId);
        if (status != ReversalOperationStatus.PENDING) {
            throw new IllegalStateException("진행 중인 역분개 작업만 전기 완료할 수 있습니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        this.status = ReversalOperationStatus.POSTED;
        this.postedAt = now;
        this.updatedAt = now;
    }

    /** 현재 연결된 PENDING 역분개 초안을 취소하고 감사 정보를 남깁니다. */
    public void cancel(Long cancelledReversalJournalEntryId, String actor, String reason) {
        assertCurrentReversal(cancelledReversalJournalEntryId);
        if (status != ReversalOperationStatus.PENDING) {
            throw new IllegalStateException("진행 중인 역분개 작업만 취소할 수 있습니다.");
        }
        String canonicalActor = JournalActor.canonicalize(actor);
        String validatedReason = requireCancellationReason(reason);
        LocalDateTime now = LocalDateTime.now();
        this.status = ReversalOperationStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelledBy = canonicalActor;
        this.cancellationReason = validatedReason;
        this.updatedAt = now;
    }

    private void assertCurrentReversal(Long candidateReversalJournalEntryId) {
        Long validatedId = requirePersistentId(candidateReversalJournalEntryId, "역분개 전표 ID");
        if (!Objects.equals(reversalJournalEntryId, validatedId)) {
            throw new IllegalStateException("현재 역분개 전표와 일치하지 않습니다.");
        }
    }

    private void assertDistinctEntries(Long candidateReversalJournalEntryId) {
        if (Objects.equals(originalJournalEntryId, candidateReversalJournalEntryId)) {
            throw new IllegalArgumentException("원본 전표와 역분개 전표는 달라야 합니다.");
        }
    }

    private static Long requirePersistentId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + "는 저장된 양수 ID여야 합니다.");
        }
        return id;
    }

    private static String requireCancellationReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("역분개 취소 사유는 필수입니다.");
        }
        String normalized = reason.trim();
        if (normalized.length() > MAX_CANCELLATION_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "역분개 취소 사유는 " + MAX_CANCELLATION_REASON_LENGTH + "자를 초과할 수 없습니다.");
        }
        return normalized;
    }

    public Long getOriginalJournalEntryId() {
        return originalJournalEntryId;
    }

    public Long getReversalJournalEntryId() {
        return reversalJournalEntryId;
    }

    public ReversalOperationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }
}
