package com.ho.account.closing.application.port.in;

import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Inbound contract for appending a provider snapshot; the adapter supplies the trusted actor separately. */
public interface FinalCloseEvidenceUseCase {

    FinalCloseEvidenceSet record(Long calendarId, Submission submission, String trustedSubmittedBy);

    /** Provider payload. It deliberately has no submittedBy field that a request body could claim. */
    record Submission(
            String evidenceSetId,
            Long calendarId,
            Long fiscalPeriodId,
            String fiscalYear,
            String fiscalPeriod,
            LocalDate ledgerCutoff,
            Instant observedAt,
            String contentDigest,
            List<FinalCloseEvidenceControl> controls) {

        public Submission {
            controls = controls == null ? List.of() : List.copyOf(controls);
        }
    }
}
