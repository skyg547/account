package com.ho.account.closing.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

final class ClosingSlipNoFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int BATCH_SUFFIX_LENGTH = 4;
    private static final int DISCRIMINATOR_HASH_LENGTH = 5;

    private ClosingSlipNoFactory() {
    }

    static String fxValuation(LocalDate valuationDate, String accountCode, Long batchId) {
        return build("FXV", valuationDate, accountCode, batchId);
    }

    static String eclProvision(LocalDate closingDate, String allowanceAccountCode, Long batchId) {
        return build("ECL", closingDate, allowanceAccountCode, batchId);
    }

    private static String build(String prefix, LocalDate date, String discriminator, Long batchId) {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(batchId, "batchId must not be null");

        return prefix
                + date.format(DATE_FORMATTER)
                + suffix(batchId)
                + hashPart(discriminator);
    }

    private static String suffix(Long batchId) {
        String value = Long.toUnsignedString(batchId);
        if (value.length() > BATCH_SUFFIX_LENGTH) {
            return value.substring(value.length() - BATCH_SUFFIX_LENGTH);
        }
        return "0".repeat(BATCH_SUFFIX_LENGTH - value.length()) + value;
    }

    private static String hashPart(String discriminator) {
        String value = discriminator == null ? "" : discriminator.trim();
        String hash = Integer.toHexString(value.hashCode()).toUpperCase(Locale.ROOT);
        if (hash.length() > DISCRIMINATOR_HASH_LENGTH) {
            return hash.substring(hash.length() - DISCRIMINATOR_HASH_LENGTH);
        }
        return "0".repeat(DISCRIMINATOR_HASH_LENGTH - hash.length()) + hash;
    }
}