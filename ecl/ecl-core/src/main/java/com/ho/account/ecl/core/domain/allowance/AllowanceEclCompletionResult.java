package com.ho.account.ecl.core.domain.allowance;

import java.time.LocalDate;

public record AllowanceEclCompletionResult(
        LocalDate baseDate,
        int calculatedResultCount,
        int completedResultCount) {
}
