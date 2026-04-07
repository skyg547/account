package com.ho.account.common.adapter;

import com.ho.account.closing.service.ClosingService;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ClosingStatusAdapter implements AccountingPeriodStatusPort {

    private final ClosingService closingService;

    public ClosingStatusAdapter(ClosingService closingService) {
        this.closingService = closingService;
    }

    @Override
    public boolean isClosed(LocalDate accountingDate) {
        return closingService.isClosed(accountingDate);
    }
}
