package com.ho.account.reconciliation.application.port.in;

import java.time.LocalDate;

public interface ReconciliationBatchUseCase {

    ReconciliationBatchRunResult runDailyReconciliation(LocalDate reconciliationDate, String runBy, boolean deepMode);

    record ReconciliationBatchRunResult(int unitCount, int runCount) {
    }
}
