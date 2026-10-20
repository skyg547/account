package com.ho.account.journalledger.domain.ledger.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
public class GlBalance {

    private Long id;

    private String accountCode;

    private String currencyCode;

    private LocalDate balanceDate;

    private YearMonth period;

    private BigDecimal beginningBalance = BigDecimal.ZERO;

    private BigDecimal debitAmount = BigDecimal.ZERO;

    private BigDecimal creditAmount = BigDecimal.ZERO;

    private BigDecimal endingBalance = BigDecimal.ZERO;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void addDebit(BigDecimal amount) {
        BigDecimal normalized = AccountingPrecision.nonNegativeLedgerAmount(amount);
        this.debitAmount = AccountingPrecision.nonNegativeLedgerAmount(
                (this.debitAmount == null ? BigDecimal.ZERO : this.debitAmount).add(normalized));
        recalculate();
    }

    public void addCredit(BigDecimal amount) {
        BigDecimal normalized = AccountingPrecision.nonNegativeLedgerAmount(amount);
        this.creditAmount = AccountingPrecision.nonNegativeLedgerAmount(
                (this.creditAmount == null ? BigDecimal.ZERO : this.creditAmount).add(normalized));
        recalculate();
    }

    public void recalculate() {
        BigDecimal beg = this.beginningBalance == null ? BigDecimal.ZERO : this.beginningBalance;
        BigDecimal dr  = this.debitAmount      == null ? BigDecimal.ZERO : this.debitAmount;
        BigDecimal cr  = this.creditAmount     == null ? BigDecimal.ZERO : this.creditAmount;
        this.endingBalance = AccountingPrecision.ledgerAmount(beg.add(dr).subtract(cr));
        this.updatedAt = LocalDateTime.now();
    }

    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.beginningBalance == null) this.beginningBalance = BigDecimal.ZERO;
        if (this.debitAmount      == null) this.debitAmount      = BigDecimal.ZERO;
        if (this.creditAmount     == null) this.creditAmount     = BigDecimal.ZERO;
        recalculate();
    }

    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setBeginningBalance(BigDecimal beginningBalance) {
        this.beginningBalance = AccountingPrecision.ledgerAmount(beginningBalance);
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = AccountingPrecision.nonNegativeLedgerAmount(debitAmount);
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = AccountingPrecision.nonNegativeLedgerAmount(creditAmount);
    }

    public void setEndingBalance(BigDecimal endingBalance) {
        this.endingBalance = AccountingPrecision.ledgerAmount(endingBalance);
    }
}
