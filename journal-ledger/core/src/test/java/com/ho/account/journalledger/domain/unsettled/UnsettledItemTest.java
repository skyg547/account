package com.ho.account.journalledger.domain.unsettled;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnsettledItemTest {

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
