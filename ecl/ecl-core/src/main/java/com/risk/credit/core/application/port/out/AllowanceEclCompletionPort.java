package com.risk.credit.core.application.port.out;

import java.time.LocalDate;

public interface AllowanceEclCompletionPort {

    int countCalculatedEclResults(LocalDate baseDate);

    int markCalculatedEclResultsCompleted(LocalDate baseDate);
}
