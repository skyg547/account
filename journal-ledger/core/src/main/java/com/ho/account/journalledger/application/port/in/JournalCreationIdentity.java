package com.ho.account.journalledger.application.port.in;

import java.util.Map;

/**
 * Durable identity of one journal-creation operation.
 *
 * <p>Lineage remains a one-to-many audit relationship. The additional operation key identifies one
 * creation within that lineage, so legitimate multi-journal source events do not become accidental
 * duplicates.</p>
 */
public record JournalCreationIdentity(String sourceType, String sourceId, String operationKey) {

    public static final String PRIMARY_OPERATION_KEY = "PRIMARY";

    private static final int MAX_SOURCE_TYPE_LENGTH = 50;
    private static final int MAX_SOURCE_ID_LENGTH = 100;
    private static final int MAX_OPERATION_KEY_LENGTH = 100;

    public JournalCreationIdentity {
        sourceType = requireText(sourceType, "sourceType", MAX_SOURCE_TYPE_LENGTH);
        sourceId = requireText(sourceId, "sourceId", MAX_SOURCE_ID_LENGTH);
        operationKey = requireText(operationKey, "operationKey", MAX_OPERATION_KEY_LENGTH);
    }

    public static JournalCreationIdentity primary(String sourceType, String sourceId) {
        return new JournalCreationIdentity(sourceType, sourceId, PRIMARY_OPERATION_KEY);
    }

    /** Resolve the source's stable business event/version identity before rule evaluation or quarantine. */
    public static JournalCreationIdentity fromEvent(Map<String, Object> event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        return new JournalCreationIdentity(
                firstStableScalar(event, "lineageSourceType", "eventType", "ruleCode"),
                firstStableScalar(event, "lineageSourceId", "eventId", "sourceId", "documentId", "assetCode", "contractNo"),
                firstStableScalar(event, "journalOperationKey", "operationKey"));
    }

    private static String firstStableScalar(Map<String, Object> event, String... keys) {
        for (String key : keys) {
            Object value = event.get(key);
            if (value == null) continue;
            if (!(value instanceof String) && !(value instanceof Number)) {
                throw new IllegalArgumentException(key + " must be a string or number");
            }
            if (!value.toString().isBlank()) return value.toString();
        }
        return null;
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
}
