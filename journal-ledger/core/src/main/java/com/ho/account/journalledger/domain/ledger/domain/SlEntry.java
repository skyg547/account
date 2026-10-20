package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
public class SlEntry {

    private Long id;

    private JournalDetail journalDetail;

    private String accountCode;

    private String businessPartnerCode;

    private String departmentCode;

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
