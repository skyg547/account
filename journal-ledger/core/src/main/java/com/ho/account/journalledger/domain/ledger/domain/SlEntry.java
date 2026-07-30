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

    @Column(name = "dr_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal drAmount = BigDecimal.ZERO;

    @Column(name = "cr_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal crAmount = BigDecimal.ZERO;

    @Column(name = "base_dr_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseDrAmount = BigDecimal.ZERO;

    @Column(name = "base_cr_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseCrAmount = BigDecimal.ZERO;

    private String summary;

    private String lineageSourceType;

    private String lineageSourceId;

    public void setDrAmount(BigDecimal drAmount) {
        this.drAmount = AccountingPrecision.nonNegativeLedgerAmount(drAmount);
    }

    public void setCrAmount(BigDecimal crAmount) {
        this.crAmount = AccountingPrecision.nonNegativeLedgerAmount(crAmount);
    }

    public void setBaseDrAmount(BigDecimal baseDrAmount) {
        this.baseDrAmount = AccountingPrecision.nonNegativeLedgerAmount(baseDrAmount);
    }

    public void setBaseCrAmount(BigDecimal baseCrAmount) {
        this.baseCrAmount = AccountingPrecision.nonNegativeLedgerAmount(baseCrAmount);
    }
}
