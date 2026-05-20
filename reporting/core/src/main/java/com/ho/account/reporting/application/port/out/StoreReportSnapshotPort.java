package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.FinancialStatement;

public interface StoreReportSnapshotPort {

    void saveFinalized(FinancialStatement statement);
}
