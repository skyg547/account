package com.ho.account.journalledger.core.domain.ledger.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * General Ledger Account Balance
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
    @JoinColumn(name = "account_code", referencedColumnName = "account_code")
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code")
    private Currency currency;

    @Column(nullable = false)
    private LocalDate balanceDate;

    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private BigDecimal endingBalance = BigDecimal.ZERO;

    public void updateBalance(BigDecimal debit, BigDecimal credit) {
        this.debitAmount = this.debitAmount.add(debit);
        this.creditAmount = this.creditAmount.add(credit);
        // Calculation logic depends on Account Type (Asset/Liability)
        this.endingBalance = this.debitAmount.subtract(this.creditAmount);
    }
}
