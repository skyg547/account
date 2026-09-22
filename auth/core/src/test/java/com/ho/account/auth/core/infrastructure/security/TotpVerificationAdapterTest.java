package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class TotpVerificationAdapterTest {

    // RFC 6238 Appendix B public SHA-1 test secret: ASCII "12345678901234567890".
    private static final byte[] RFC_SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    private static final String RFC_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private static final Instant FIXED_TIME = Instant.ofEpochSecond(59L);

    @Test
    void verifiesSixDigitRfc6238CodeAtFixedClock() throws Exception {
        TotpVerificationAdapter adapter = adapter(true, RFC_SECRET_BASE32, 1);

        assertThat(adapter.verifyOtp("alice", codeForStep(1L))).isTrue();
        assertThat(adapter.requiresOtp("alice")).isTrue();
        assertThat(adapter.requiresOtp("bob")).isFalse();
    }

    @Test
    void acceptsOneAdjacentTimeStepButRejectsOutsideTolerance() throws Exception {
        TotpVerificationAdapter adapter = adapter(true, RFC_SECRET_BASE32, 1);

        assertThat(adapter.verifyOtp("alice", codeForStep(0L))).isTrue();
        assertThat(adapter.verifyOtp("alice", codeForStep(2L))).isTrue();
        assertThat(adapter.verifyOtp("alice", codeForStep(3L))).isFalse();
    }

    @Test
    void rejectsSecondUseOfSameUsernameCodeAndCounter() throws Exception {
        TotpVerificationAdapter adapter = adapter(true, RFC_SECRET_BASE32, 1);
        String currentCode = codeForStep(1L);

        assertThat(adapter.verifyOtp("alice", currentCode)).isTrue();
        assertThat(adapter.verifyOtp("alice", currentCode)).isFalse();
    }

    @Test
    void concurrentUseOfSameCodeHasExactlyOneWinner() throws Exception {
        TotpVerificationAdapter adapter = adapter(true, RFC_SECRET_BASE32, 1);
        String currentCode = codeForStep(1L);
        int contenders = 8;
        ExecutorService executor = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = java.util.stream.IntStream.range(0, contenders)
                    .mapToObj(ignored -> executor.submit(() -> {
                        ready.countDown();
                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                        return adapter.verifyOtp("alice", currentCode);
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(1L);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void acceptsNextCounterAfterCurrentCounterWasConsumed() throws Exception {
        MutableClock clock = new MutableClock(FIXED_TIME);
        TotpVerificationAdapter adapter = new TotpVerificationAdapter(
                properties(true, RFC_SECRET_BASE32, 1), clock);

        assertThat(adapter.verifyOtp("alice", codeForStep(1L))).isTrue();
        clock.setInstant(Instant.ofEpochSecond(89L));
        assertThat(adapter.verifyOtp("alice", codeForStep(2L))).isTrue();
    }

    @Test
    void disabledMissingOrMalformedInputsFailClosed() {
        assertThat(adapter(false, RFC_SECRET_BASE32, 1).verifyOtp("alice", "287082")).isFalse();
        assertThat(adapter(true, RFC_SECRET_BASE32, 1).verifyOtp("bob", "287082")).isFalse();
        assertThat(adapter(true, "NOT-BASE32!", 1).verifyOtp("alice", "287082")).isFalse();
        assertThat(adapter(true, RFC_SECRET_BASE32, 1).verifyOtp("alice", null)).isFalse();
        assertThat(adapter(true, RFC_SECRET_BASE32, 1).verifyOtp("alice", "12345")).isFalse();
        assertThat(adapter(true, RFC_SECRET_BASE32, 1).verifyOtp("alice", "12345x")).isFalse();
    }

    @Test
    void unsafeAlgorithmOrWindowConfigurationFailsClosed() throws Exception {
        AuthModuleProperties properties = properties(true, RFC_SECRET_BASE32, 2);
        properties.getOtp().setAlgorithm("HmacSHA256");
        TotpVerificationAdapter adapter = new TotpVerificationAdapter(
                properties, Clock.fixed(FIXED_TIME, ZoneOffset.UTC));

        assertThat(adapter.verifyOtp("alice", codeForStep(1L))).isFalse();
    }

    private TotpVerificationAdapter adapter(boolean enabled, String secret, int tolerance) {
        return new TotpVerificationAdapter(
                properties(enabled, secret, tolerance), Clock.fixed(FIXED_TIME, ZoneOffset.UTC));
    }

    private AuthModuleProperties properties(boolean enabled, String secret, int tolerance) {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getOtp().setEnabled(enabled);
        properties.getOtp().setToleranceSteps(tolerance);
        properties.getOtp().setUserSecrets(Map.of("alice", secret));
        return properties;
    }

    private String codeForStep(long step) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(RFC_SECRET, "HmacSHA1"));
        byte[] digest = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
        int offset = digest[digest.length - 1] & 0x0f;
        int binary = ((digest[offset] & 0x7f) << 24)
                | ((digest[offset + 1] & 0xff) << 16)
                | ((digest[offset + 2] & 0xff) << 8)
                | (digest[offset + 3] & 0xff);
        return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
    }

    private static final class MutableClock extends Clock {
        private volatile Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
