package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.domain.ReconciliationItem;
import com.ho.account.reconciliation.service.ReconciliationMatchingEngine.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationMatchingEngineTest {

    private ReconciliationMatchingEngine matchingEngine;
    private final LocalDate today = LocalDate.of(2026, 8, 12);

    @BeforeEach
    void setUp() {
        ItemLevelMatcher matcher = new ItemLevelMatcher();
        matchingEngine = new ReconciliationMatchingEngine(matcher);
    }

    @Test
    @DisplayName("MatchingEngine: 매칭 결과 집계 및 감사 추적 그룹(Matched / Discrepancy) 올바르게 분류")
    void testMatchingEngineExecutionResult() {
        // Matched pair (1:1)
        ReconciliationItem srcMatched = ReconciliationItem.ofSource("S1", today, "REF1", "PARTNER_1", "1110", new BigDecimal("50000"), "Exact 1");
        ReconciliationItem tgtMatched = ReconciliationItem.ofTarget("T1", today, "REF1", "PARTNER_1", "1110", new BigDecimal("50000"), "Exact 1");

        // Discrepancy item (Unmatched Source)
        ReconciliationItem srcUnmatched = ReconciliationItem.ofSource("S2", today, "REF2", "PARTNER_2", "1110", new BigDecimal("30000"), "Unmatched 2");

        ExecutionResult result = matchingEngine.matchItems(
                List.of(srcMatched, srcUnmatched),
                List.of(tgtMatched),
                BigDecimal.ZERO
        );

        assertThat(result.totalSourceCount()).isEqualTo(2);
        assertThat(result.totalSourceAmount()).isEqualByComparingTo("80000");
        assertThat(result.totalTargetCount()).isEqualTo(1);
        assertThat(result.totalTargetAmount()).isEqualByComparingTo("50000");

        assertThat(result.matchedItemsCount()).isEqualTo(1);
        assertThat(result.matchedAmount()).isEqualByComparingTo("50000");
        assertThat(result.matchedGroups()).hasSize(1);

        assertThat(result.unmatchedItemsCount()).isEqualTo(1);
        assertThat(result.unmatchedAmount()).isEqualByComparingTo("30000");
        assertThat(result.discrepancyGroups()).hasSize(1);
    }
}
