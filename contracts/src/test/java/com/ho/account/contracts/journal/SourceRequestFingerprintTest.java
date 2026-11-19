package com.ho.account.contracts.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SourceRequestFingerprintTest {
    @Test
    void objectIdentityAndFloatingPointAreNotAcceptedAsCanonicalSourceFields() {
        assertThatThrownBy(() -> SourceRequestFingerprint.calculate(new Object()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SourceRequestFingerprint.calculate(0.1d))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decimalScaleIsCanonicalWhileAmountsAndTargetsRemainBound() {
        String original = SourceRequestFingerprint.calculate("match-v1", 20L, 30L, new BigDecimal("10.00"));
        assertThat(SourceRequestFingerprint.calculate("match-v1", 20L, 30L, new BigDecimal("10.000")))
                .isEqualTo(original);
        assertThat(SourceRequestFingerprint.calculate("match-v1", 20L, 31L, new BigDecimal("10.00")))
                .isNotEqualTo(original);
        assertThat(SourceRequestFingerprint.calculate("match-v1", 20L, 30L, new BigDecimal("10.01")))
                .isNotEqualTo(original);
    }

    @Test
    void fieldFramingPreservesSeparatorsNullsOrderAndVersion() {
        assertThat(SourceRequestFingerprint.calculate("v1", "a:b", "c"))
                .isNotEqualTo(SourceRequestFingerprint.calculate("v1", "a", "b:c"));
        assertThat(SourceRequestFingerprint.calculate("v1", (Object) null))
                .isNotEqualTo(SourceRequestFingerprint.calculate("v1", ""));
        assertThat(SourceRequestFingerprint.calculate("v1", "a", "b"))
                .isNotEqualTo(SourceRequestFingerprint.calculate("v1", "b", "a"))
                .isNotEqualTo(SourceRequestFingerprint.calculate("v2", "a", "b"));
    }
}
