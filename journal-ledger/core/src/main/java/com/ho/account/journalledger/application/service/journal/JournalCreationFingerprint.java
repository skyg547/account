package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.domain.JournalCurrencyConversionPolicy;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Builds the versioned canonical SHA-256 fingerprint used by creation idempotency. */
public final class JournalCreationFingerprint {

    public static final int CURRENT_VERSION = 1;

    private static final byte[] FORMAT_MARKER =
            "journal-creation-fingerprint-v1".getBytes(StandardCharsets.UTF_8);

    private JournalCreationFingerprint() {
    }

    /**
     * Fingerprints only effective persisted financial and audit content.
     *
     * <p>Generated database identifiers, lifecycle status, and timestamps are deliberately absent.
     * Every value is UTF-8 length-prefixed, numeric columns use their fixed persistence scale, and
     * line encodings are sorted as complete byte sequences. Sorting makes caller list order irrelevant
     * while retaining duplicate lines because the list itself is never converted to a set.</p>
     */
    public static String calculate(JournalEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("journalEntry must not be null");
        }
        try {
            ByteArrayOutputStream canonicalBytes = new ByteArrayOutputStream();
            try (DataOutputStream canonical = new DataOutputStream(canonicalBytes)) {
                writeBytes(canonical, FORMAT_MARKER);
                writeText(canonical, toText(entry.getSlipDate()));
                writeText(canonical, toText(
                        entry.getAccountingDate() == null
                                ? entry.getSlipDate()
                                : entry.getAccountingDate()));
                writeText(canonical, entry.getDescription());
                writeText(canonical, effectiveEntryType(entry));
                writeText(canonical, effectiveCurrency(entry));
                writeText(canonical, fixedScale(effectiveExchangeRate(entry), 8));
                writeText(canonical, entry.getCreatedBy());
                writeText(canonical, entry.getLineageSourceType());
                writeText(canonical, entry.getLineageSourceId());

                List<byte[]> lineEncodings = new ArrayList<>();
                for (JournalDetail detail : entry.getDetails()) {
                    lineEncodings.add(encodeLine(detail));
                }
                lineEncodings.sort(Arrays::compareUnsigned);
                canonical.writeInt(lineEncodings.size());
                for (byte[] lineEncoding : lineEncodings) {
                    writeBytes(canonical, lineEncoding);
                }
            }
            return HexFormat.of().formatHex(sha256(canonicalBytes.toByteArray()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode journal creation fingerprint.", exception);
        }
    }

    private static byte[] encodeLine(JournalDetail detail) throws IOException {
        if (detail == null) {
            throw new IllegalArgumentException("journal detail must not be null");
        }
        ByteArrayOutputStream lineBytes = new ByteArrayOutputStream();
        try (DataOutputStream line = new DataOutputStream(lineBytes)) {
            writeText(line, detail.getSide() == null ? null : detail.getSide().name());
            writeText(line, detail.getAccountCode());
            writeText(line, fixedScale(detail.getAmount(), 2));
            writeText(line, fixedScale(detail.getBaseAmount(), 2));
            writeText(line, detail.getDepartmentCode());
            writeText(line, detail.getBusinessPartnerCode());
            writeText(line, detail.getDetailDescription());
        }
        return lineBytes.toByteArray();
    }

    private static String effectiveCurrency(JournalEntry entry) {
        return entry.getCurrencyCode() == null || entry.getCurrencyCode().isBlank()
                ? JournalCurrencyConversionPolicy.BASE_CURRENCY_CODE
                : entry.getCurrencyCode();
    }

    private static String effectiveEntryType(JournalEntry entry) {
        return entry.getEntryType() == null || entry.getEntryType().isBlank()
                ? "NORMAL"
                : entry.getEntryType().trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal effectiveExchangeRate(JournalEntry entry) {
        if (entry.getExchangeRate() == null
                && JournalCurrencyConversionPolicy.BASE_CURRENCY_CODE.equals(effectiveCurrency(entry))) {
            return BigDecimal.ONE;
        }
        return entry.getExchangeRate();
    }

    private static String fixedScale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String toText(Object value) {
        return value == null ? null : value.toString();
    }

    private static void writeText(DataOutputStream output, String value) throws IOException {
        if (value == null) {
            output.writeInt(-1);
            return;
        }
        writeBytes(output, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
