package com.ho.account.reporting.domain.model;

import java.time.LocalDateTime;

public record RegulatoryFilingReceipt(
        String receiptId,
        LocalDateTime receivedAt,
        String message) {

    public RegulatoryFilingReceipt {
        receiptId = requireText(receiptId, "receiptId is required.");
        if (receivedAt == null) {
            throw new IllegalArgumentException("receivedAt is required.");
        }
        message = message == null ? "" : message.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
