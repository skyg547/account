package com.ho.account.journalledger.domain.journal.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** Durable link from one creation request identity and fingerprint to its single journal entry. */
public class JournalCreationOperation {

    private Long id;

    private String sourceType;

    private String sourceId;

    private String operationKey;

    private int fingerprintVersion;

    private String requestFingerprint;

    private Long journalEntryId;

    private LocalDateTime createdAt;

    protected JournalCreationOperation() {
        // JPA construction; persistence metadata lives in infrastructure XML
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
