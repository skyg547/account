package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.OtpVerificationPort;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local-profile RFC 6238 verifier backed by per-user Base32 secrets supplied at runtime.
 * Production deployments must provide a provider adapter with cluster-wide atomic replay protection.
 */
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "auth.otp", name = "enabled", havingValue = "true")
public class TotpVerificationAdapter implements OtpVerificationPort {

    private final AuthModuleProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, Long> lastAcceptedCounters = new ConcurrentHashMap<>();

    public TotpVerificationAdapter(AuthModuleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean verifyOtp(String username, String otpCode) {
        AuthModuleProperties.Otp otp = properties.getOtp();
        if (!otp.isEnabled()
                || !hasSafeAlgorithmConfiguration(otp)
                || username == null || otpCode == null
                || !otpCode.matches("\\d{" + otp.getDigits() + "}")) {
            return false;
        }

        String encodedSecret = otp.getUserSecrets().get(username.trim());
        if (encodedSecret == null || encodedSecret.isBlank()) {
            return false;
        }

        try {
            byte[] secret = decodeBase32(encodedSecret);
            long currentStep = Math.floorDiv(clock.instant().getEpochSecond(), otp.getPeriodSeconds());
            byte[] suppliedCode = otpCode.getBytes(StandardCharsets.US_ASCII);
            long highestMatchingCounter = -1L;
            for (int offset = -otp.getToleranceSteps(); offset <= otp.getToleranceSteps(); offset++) {
                long candidateStep = currentStep + offset;
                if (candidateStep >= 0) {
                    byte[] expectedCode = generateCode(secret, candidateStep, otp)
                            .getBytes(StandardCharsets.US_ASCII);
                    if (MessageDigest.isEqual(expectedCode, suppliedCode)) {
                        highestMatchingCounter = Math.max(highestMatchingCounter, candidateStep);
                    }
                }
            }
            if (highestMatchingCounter < 0) {
                return false;
            }
            return consumeSuccessfulCounter(username.trim(), highestMatchingCounter);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            // Malformed runtime secrets and unavailable algorithms must never bypass the second factor.
            return false;
        }
    }

    @Override
    public boolean requiresOtp(String username) {
        return properties.getOtp().isEnabled()
                && username != null
                && properties.getOtp().getUserSecrets().containsKey(username.trim());
    }

    private String generateCode(byte[] secret, long counter, AuthModuleProperties.Otp otp)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance(otp.getAlgorithm());
        mac.init(new SecretKeySpec(secret, otp.getAlgorithm()));
        byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
        int offset = digest[digest.length - 1] & 0x0f;
        int binary = ((digest[offset] & 0x7f) << 24)
                | ((digest[offset + 1] & 0xff) << 16)
                | ((digest[offset + 2] & 0xff) << 8)
                | (digest[offset + 3] & 0xff);
        int code = binary % 1_000_000;
        return String.format(Locale.ROOT, "%06d", code);
    }

    private byte[] decodeBase32(String encoded) {
        String normalized = encoded.trim().toUpperCase(Locale.ROOT);
        int paddingStart = normalized.indexOf('=');
        if (paddingStart >= 0) {
            String padding = normalized.substring(paddingStart);
            if (!padding.chars().allMatch(character -> character == '=')) {
                throw new IllegalArgumentException("Invalid Base32 padding");
            }
            normalized = normalized.substring(0, paddingStart);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Base32 input must not be empty");
        }

        byte[] decoded = new byte[(normalized.length() * 5) / 8];
        int buffer = 0;
        int bitsInBuffer = 0;
        int outputIndex = 0;
        for (int index = 0; index < normalized.length(); index++) {
            int value = base32Value(normalized.charAt(index));
            buffer = (buffer << 5) | value;
            bitsInBuffer += 5;
            if (bitsInBuffer >= 8) {
                decoded[outputIndex++] = (byte) (buffer >> (bitsInBuffer - 8));
                bitsInBuffer -= 8;
                buffer &= (1 << bitsInBuffer) - 1;
            }
        }
        if (outputIndex == 0) {
            throw new IllegalArgumentException("Base32 input is too short");
        }
        return decoded;
    }

    private boolean hasSafeAlgorithmConfiguration(AuthModuleProperties.Otp otp) {
        return "HmacSHA1".equals(otp.getAlgorithm())
                && otp.getDigits() == 6
                && otp.getPeriodSeconds() > 0
                && otp.getToleranceSteps() >= 0
                && otp.getToleranceSteps() <= 1;
    }

    private boolean consumeSuccessfulCounter(String username, long matchingCounter) {
        AtomicBoolean consumed = new AtomicBoolean(false);
        // ConcurrentHashMap.compute serializes each user's update, so concurrent replays yield one success.
        // This local-only adapter is process-scoped; production requires a provider/shared atomic counter store.
        lastAcceptedCounters.compute(username, (ignored, lastAcceptedCounter) -> {
            if (lastAcceptedCounter == null || matchingCounter > lastAcceptedCounter) {
                consumed.set(true);
                return matchingCounter;
            }
            return lastAcceptedCounter;
        });
        return consumed.get();
    }

    private int base32Value(char character) {
        if (character >= 'A' && character <= 'Z') {
            return character - 'A';
        }
        if (character >= '2' && character <= '7') {
            return character - '2' + 26;
        }
        throw new IllegalArgumentException("Invalid Base32 input");
    }
}
