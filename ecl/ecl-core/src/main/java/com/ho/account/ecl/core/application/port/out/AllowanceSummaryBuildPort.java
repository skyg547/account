package com.ho.account.ecl.core.application.port.out;

import java.time.LocalDate;

public interface AllowanceSummaryBuildPort {

    int countEligibleResults(LocalDate baseDate);

    int countMissingAccountMappings(LocalDate baseDate);

    void deleteByBaseDate(LocalDate baseDate);

    int insertSummariesFromAllowanceResults(LocalDate baseDate, String runId, String modelVersion);
}
