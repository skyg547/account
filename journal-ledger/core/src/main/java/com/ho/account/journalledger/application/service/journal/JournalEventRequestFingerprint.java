package com.ho.account.journalledger.application.service.journal;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Canonicalizes the submitted event before mutable journal rules are evaluated. */
public final class JournalEventRequestFingerprint {

    /** Distinct from v1's generated-journal fingerprint for manual and contract commands. */
    public static final int VERSION = 2;

    private JournalEventRequestFingerprint() {
    }

    public static String calculate(Map<String, Object> event, LocalDate accountingDate) {
        if (event == null || accountingDate == null) {
            throw new IllegalArgumentException("event and accountingDate are required");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writeText(output, "journal-event-request-v2");
                writeText(output, accountingDate.toString());
                writeValue(output, event);
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to fingerprint journal event request", exception);
        }
    }

    private static void writeValue(DataOutputStream output, Object value) throws IOException {
        if (value == null) {
            output.writeByte(0);
        } else if (value instanceof String text) {
            output.writeByte(1);
            writeText(output, text);
        } else if (value instanceof Number number) {
            output.writeByte(2);
            try {
                writeText(output, new BigDecimal(number.toString()).stripTrailingZeros().toPlainString());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Event number must be finite", exception);
            }
        } else if (value instanceof Boolean flag) {
            output.writeByte(3);
            output.writeBoolean(flag);
        } else if (value instanceof Map<?, ?> map) {
            output.writeByte(4);
            if (map.keySet().stream().anyMatch(key -> !(key instanceof String))) {
                throw new IllegalArgumentException("Event map keys must be strings");
            }
            List<String> keys = map.keySet().stream().map(String.class::cast).sorted().toList();
            output.writeInt(keys.size());
            for (String key : keys) {
                writeText(output, key);
                writeValue(output, map.get(key));
            }
        } else if (value instanceof List<?> list) {
            output.writeByte(5);
            output.writeInt(list.size());
            for (Object item : list) {
                writeValue(output, item);
            }
        } else {
            // External HTTP/Kafka events are JSON values. Unknown Java objects must not silently
            // receive unstable toString() encodings that could acknowledge changed requests.
            throw new IllegalArgumentException("Unsupported journal event value type: " + value.getClass().getName());
        }
    }

    private static void writeText(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }
}
