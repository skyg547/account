package com.ho.account.journalledger.core.domain.ledger.dto;

import java.math.BigDecimal;

public class TrialBalanceDTO {
    private String accountCode;
    private String accountName;
    private BigDecimal beginBalanceDr;
    private BigDecimal beginBalanceCr;
    private BigDecimal periodDr;
    private BigDecimal periodCr;
    private BigDecimal endBalanceDr;
    private BigDecimal endBalanceCr;

    public TrialBalanceDTO(String accountCode, String accountName,
            BigDecimal beginBalanceDr, BigDecimal beginBalanceCr,
            BigDecimal periodDr, BigDecimal periodCr,
            BigDecimal endBalanceDr, BigDecimal endBalanceCr) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.beginBalanceDr = beginBalanceDr;
        this.beginBalanceCr = beginBalanceCr;
        this.periodDr = periodDr;
        this.periodCr = periodCr;
        this.endBalanceDr = endBalanceDr;
        this.endBalanceCr = endBalanceCr;
    }

    // Getter
    public String getCode() {
        return accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public BigDecimal getBeginBalanceDr() {
        return beginBalanceDr;
    }

    public BigDecimal getBeginBalanceCr() {
        return beginBalanceCr;
    }

    public BigDecimal getPeriodDr() {
        return periodDr;
    }

    public BigDecimal getPeriodCr() {
        return periodCr;
    }

    public BigDecimal getEndBalanceDr() {
        return endBalanceDr;
    }

    public BigDecimal getEndBalanceCr() {
        return endBalanceCr;
    }
}
