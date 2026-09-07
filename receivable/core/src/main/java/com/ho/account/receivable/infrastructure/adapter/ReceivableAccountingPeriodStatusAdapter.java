package com.ho.account.receivable.infrastructure.adapter;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Remote adapter checking whether an accounting period is closed via master-data fiscal period control in receivable module.
 */
@Component
@Primary
@ConditionalOnProperty(
        prefix = "receivable.master-data.remote",
        name = "enabled",
        havingValue = "true")
public class ReceivableAccountingPeriodStatusAdapter implements AccountingPeriodStatusPort {

    private final FiscalPeriodControlPort fiscalPeriodControlPort;

    @Autowired
    public ReceivableAccountingPeriodStatusAdapter(FiscalPeriodControlPort fiscalPeriodControlPort) {
        this.fiscalPeriodControlPort = Objects.requireNonNull(fiscalPeriodControlPort, "fiscalPeriodControlPort must not be null");
    }

    @Override
    public boolean isClosed(LocalDate accountingDate) {
        Objects.requireNonNull(accountingDate, "accountingDate must not be null");
        String fiscalYear = String.valueOf(accountingDate.getYear());
        String fiscalPeriod = String.format("%02d", accountingDate.getMonthValue());

        return fiscalPeriodControlPort.findFiscalPeriod(fiscalYear, fiscalPeriod)
                .map(this::isClosed)
                .orElseThrow(() -> new IllegalStateException(
                        "Fiscal period is missing for accounting date " + accountingDate));
    }

    private boolean isClosed(FiscalPeriodRef fiscalPeriod) {
        return "CLOSED".equals(fiscalPeriod.closingStatus())
                || "PERMANENTLY_CLOSED".equals(fiscalPeriod.closingStatus());
    }
}
