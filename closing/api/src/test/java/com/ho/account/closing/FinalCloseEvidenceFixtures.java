package com.ho.account.closing;

import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase;
import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase.Submission;
import com.ho.account.closing.application.service.FinalCloseEvidenceService;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Outcome;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Type;
import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

final class FinalCloseEvidenceFixtures {
    private FinalCloseEvidenceFixtures() { }

    static com.ho.account.contracts.journal.JournalPeriodFenceCommand closedFence(
            long periodId, String year, String month) {
        LocalDate start = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);
        return new com.ho.account.contracts.journal.JournalPeriodFenceCommand(java.util.UUID.randomUUID().toString(),
                new com.ho.account.contracts.journal.JournalPeriodCheckpoint(
                        new com.ho.account.contracts.journal.JournalPeriodScope(periodId, start,
                                start.withDayOfMonth(start.lengthOfMonth())), 0), "original-closer");
    }

    static String recordValid(FinalCloseEvidenceUseCase useCase, ClosingCalendar calendar,
            Long fiscalPeriodId, LocalDate cutoff) {
        return recordValid(useCase, calendar, fiscalPeriodId, cutoff, Instant.now());
    }

    static String recordValid(FinalCloseEvidenceUseCase useCase, ClosingCalendar calendar,
            Long fiscalPeriodId, LocalDate cutoff, Instant observedAt) {
        return recordValid(useCase, calendar, fiscalPeriodId, cutoff, observedAt, "", BigDecimal.ZERO);
    }

    static String recordValid(FinalCloseEvidenceUseCase useCase, ClosingCalendar calendar,
            Long fiscalPeriodId, LocalDate cutoff, Instant observedAt, String sourceRunSuffix,
            BigDecimal matchedTotal) {
        String id = "test-evidence-" + calendar.getId() + "-" + System.nanoTime();
        List<FinalCloseEvidenceControl> controls = Arrays.stream(Type.values())
                .filter(type -> type != Type.ANNUAL_TRANSFER || "YEAR".equals(calendar.getFiscalPeriod()))
                .map(type -> new FinalCloseEvidenceControl(type, type.expectedSourceSystem(),
                        "run-" + type + sourceRunSuffix,
                        Outcome.PASS, 0, List.of(new FinalCloseEvidenceTotal(
                                "1000", "KRW", matchedTotal, matchedTotal))))
                .toList();
        var checkpoint = useCase.checkpoint(calendar.getId());
        Submission seed = new Submission(id, calendar.getId(), fiscalPeriodId, calendar.getFiscalYear(),
                calendar.getFiscalPeriod(), cutoff, observedAt, "0".repeat(64), controls, checkpoint);
        Submission signed = new Submission(id, calendar.getId(), fiscalPeriodId, calendar.getFiscalYear(),
                calendar.getFiscalPeriod(), cutoff, observedAt,
                FinalCloseEvidenceService.computeContentDigest(seed, "test-provider"), controls, checkpoint);
        useCase.record(calendar.getId(), signed, "test-provider");
        return id;
    }
}
