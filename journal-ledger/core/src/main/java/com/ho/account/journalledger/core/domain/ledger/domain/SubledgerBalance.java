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
 * Abstract Base class for Subledger Balances
 */
@MappedSuperclass
@Getter @Setter
@NoArgsConstructor
public abstract class SubledgerBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", referencedColumnName = "account_code")
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code")
    private Currency currency;

    private LocalDate balanceDate;
    private BigDecimal endingBalance = BigDecimal.ZERO;
}
