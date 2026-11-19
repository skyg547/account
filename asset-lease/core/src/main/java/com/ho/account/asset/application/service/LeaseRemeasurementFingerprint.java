package com.ho.account.asset.application.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

/** v1 fingerprints raw nullable terms, before defaults are resolved from mutable contract state. */
final class LeaseRemeasurementFingerprint {
    private LeaseRemeasurementFingerprint() { }

    static String calculate(LocalDate date, BigDecimal payment, LocalDate end, BigDecimal rate, String actor) {
        String input = "lease-remeasurement-v1" + field(date) + field(payment) + field(end) + field(rate) + field(actor);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private static String field(Object value) {
        if (value == null) return "-1:";
        String text = value instanceof BigDecimal number ? number.stripTrailingZeros().toPlainString() : value.toString();
        return text.length() + ":" + text;
    }
}
