package com.ho.account.journalledger.domain.ledger.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 총계정원장 계정별 잔액 (General Ledger Account Balance)
 */
@Entity
@Table(name = "gl_account_balances")
@Getter @Setter
@NoArgsConstructor
public class GlAccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false)
    private LocalDate balanceDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GlBalanceType balanceType; // DEBIT, CREDIT

    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private BigDecimal endingBalance = BigDecimal.ZERO;

    public void updateBalance(BigDecimal debit, BigDecimal credit) {
        this.debitAmount = this.debitAmount.add(debit);
        this.creditAmount = this.creditAmount.add(credit);
        // 계산 로직은 계정 유형(자산/부채)에 따라 달라질 수 있음
        this.endingBalance = this.debitAmount.subtract(this.creditAmount);
    }
}