package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sl_entries")
@Getter @Setter
@NoArgsConstructor
public class SlEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id")
    private JournalDetail journalDetail;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "business_partner_code", length = 50)
    private String businessPartnerCode;

    @Column(name = "dept_code", length = 50)
    private String departmentCode;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    private String fiscalYear;

    private String fiscalPeriod;

    private LocalDate postingDate;

    private BigDecimal drAmount = BigDecimal.ZERO;

    private BigDecimal crAmount = BigDecimal.ZERO;

    private BigDecimal baseDrAmount = BigDecimal.ZERO;

    private BigDecimal baseCrAmount = BigDecimal.ZERO;

    private String summary;

    private String lineageSourceType;

    private String lineageSourceId;
}