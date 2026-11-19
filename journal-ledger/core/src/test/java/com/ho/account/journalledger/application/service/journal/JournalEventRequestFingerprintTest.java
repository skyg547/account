package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JournalEventRequestFingerprintTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 28);

    @Test
    void nestedMapOrderAndNumericScaleDoNotChangeTheSubmittedEvent() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("amount", new BigDecimal("10.00"));
        first.put("dimensions", Map.of("branch", "SEOUL", "weights", List.of(1, new BigDecimal("2.0"))));
        Map<String, Object> reordered = new LinkedHashMap<>();
        reordered.put("dimensions", Map.of("weights", List.of(new BigDecimal("1.00"), 2), "branch", "SEOUL"));
        reordered.put("amount", 10);

        assertThat(JournalEventRequestFingerprint.calculate(first, DATE))
                .isEqualTo(JournalEventRequestFingerprint.calculate(reordered, DATE));
        assertThat(JournalEventRequestFingerprint.calculate(first, DATE.plusDays(1)))
                .isNotEqualTo(JournalEventRequestFingerprint.calculate(first, DATE));
        assertThat(JournalEventRequestFingerprint.calculate(Map.of("amount", 11), DATE))
                .isNotEqualTo(JournalEventRequestFingerprint.calculate(Map.of("amount", 10), DATE));
    }

    @Test
    void unsupportedValuesFailClosed() {
        assertThatThrownBy(() -> JournalEventRequestFingerprint.calculate(
                Map.of("amount", new Object()), DATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported journal event value type");
    }
}
