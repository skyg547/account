package com.ho.account.closing.domain;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable ECL source identity and original posting reconciliation for one ledger group. */
public record EclProvisionSnapshot(
        LocalDate baseDate, long provisionBatchId, String runId, String modelVersion,
        String legalEntityCode, String currencyCode, String allowanceAccountCode,
        BigDecimal targetAllowanceAmount, BigDecimal sourceExposureAmount,
        BigDecimal stage1AllowanceAmount, BigDecimal stage2AllowanceAmount,
        BigDecimal stage3AllowanceAmount, BigDecimal existingTransactionAmount,
        BigDecimal existingBaseAmount, BigDecimal closingRate,
        BigDecimal adjustmentTransactionAmount, BigDecimal adjustmentBaseAmount) {

    public EclProvisionSnapshot {
        Objects.requireNonNull(baseDate);
        if (provisionBatchId <= 0) throw new IllegalArgumentException("provisionBatchId must be positive");
        for (String value : new String[] {runId, modelVersion, legalEntityCode, currencyCode, allowanceAccountCode}) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException("ECL snapshot identity is incomplete");
        }
    }

    /** Batch ID identifies the operation; the reference identifies the confirmed source content. */
    public String operationKey() {
        return digest(baseDate, provisionBatchId, legalEntityCode, currencyCode, allowanceAccountCode);
    }

    public String reference() {
        return "ECLSNAP:" + digest(sourceFingerprint(),
                existingTransactionAmount, existingBaseAmount, closingRate,
                adjustmentTransactionAmount, adjustmentBaseAmount);
    }

    public String sourceFingerprint() {
        return digest(baseDate, runId, modelVersion, legalEntityCode,
                currencyCode, allowanceAccountCode, targetAllowanceAmount,
                sourceExposureAmount, stage1AllowanceAmount, stage2AllowanceAmount, stage3AllowanceAmount);
    }

    private static String digest(Object... values) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            for (Object value : values) {
                String normalized = value instanceof BigDecimal amount
                        ? amount.stripTrailingZeros().toPlainString() : String.valueOf(value);
                byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
                sha.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
                sha.update((byte) ':');
                sha.update(bytes);
            }
            return HexFormat.of().formatHex(sha.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
