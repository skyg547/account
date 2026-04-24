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
 * General Ledger Entry
 */
@Entity
@Table(name = "gl_entries")
@Getter @Setter
@NoArgsConstructor
public class GlEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", referencedColumnName = "account_code")
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code")
    private Currency currency;

    private LocalDate accountingDate;

    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;

    private String summary;
    
    @Column(name = "journal_entry_id")
    private Long journalEntryId;
}
