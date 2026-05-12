package com.ho.account.contracts.ledger;

import java.time.LocalDate;
import java.util.List;

public interface LedgerQueryPort {

    List<LedgerBalanceSummary> getGlBalanceSummaries(LocalDate startDate,
                                                     LocalDate endDate,
                                                     String accountCode,
                                                     String currencyCode);
}
