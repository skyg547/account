package com.ho.account.closing.dto;

import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase;
import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Bounded provider payload for one immutable final-close evidence snapshot.
 * The authenticated submitter is intentionally absent and comes only from the trusted header.
 */
public record FinalCloseEvidenceRequest(
        @NotBlank @Size(max = 100) String evidenceSetId,
        @NotNull @Positive Long calendarId,
        @NotNull @Positive Long fiscalPeriodId,
        @NotBlank @Size(max = 10) String fiscalYear,
        @NotBlank @Size(max = 20) String fiscalPeriod,
        @NotNull LocalDate ledgerCutoff,
        @NotNull Instant observedAt,
        @NotBlank
        @Size(min = 64, max = 64)
        @Pattern(regexp = "[0-9a-f]{64}", message = "contentDigest must be a lowercase SHA-256 hex value")
        String contentDigest,
        @NotEmpty @Size(max = 32) List<@NotNull @Valid Control> controls) {

    public FinalCloseEvidenceUseCase.Submission toSubmission() {
        return new FinalCloseEvidenceUseCase.Submission(
                evidenceSetId,
                calendarId,
                fiscalPeriodId,
                fiscalYear,
                fiscalPeriod,
                ledgerCutoff,
                observedAt,
                contentDigest,
                controls.stream().map(Control::toDomain).toList());
    }

    public record Control(
            @NotNull FinalCloseEvidenceControl.Type type,
            @NotBlank @Size(max = 100) String sourceSystem,
            @NotBlank @Size(max = 100) String sourceRunId,
            @NotNull FinalCloseEvidenceControl.Outcome outcome,
            @PositiveOrZero int blockingItemCount,
            @NotEmpty @Size(max = 1_000) List<@NotNull @Valid DimensionTotal> totals) {

        private FinalCloseEvidenceControl toDomain() {
            return new FinalCloseEvidenceControl(
                    type,
                    sourceSystem,
                    sourceRunId,
                    outcome,
                    blockingItemCount,
                    totals.stream().map(DimensionTotal::toDomain).toList());
        }
    }

    public record DimensionTotal(
            @NotBlank @Size(max = 100) String accountCode,
            @NotBlank @Size(max = 3) String currencyCode,
            @NotNull BigDecimal sourceTotal,
            @NotNull BigDecimal postedTotal) {

        private FinalCloseEvidenceTotal toDomain() {
            return new FinalCloseEvidenceTotal(accountCode, currencyCode, sourceTotal, postedTotal);
        }
    }
}
