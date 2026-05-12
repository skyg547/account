package com.ho.account.contracts.journal;

import java.math.BigDecimal;

public class JournalDetailAggregateSummary {
    private long detailCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public JournalDetailAggregateSummary() {
    }

    public JournalDetailAggregateSummary(long detailCount, BigDecimal totalAmount) {
        this.detailCount = detailCount;
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public long getDetailCount() {
        return detailCount;
    }

    public void setDetailCount(long detailCount) {
        this.detailCount = detailCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }
}
