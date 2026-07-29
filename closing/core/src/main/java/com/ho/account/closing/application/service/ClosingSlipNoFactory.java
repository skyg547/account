package com.ho.account.closing.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

final class ClosingSlipNoFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int HASH_LENGTH = 9;

    private ClosingSlipNoFactory() {
    }

    static String fxValuation(LocalDate valuationDate, String accountCode, Long batchId) {
        return build("FXV", valuationDate, accountCode, batchId);
    }

    static String eclProvision(LocalDate closingDate, String allowanceAccountCode, Long batchId) {
        return build("ECL", closingDate, allowanceAccountCode, batchId);
    }

    static String annualClosing(LocalDate closingDate, int year, String retainedEarningsAccountCode) {
        return build(
                "ACL",
                closingDate,
                year + "|" + retainedEarningsAccountCode,
                (long) year);
    }

    private static String build(String prefix, LocalDate date, String discriminator, Long batchId) {
        Objects.requireNonNull(date, "date must not be null");
        if (discriminator == null || discriminator.isBlank()) {
            throw new IllegalArgumentException("discriminator must not be blank");
        }
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException("batchId must be positive");
        }

        return prefix
                + date.format(DATE_FORMATTER)
                + hashPart(batchId + "|" + discriminator.trim());
    }

    private static String hashPart(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest)
                    .substring(0, HASH_LENGTH)
                    .toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
