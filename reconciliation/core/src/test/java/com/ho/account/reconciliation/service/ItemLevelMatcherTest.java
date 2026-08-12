package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.domain.ReconciliationItem;
import com.ho.account.reconciliation.service.ItemLevelMatcher.ItemMatchGroup;
import com.ho.account.reconciliation.service.ItemLevelMatcher.MatchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemLevelMatcherTest {

    private ItemLevelMatcher matcher;
    private final LocalDate today = LocalDate.of(2026, 8, 12);

    @BeforeEach
    void setUp() {
        matcher = new ItemLevelMatcher();
    }

    @Test
    @DisplayName("1:1 Exact Match: 동일한 복합 키와 일치하는 금액인 경우 1:1 매칭 성공")
    void testExactOneToOneMatch() {
        ReconciliationItem src = ReconciliationItem.ofSource("S1", today, "REF100", "PARTNER_A", "1110", new BigDecimal("50000"), "Payment A");
        ReconciliationItem tgt = ReconciliationItem.ofTarget("T1", today, "REF100", "PARTNER_A", "1110", new BigDecimal("50000"), "Journal A");

        List<ItemMatchGroup> results = matcher.match(List.of(src), List.of(tgt), BigDecimal.ZERO);

        assertThat(results).hasSize(1);
        ItemMatchGroup group = results.get(0);
        assertThat(group.matchType()).isEqualTo(MatchType.EXACT_1_1);
        assertThat(group.sourceItems()).containsExactly(src);
        assertThat(group.targetItems()).containsExactly(tgt);
        assertThat(group.differenceAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("1:N Match: 1건의 원천 입금(10만 원)과 2건의 대상 전표(6만 원 + 4만 원)가 복합 키 그룹에서 1:N 매칭 성공")
    void testOneToManyMatch() {
        ReconciliationItem src = ReconciliationItem.ofSource("S1", today, "REF200", "PARTNER_B", "1110", new BigDecimal("100000"), "Bulk Deposit");
        ReconciliationItem tgt1 = ReconciliationItem.ofTarget("T1", today, "REF200", "PARTNER_B", "1110", new BigDecimal("60000"), "Invoice B1");
        ReconciliationItem tgt2 = ReconciliationItem.ofTarget("T2", today, "REF200", "PARTNER_B", "1110", new BigDecimal("40000"), "Invoice B2");

        List<ItemMatchGroup> results = matcher.match(List.of(src), List.of(tgt1, tgt2), BigDecimal.ZERO);

        assertThat(results).hasSize(1);
        ItemMatchGroup group = results.get(0);
        assertThat(group.matchType()).isEqualTo(MatchType.ONE_TO_MANY_1_N);
        assertThat(group.sourceItems()).containsExactly(src);
        assertThat(group.targetItems()).containsExactlyInAnyOrder(tgt1, tgt2);
        assertThat(group.sourceTotalAmount()).isEqualByComparingTo("100000");
        assertThat(group.targetTotalAmount()).isEqualByComparingTo("100000");
        assertThat(group.differenceAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("N:1 Match: 2건의 원천 결제(3만 원 + 7만 원)와 1건의 대상 집계 전표(10만 원)가 N:1 매칭 성공")
    void testManyToOneMatch() {
        ReconciliationItem src1 = ReconciliationItem.ofSource("S1", today, "REF300", "PARTNER_C", "1110", new BigDecimal("30000"), "Card 1");
        ReconciliationItem src2 = ReconciliationItem.ofSource("S2", today, "REF300", "PARTNER_C", "1110", new BigDecimal("70000"), "Card 2");
        ReconciliationItem tgt = ReconciliationItem.ofTarget("T1", today, "REF300", "PARTNER_C", "1110", new BigDecimal("100000"), "Settlement C");

        List<ItemMatchGroup> results = matcher.match(List.of(src1, src2), List.of(tgt), BigDecimal.ZERO);

        assertThat(results).hasSize(1);
        ItemMatchGroup group = results.get(0);
        assertThat(group.matchType()).isEqualTo(MatchType.MANY_TO_ONE_N_1);
        assertThat(group.sourceItems()).containsExactlyInAnyOrder(src1, src2);
        assertThat(group.targetItems()).containsExactly(tgt);
        assertThat(group.differenceAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("N:M Match: 2건의 원천 결제(30만 원 + 20만 원 = 50만 원)와 2건의 대상 입금(10만 원 + 40만 원 = 50만 원)이 N:M 매칭 성공")
    void testManyToManySubsetSumMatch() {
        ReconciliationItem src1 = ReconciliationItem.ofSource("S1", today, "REF400", "PARTNER_D", "1110", new BigDecimal("300000"), "Sales D1");
        ReconciliationItem src2 = ReconciliationItem.ofSource("S2", today, "REF400", "PARTNER_D", "1110", new BigDecimal("200000"), "Sales D2");

        ReconciliationItem tgt1 = ReconciliationItem.ofTarget("T1", today, "REF400", "PARTNER_D", "1110", new BigDecimal("100000"), "Deposit D1");
        ReconciliationItem tgt2 = ReconciliationItem.ofTarget("T2", today, "REF400", "PARTNER_D", "1110", new BigDecimal("400000"), "Deposit D2");

        List<ItemMatchGroup> results = matcher.match(List.of(src1, src2), List.of(tgt1, tgt2), BigDecimal.ZERO);

        assertThat(results).hasSize(1);
        ItemMatchGroup group = results.get(0);
        assertThat(group.matchType()).isEqualTo(MatchType.MANY_TO_MANY_N_M);
        assertThat(group.sourceItems()).containsExactlyInAnyOrder(src1, src2);
        assertThat(group.targetItems()).containsExactlyInAnyOrder(tgt1, tgt2);
        assertThat(group.sourceTotalAmount()).isEqualByComparingTo("500000");
        assertThat(group.targetTotalAmount()).isEqualByComparingTo("500000");
        assertThat(group.differenceAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Discrepancy Detection: 대상 데이터가 누락된 경우 MISSING_TARGET으로 적발")
    void testMissingTargetDetection() {
        ReconciliationItem src = ReconciliationItem.ofSource("S1", today, "REF500", "PARTNER_E", "1110", new BigDecimal("150000"), "Unmatched Source");

        List<ItemMatchGroup> results = matcher.match(List.of(src), List.of(), BigDecimal.ZERO);

        assertThat(results).hasSize(1);
        ItemMatchGroup group = results.get(0);
        assertThat(group.matchType()).isEqualTo(MatchType.MISSING_TARGET);
        assertThat(group.sourceItems()).containsExactly(src);
        assertThat(group.targetItems()).isEmpty();
        assertThat(group.differenceAmount()).isEqualByComparingTo("150000");
    }

    @Test
    @DisplayName("Discrepancy Detection: 금액 불일치 시 AMOUNT_MISMATCH로 적발")
    void testAmountMismatchDetection() {
        ReconciliationItem src = ReconciliationItem.ofSource("S1", today, "REF600", "PARTNER_F", "1110", new BigDecimal("100000"), "Source F");
        ReconciliationItem tgt = ReconciliationItem.ofTarget("T1", today, "REF600", "PARTNER_F", "1110", new BigDecimal("90000"), "Target F");

        List<ItemMatchGroup> results = matcher.match(List.of(src), List.of(tgt), BigDecimal.ZERO);

        assertThat(results).hasSize(1);
        ItemMatchGroup group = results.get(0);
        assertThat(group.matchType()).isEqualTo(MatchType.AMOUNT_MISMATCH);
        assertThat(group.sourceTotalAmount()).isEqualByComparingTo("100000");
        assertThat(group.targetTotalAmount()).isEqualByComparingTo("90000");
        assertThat(group.differenceAmount()).isEqualByComparingTo("10000");
    }
}
