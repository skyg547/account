package com.ho.account.journalledger.core.domain.ledger.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.domain.model.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Subsidiary Ledger Entry
 */
@Entity
@Table(name = "sl_entries")
@Getter @Setter
@NoArgsConstructor
public class SlEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", referencedColumnName = "account_code")
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bp_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner businessPartner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "dept_code")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code")
    private Currency currency;

    private LocalDate accountingDate;

    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;

    private String summary;

    @Column(name = "journal_detail_id")
    private Long journalDetailId;
}
