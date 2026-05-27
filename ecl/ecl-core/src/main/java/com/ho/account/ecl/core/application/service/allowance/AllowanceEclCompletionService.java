package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.port.out.AllowanceEclCompletionPort;
import com.ho.account.ecl.core.domain.allowance.AllowanceEclCompletionResult;
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
public class AllowanceEclCompletionService {

    private final AllowanceEclCompletionPort completionPort;

    @Transactional
    public AllowanceEclCompletionResult completeCalculatedEclResults(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "baseDate must not be null");

        int calculatedResultCount = completionPort.countCalculatedEclResults(baseDate);
        if (calculatedResultCount == 0) {
            log.info("[Allowance ECL Completion] no calculated ECL results. baseDate={}", baseDate);
            return new AllowanceEclCompletionResult(baseDate, 0, 0);
        }

        int completedResultCount = completionPort.markCalculatedEclResultsCompleted(baseDate);
        if (completedResultCount != calculatedResultCount) {
            throw new IllegalStateException("Allowance ECL completion row count mismatch. baseDate="
                    + baseDate + ", calculatedResultCount=" + calculatedResultCount
                    + ", completedResultCount=" + completedResultCount);
        }

        log.info("[Allowance ECL Completion] completed. baseDate={}, results={}", baseDate, completedResultCount);
        return new AllowanceEclCompletionResult(baseDate, calculatedResultCount, completedResultCount);
    }
}
