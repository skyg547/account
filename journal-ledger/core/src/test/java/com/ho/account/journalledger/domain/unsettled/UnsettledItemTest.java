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
    }
}
