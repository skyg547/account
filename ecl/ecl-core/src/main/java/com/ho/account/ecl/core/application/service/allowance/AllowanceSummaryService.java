package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.port.out.AllowanceSummaryBuildPort;
import com.ho.account.ecl.core.domain.allowance.AllowanceSummaryBuildResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllowanceSummaryService {

    private final AllowanceSummaryBuildPort allowanceSummaryBuildPort;

    @Transactional
    public AllowanceSummaryBuildResult rebuildAllowanceSummary(
            LocalDate baseDate,
            String runId,
            String modelVersion) {

        Objects.requireNonNull(baseDate, "baseDate must not be null");
        requireText(runId, "runId");
        requireText(modelVersion, "modelVersion");

        int sourceResultCount = allowanceSummaryBuildPort.countEligibleResults(baseDate);
        if (sourceResultCount == 0) {
            // Zero results may mean an incomplete rerun; only validated input may replace a closing snapshot.
            throw new IllegalStateException("No completed ECL results for allowance summary. baseDate="
                    + baseDate + ", runId=" + runId);
        }

        int missingMappingCount = allowanceSummaryBuildPort.countMissingAccountMappings(baseDate);
        if (missingMappingCount > 0) {
            throw new IllegalStateException("Missing allowance account mappings. baseDate="
                    + baseDate + ", missingResultCount=" + missingMappingCount);
        }

        allowanceSummaryBuildPort.deleteByBaseDate(baseDate);
        int summaryRowCount = allowanceSummaryBuildPort.insertSummariesFromAllowanceResults(baseDate, runId, modelVersion);

        log.info("[Allowance Summary] rebuilt. baseDate={}, runId={}, sourceResults={}, summaryRows={}",
                baseDate, runId, sourceResultCount, summaryRowCount);
        return new AllowanceSummaryBuildResult(baseDate, runId, modelVersion, sourceResultCount, summaryRowCount);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
