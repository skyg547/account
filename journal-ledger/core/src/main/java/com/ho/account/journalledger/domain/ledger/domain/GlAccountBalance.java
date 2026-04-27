package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code")
    private Currency currency;

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
