package com.ho.account.journalledger.core.domain.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LedgerDTO {
    private LocalDate date;
    private String slipNo;
    private String description; // 적요
    private BigDecimal debitAmount; // 차변
    private BigDecimal creditAmount; // 대변
    private BigDecimal balance; // 잔액

    public LedgerDTO(LocalDate date, String slipNo, String description, BigDecimal debitAmount, BigDecimal creditAmount, BigDecimal balance) {
        this.date = date;
        this.slipNo = slipNo;
        this.description = description;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.balance = balance;
    }

    // Getter
    public LocalDate getDate() { return date; }
    public String getSlipNo() { return slipNo; }
    public String getDescription() { return description; }
    public BigDecimal getDebitAmount() { return debitAmount; }
    public BigDecimal getCreditAmount() { return creditAmount; }
    public BigDecimal getBalance() { return balance; }
}
