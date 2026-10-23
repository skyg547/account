package com.ho.account.journalledger.domain.journal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

/** Durable link from one creation request identity and fingerprint to its single journal entry. */
@Entity
@Table(
        name = "journal_creation_operations",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_journal_creation_operation_identity",
                    columnNames = {"source_type", "source_id", "operation_key"})
        })
public class JournalCreationOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, updatable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false, updatable = false, length = 100)
    private String sourceId;

    @Column(name = "operation_key", nullable = false, updatable = false, length = 100)
    private String operationKey;

    @Column(name = "fingerprint_version", nullable = false, updatable = false)
    private int fingerprintVersion;

    @Column(name = "request_fingerprint", nullable = false, updatable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "journal_entry_id", nullable = false, updatable = false, unique = true)
    private Long journalEntryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected JournalCreationOperation() {
        // JPA only
    }

    private JournalCreationOperation(
            String sourceType,
            String sourceId,
            String operationKey,
            int fingerprintVersion,
            String requestFingerprint,
            Long journalEntryId) {
        this.sourceType = requireText(sourceType, "sourceType", 50);
        this.sourceId = requireText(sourceId, "sourceId", 100);
        this.operationKey = requireText(operationKey, "operationKey", 100);
        if (fingerprintVersion <= 0) {
            throw new IllegalArgumentException("fingerprintVersion must be positive");
        }
        this.fingerprintVersion = fingerprintVersion;
        this.requestFingerprint = requireFingerprint(requestFingerprint);
        if (journalEntryId == null || journalEntryId <= 0) {
            throw new IllegalArgumentException("journalEntryId must be a persisted positive ID");
        }
        this.journalEntryId = journalEntryId;
        this.createdAt = LocalDateTime.now();
    }

    public static JournalCreationOperation create(
            String sourceType,
            String sourceId,
            String operationKey,
            int fingerprintVersion,
            String requestFingerprint,
            Long journalEntryId) {
        return new JournalCreationOperation(
                sourceType,
                sourceId,
                operationKey,
                fingerprintVersion,
                requestFingerprint,
                journalEntryId);
    }

    /** Both version and digest are part of equality so future canonical forms never compare silently. */
    public boolean matchesFingerprint(int candidateVersion, String candidateFingerprint) {
        return fingerprintVersion == candidateVersion
                && Objects.equals(requestFingerprint, candidateFingerprint);
    }

    private static String requireText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maximumLength + " characters");
        }
        return normalized;
    }

    private static String requireFingerprint(String fingerprint) {
        if (fingerprint == null || !fingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("requestFingerprint must be a lowercase SHA-256 digest");
        }
        return fingerprint;
    }

    public Long getId() {
        return id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getOperationKey() {
        return operationKey;
    }

    public int getFingerprintVersion() {
        return fingerprintVersion;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
