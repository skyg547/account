package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.EclAllowanceSummary;

import java.time.LocalDate;
import java.util.List;

/**
 * Outbound port for reading finalized IFRS 9 ECL allowance results.
 * Each summary's monetary values retain its transaction {@code currencyCode}; adapters must not
 * substitute functional-currency GL balances or convert targets before the posting-group sum.
 */
public interface EclAllowanceResultPort {

    List<EclAllowanceSummary> loadSummaries(LocalDate baseDate);
}
