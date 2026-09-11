package com.ho.account.journalledger.domain.unsettled;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UnsettledItemTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0.999", "0.001", "100000000000000000.00", "0", "-0.01", "1.01"})
    void invalidNewAmountLeavesOpenAndPreviouslySettledItemsUnchanged(String amount) {
        for (UnsettledItem item : List.of(openItem("1.00"), partiallySettledItem())) {
            SettlementState before = SettlementState.from(item);

            assertThatIllegalArgumentException().isThrownBy(
                    () -> item.settle(decimalOrNull(amount), "new-collector", "NEW-REF"));

            assertThat(SettlementState.from(item)).isEqualTo(before);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0.999", "0.001", "100000000000000000.00", "0", "-0.01", "1.01"})
    void invalidNewAmountDoesNotInitializeMissingReferenceCollection(String amount) {
        UnsettledItem item = partiallySettledItem();
        item.setSettlementReferences(null);
        SettlementState before = SettlementState.from(item);

        assertThatIllegalArgumentException().isThrownBy(
                () -> item.settle(decimalOrNull(amount), "new-collector", "NEW-REF"));

        assertThat(SettlementState.from(item)).isEqualTo(before);
    }

    @Test
    void newAmountPrecisionOverflowIsRejectedEvenWhenRemainingBalanceIsSufficient() {
        // 잔액 초과 검사가 정밀도 누락을 가리지 않도록 합성 범위 초과 잔액을 사용합니다.
        UnsettledItem item = openItem("100000000000000000.00");
        SettlementState before = SettlementState.from(item);

        assertThatIllegalArgumentException().isThrownBy(
                () -> item.settle(new BigDecimal("100000000000000000.00"), "collector", "NEW-REF"));

        assertThat(SettlementState.from(item)).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource({
            "99999999999999999.99, 1.00",
            "0.001, 1.00",
            "-0.02, 1.00",
            "0.00, 1.001",
            "0.00, 100000000000000001.00"
    })
    void invalidNextAmountsAreRejectedBeforeEitherAmountOrMetadataChanges(String settled, String remaining) {
        // 합성 부정합 상태로 검증 순서만 확인하며 기존 데이터 복구를 수행하는 테스트는 아닙니다.
        UnsettledItem item = partiallySettledItem();
        item.setSettledAmount(new BigDecimal(settled));
        item.setRemainingAmount(new BigDecimal(remaining));
        SettlementState before = SettlementState.from(item);

        assertThatIllegalArgumentException().isThrownBy(
                () -> item.settle(new BigDecimal("0.01"), "new-collector", "NEW-REF"));

        assertThat(SettlementState.from(item)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1.00", "1.000", "99999999999999999.99"})
    void exactlyRepresentableAmountsNormalizeAndClear(String amount) {
        BigDecimal expected = new BigDecimal(amount).setScale(2);
        UnsettledItem item = openItem(expected.toPlainString());

        item.settle(new BigDecimal(amount), " collector ", " EXACT-REF ");

        assertThat(item.getSettledAmount()).isEqualTo(expected);
        assertThat(item.getRemainingAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(item.getOriginalAmount()).isEqualTo(expected);
        assertThat(item.getStatus()).isEqualTo("CLEARED");
        assertThat(item.isResolved()).isTrue();
        assertThat(item.getSettlementReferences()).containsExactly("EXACT-REF");
        assertThat(item.getLastSettlementReference()).isEqualTo("EXACT-REF");
        assertThat(item.getLastSettledBy()).isEqualTo("collector");
        assertThat(item.getLastSettledAt()).isNotNull();
    }

    @Test
    void oneCentThenRemainderClearsWithoutRounding() {
        UnsettledItem item = openItem("1.00");
        item.setSettlementReferences(null);

        item.settle(new BigDecimal("0.01"), "first-collector", "FIRST-REF");

        assertThat(item.getSettledAmount()).isEqualTo(new BigDecimal("0.01"));
        assertThat(item.getRemainingAmount()).isEqualTo(new BigDecimal("0.99"));
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        assertThat(item.isResolved()).isFalse();

        item.settle(new BigDecimal("0.99"), "last-collector", "LAST-REF");

        assertThat(item.getSettledAmount()).isEqualTo(new BigDecimal("1.00"));
        assertThat(item.getRemainingAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(item.getStatus()).isEqualTo("CLEARED");
        assertThat(item.isResolved()).isTrue();
        assertThat(item.getSettlementReferences()).containsExactly("FIRST-REF", "LAST-REF");
        assertThat(item.getLastSettlementReference()).isEqualTo("LAST-REF");
        assertThat(item.getLastSettledBy()).isEqualTo("last-collector");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0.999", "0.001", "100000000000000000.00", "0", "-0.01", "1.01"})
    void knownReferenceReplayPrecedesNewAmountValidationAndLeavesAllStateUnchanged(String amount) {
        UnsettledItem partial = partiallySettledItem();
        partial.settle(new BigDecimal("0.01"), "second-collector", "SECOND-REF");
        UnsettledItem cleared = partiallySettledItem();
        cleared.settle(new BigDecimal("0.99"), "last-collector", "LAST-REF");
        UnsettledItem legacy = partiallySettledItem();
        legacy.getSettlementReferences().clear();
        UnsettledItem legacyWithoutCollection = partiallySettledItem();
        legacyWithoutCollection.setSettlementReferences(null);

        for (UnsettledItem item : List.of(partial, cleared, legacy, legacyWithoutCollection)) {
            SettlementState before = SettlementState.from(item);

            item.settle(decimalOrNull(amount), "different-collector", " FIRST-REF ");

            assertThat(SettlementState.from(item)).isEqualTo(before);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void actorIsRequiredEvenForKnownReferenceReplay(String actor) {
        UnsettledItem item = partiallySettledItem();
        SettlementState before = SettlementState.from(item);

        assertThatIllegalArgumentException().isThrownBy(
                () -> item.settle(null, actor, "FIRST-REF"));

        assertThat(SettlementState.from(item)).isEqualTo(before);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void referenceIsRequiredWithoutChangingState(String reference) {
        UnsettledItem item = partiallySettledItem();
        SettlementState before = SettlementState.from(item);

        assertThatIllegalArgumentException().isThrownBy(
                () -> item.settle(new BigDecimal("0.01"), "collector", reference));

        assertThat(SettlementState.from(item)).isEqualTo(before);
    }

    private static UnsettledItem openItem(String originalAmount) {
        UnsettledItem item = new UnsettledItem();
        item.setOriginalAmount(new BigDecimal(originalAmount));
        item.setSettledAmount(new BigDecimal("0.00"));
        item.setRemainingAmount(new BigDecimal(originalAmount));
        item.setStatus("OPEN");
        return item;
    }

    private static UnsettledItem partiallySettledItem() {
        UnsettledItem item = openItem("1.00");
        item.settle(new BigDecimal("0.01"), "first-collector", "FIRST-REF");
        item.setLastSettledAt(LocalDateTime.of(2026, 9, 1, 12, 0));
        return item;
    }

    private static BigDecimal decimalOrNull(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private record SettlementState(BigDecimal original, BigDecimal settled, BigDecimal remaining,
                                   String status, boolean resolved, Set<String> references,
                                   String lastReference, String actor, LocalDateTime settledAt) {
        private static SettlementState from(UnsettledItem item) {
            // 원본 Set을 보관하면 내부 변경을 놓치므로 참조번호 내용까지 복사합니다.
            Set<String> references = item.getSettlementReferences() == null
                    ? null : new LinkedHashSet<>(item.getSettlementReferences());
            return new SettlementState(item.getOriginalAmount(), item.getSettledAmount(),
                    item.getRemainingAmount(), item.getStatus(), item.isResolved(), references,
                    item.getLastSettlementReference(), item.getLastSettledBy(), item.getLastSettledAt());
        }
    }

    @Test
    void repeatedSettlementReferenceDoesNotApplyAmountTwice() {
        UnsettledItem item = new UnsettledItem();
        item.setOriginalAmount(new BigDecimal("100.00"));
        item.setSettledAmount(BigDecimal.ZERO);
        item.setRemainingAmount(new BigDecimal("100.00"));

        item.settle(new BigDecimal("40.00"), "collector", "BANK-TX-1");
        item.settle(new BigDecimal("40.00"), "collector", "BANK-TX-1");

        assertThat(item.getSettledAmount()).isEqualByComparingTo("40.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("60.00");
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        assertThat(item.getLastSettledBy()).isEqualTo("collector");
        assertThat(item.getLastSettlementReference()).isEqualTo("BANK-TX-1");
        assertThat(item.getSettlementReferences()).containsExactly("BANK-TX-1");
    }

    @Test
    void multiStepPartialSettlementWithDistinctReferences() {
        UnsettledItem item = new UnsettledItem();
        item.setOriginalAmount(new BigDecimal("100.00"));
        item.setSettledAmount(BigDecimal.ZERO);
        item.setRemainingAmount(new BigDecimal("100.00"));

        item.settle(new BigDecimal("40.00"), "collector-1", "TXN-001");
        assertThat(item.getSettledAmount()).isEqualByComparingTo("40.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("60.00");
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        assertThat(item.getLastSettlementReference()).isEqualTo("TXN-001");
        assertThat(item.getSettlementReferences()).containsExactly("TXN-001");

        item.settle(new BigDecimal("35.00"), "collector-2", "TXN-002");
        assertThat(item.getSettledAmount()).isEqualByComparingTo("75.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("25.00");
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        assertThat(item.getLastSettlementReference()).isEqualTo("TXN-002");
        assertThat(item.getSettlementReferences()).containsExactly("TXN-001", "TXN-002");
    }

    @Test
    void delayedOutOfOrderReplayOfEarlierSettlementReferenceIsIdempotent() {
        UnsettledItem item = new UnsettledItem();
        item.setOriginalAmount(new BigDecimal("100.00"));
        item.setSettledAmount(BigDecimal.ZERO);
        item.setRemainingAmount(new BigDecimal("100.00"));

        // Step 1: First partial settlement
        item.settle(new BigDecimal("40.00"), "collector-1", "TXN-001");
        // Step 2: Second partial settlement (lastSettlementReference becomes TXN-002)
        item.settle(new BigDecimal("35.00"), "collector-2", "TXN-002");

        assertThat(item.getSettledAmount()).isEqualByComparingTo("75.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("25.00");
        assertThat(item.getLastSettlementReference()).isEqualTo("TXN-002");

        // Step 3: Delayed out-of-order replay of TXN-001
        item.settle(new BigDecimal("40.00"), "collector-1", "TXN-001");

        // Must be a no-op: must NOT double-deduct from remainingAmount or inflate settledAmount
        assertThat(item.getSettledAmount()).isEqualByComparingTo("75.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("25.00");
        assertThat(item.getStatus()).isEqualTo("PARTIAL");
        assertThat(item.getLastSettlementReference()).isEqualTo("TXN-002");
        assertThat(item.getSettlementReferences()).containsExactly("TXN-001", "TXN-002");

        // Step 4: Final clearance with TXN-003
        item.settle(new BigDecimal("25.00"), "collector-3", "TXN-003");
        assertThat(item.getSettledAmount()).isEqualByComparingTo("100.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(item.getStatus()).isEqualTo("CLEARED");
        assertThat(item.isResolved()).isTrue();

        // Step 5: Delayed replay of any earlier reference after clearance
        item.settle(new BigDecimal("40.00"), "collector-1", "TXN-001");
        item.settle(new BigDecimal("35.00"), "collector-2", "TXN-002");
        item.settle(new BigDecimal("25.00"), "collector-3", "TXN-003");

        assertThat(item.getSettledAmount()).isEqualByComparingTo("100.00");
        assertThat(item.getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(item.getStatus()).isEqualTo("CLEARED");
        assertThat(item.isResolved()).isTrue();
    }
}
