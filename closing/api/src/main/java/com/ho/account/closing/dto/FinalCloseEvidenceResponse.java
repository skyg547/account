package com.ho.account.closing.dto;

import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import java.time.Instant;
import java.time.LocalDate;

/** Concise acknowledgement of the immutable snapshot selected by the application service. */
public record FinalCloseEvidenceResponse(
        String evidenceSetId,
        Long calendarId,
        Long fiscalPeriodId,
        String fiscalYear,
        String fiscalPeriod,
        LocalDate ledgerCutoff,
        Instant observedAt,
        String submittedBy,
        String contentDigest,
        int controlCount) {

    public static FinalCloseEvidenceResponse from(FinalCloseEvidenceSet evidenceSet) {
        return new FinalCloseEvidenceResponse(
                evidenceSet.evidenceSetId(),
                evidenceSet.calendarId(),
                evidenceSet.fiscalPeriodId(),
                evidenceSet.fiscalYear(),
                evidenceSet.fiscalPeriod(),
                evidenceSet.ledgerCutoff(),
                evidenceSet.observedAt(),
                evidenceSet.submittedBy(),
                evidenceSet.contentDigest(),
                evidenceSet.controls().size());
    }
}
