package com.ho.account.common.adapter;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.time.LocalDate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class FiscalPeriodAccountingPeriodStatusAdapter implements AccountingPeriodStatusPort {

    private final FiscalPeriodControlPort fiscalPeriodControlPort;

    public FiscalPeriodAccountingPeriodStatusAdapter(FiscalPeriodControlPort fiscalPeriodControlPort) {
        this.fiscalPeriodControlPort = fiscalPeriodControlPort;
    }

    @Override
    public boolean isClosed(LocalDate accountingDate) {
        String fiscalYear = String.valueOf(accountingDate.getYear());
        String fiscalPeriod = String.format("%02d", accountingDate.getMonthValue());

        return fiscalPeriodControlPort.findFiscalPeriod(fiscalYear, fiscalPeriod)
                .map(this::isClosed)
                .orElse(false);
    }

    private boolean isClosed(FiscalPeriodRef fiscalPeriod) {
        return "CLOSED".equals(fiscalPeriod.closingStatus())
                || "PERMANENTLY_CLOSED".equals(fiscalPeriod.closingStatus());
    }
}
