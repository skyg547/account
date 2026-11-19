package com.ho.account.contracts.journal;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

/** Canonical content binding, never an operation identity. Callers supply every immutable request field. */
public final class SourceRequestFingerprint {
    private SourceRequestFingerprint() { }

    public static String calculate(Object... fields) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeUTF("source-request-v1");
                output.writeInt(fields.length);
                for (Object field : fields) {
                    if (field == null) {
                        output.writeInt(-1);
                    } else {
                        if (!(field instanceof String || field instanceof BigDecimal || field instanceof Long
                                || field instanceof Integer || field instanceof LocalDate || field instanceof Boolean)) {
                            throw new IllegalArgumentException("Source fingerprint fields must be immutable supported scalars");
                        }
                        String value = field instanceof BigDecimal amount
                                ? amount.stripTrailingZeros().toPlainString() : field.toString();
                        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
                        output.writeInt(encoded.length);
                        output.write(encoded);
                    }
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot fingerprint source request", exception);
        }
    }
}
