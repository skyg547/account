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

    static String recordValid(FinalCloseEvidenceUseCase useCase, ClosingCalendar calendar,
            Long fiscalPeriodId, LocalDate cutoff) {
        return recordValid(useCase, calendar, fiscalPeriodId, cutoff, Instant.now());
    }

    static String recordValid(FinalCloseEvidenceUseCase useCase, ClosingCalendar calendar,
            Long fiscalPeriodId, LocalDate cutoff, Instant observedAt) {
        String id = "test-evidence-" + calendar.getId() + "-" + System.nanoTime();
        List<FinalCloseEvidenceControl> controls = Arrays.stream(Type.values())
                .filter(type -> type != Type.ANNUAL_TRANSFER || "YEAR".equals(calendar.getFiscalPeriod()))
                .map(type -> new FinalCloseEvidenceControl(type, type.expectedSourceSystem(), "run-" + type,
                        Outcome.PASS, 0, List.of(new FinalCloseEvidenceTotal(
                                "1000", "KRW", BigDecimal.ZERO, new BigDecimal("0.000")))))
                .toList();
        Submission seed = new Submission(id, calendar.getId(), fiscalPeriodId, calendar.getFiscalYear(),
                calendar.getFiscalPeriod(), cutoff, observedAt, "0".repeat(64), controls);
        Submission signed = new Submission(id, calendar.getId(), fiscalPeriodId, calendar.getFiscalYear(),
                calendar.getFiscalPeriod(), cutoff, observedAt,
                FinalCloseEvidenceService.computeContentDigest(seed, "test-provider"), controls);
        useCase.record(calendar.getId(), signed, "test-provider");
        return id;
    }
}
