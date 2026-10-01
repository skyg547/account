package com.ho.account.closing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase.Submission;
import com.ho.account.closing.application.port.out.ClosingAggregatePersistencePort;
import com.ho.account.closing.application.port.out.FinalCloseEvidencePersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.FinalCloseEvidenceControl;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Outcome;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Type;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import com.ho.account.closing.domain.FinalCloseEvidenceTotal;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinalCloseEvidenceServiceTest {
    private static final Instant NOW = Instant.parse("2026-02-01T00:00:00Z");
    private static final LocalDate CUTOFF = LocalDate.of(2026, 1, 31);
    private final ClosingAggregatePersistencePort aggregates = mock(ClosingAggregatePersistencePort.class);
    private final FinalCloseEvidencePersistencePort persistence = mock(FinalCloseEvidencePersistencePort.class);
    private FinalCloseEvidenceService service;
    private ClosingCalendar calendar;
    private FiscalPeriodRef period;

    @BeforeEach
    void setUp() {
        FinalCloseEvidenceProperties properties = new FinalCloseEvidenceProperties();
        properties.setMaxAge(Duration.ofHours(24));
        service = new FinalCloseEvidenceService(aggregates, persistence, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        calendar = calendar("01");
        period = period("01", CUTOFF);
    }

    @Test
    void monthlyEvidenceAcceptsEveryRequiredControlWithExactMultiCurrencyPrecisionAndExplicitZero() {
        FinalCloseEvidenceSet evidence = evidence("monthly-ok", "01", NOW.minusSeconds(60), monthlyControls());
        when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(evidence));

        assertThat(service.requireForFinalClose(calendar, period)).isSameAs(evidence);
        assertThat(evidence.controls()).flatExtracting(FinalCloseEvidenceControl::totals)
                .extracting(FinalCloseEvidenceTotal::sourceTotal)
                .anySatisfy(amount -> assertThat(amount).isEqualByComparingTo("0"))
                .anySatisfy(amount -> assertThat(amount).isEqualByComparingTo("123456789.123456789012345678"));
    }

    @Test
    void yearRequiresAnnualTransferInAdditionToAllMonthlyControls() {
        calendar = calendar("YEAR");
        period = period("YEAR", LocalDate.of(2026, 12, 31));
        FinalCloseEvidenceSet missingAnnual = evidence("year-missing", "YEAR", NOW.minusSeconds(60), monthlyControls());
        when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(missingAnnual));
        assertRejected("required controls");

        List<FinalCloseEvidenceControl> annual = new ArrayList<>(monthlyControls());
        annual.add(control(Type.ANNUAL_TRANSFER, Outcome.PASS, 0, totals()));
        FinalCloseEvidenceSet complete = evidence("year-ok", "YEAR", NOW.minusSeconds(60), annual);
        when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(complete));
        assertThat(service.requireForFinalClose(calendar, period).evidenceSetId()).isEqualTo("year-ok");

        for (int blockers : List.of(0, 1)) {
            List<FinalCloseEvidenceControl> rejected = new ArrayList<>(monthlyControls());
            rejected.add(control(Type.ANNUAL_TRANSFER, blockers == 0 ? Outcome.FAIL : Outcome.PASS, blockers, totals()));
            when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(
                    evidence("year-rejected-" + blockers, "YEAR", NOW.minusSeconds(60), rejected)));
            assertRejected("did not pass");
        }
    }

    @Test
    void rejectsMissingDuplicateFailedBlockedWrongSourceMissingTotalsDuplicateDimensionAndMismatch() {
        List<Case> cases = List.of(
                new Case(controls -> controls.subList(1, controls.size()), "required controls"),
                new Case(controls -> { controls.add(controls.get(0)); return controls; }, "required controls"),
                new Case(controls -> replace(controls, 0, control(Type.AP_SUBLEDGER, Outcome.FAIL, 0, totals())), "did not pass"),
                new Case(controls -> replace(controls, 0, control(Type.AP_SUBLEDGER, Outcome.PASS, 1, totals())), "did not pass"),
                new Case(controls -> replace(controls, 0, new FinalCloseEvidenceControl(Type.AP_SUBLEDGER, "wrong", "run", Outcome.PASS, 0, totals())), "must come from"),
                new Case(controls -> replace(controls, 0, control(Type.AP_SUBLEDGER, Outcome.PASS, 0, List.of())), "at least one"),
                new Case(controls -> replace(controls, 0, control(Type.AP_SUBLEDGER, Outcome.PASS, 0,
                        List.of(total("100", "USD", "1.00", "1"), total("100", "usd", "2", "2.0")))), "duplicate"),
                new Case(controls -> replace(controls, 0, control(Type.AP_SUBLEDGER, Outcome.PASS, 0,
                        List.of(total("100", "USD", "1.00", "1.01")))), "totals differ"));

        for (int index = 0; index < cases.size(); index++) {
            List<FinalCloseEvidenceControl> controls = new ArrayList<>(monthlyControls());
            FinalCloseEvidenceSet invalid = evidence("invalid-" + index, "01", NOW.minusSeconds(60),
                    cases.get(index).mutation.apply(controls));
            when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(invalid));
            assertRejected(cases.get(index).message);
        }
    }

    @Test
    void rejectsWrongIdentityCutoffStaleBoundaryFutureAndDigestMismatch() {
        List<FinalCloseEvidenceSet> invalid = List.of(
                evidence("wrong-calendar", 11L, 20L, "2026", "01", CUTOFF, NOW.minusSeconds(60), monthlyControls()),
                evidence("wrong-period-id", 10L, 21L, "2026", "01", CUTOFF, NOW.minusSeconds(60), monthlyControls()),
                evidence("wrong-year", 10L, 20L, "2025", "01", CUTOFF, NOW.minusSeconds(60), monthlyControls()),
                evidence("wrong-code", 10L, 20L, "2026", "02", CUTOFF, NOW.minusSeconds(60), monthlyControls()),
                evidence("wrong-cutoff", 10L, 20L, "2026", "01", CUTOFF.minusDays(1), NOW.minusSeconds(60), monthlyControls()),
                evidence("at-initiation", "01", NOW.minusSeconds(3600), monthlyControls()),
                evidence("future", "01", NOW.plusNanos(1_000), monthlyControls()));
        for (FinalCloseEvidenceSet evidence : invalid) {
            when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(evidence));
            assertThatThrownBy(() -> service.requireForFinalClose(calendar, period))
                    .as(evidence.evidenceSetId()).isInstanceOf(FinalCloseEvidenceValidationException.class);
        }

        FinalCloseEvidenceSet valid = evidence("digest", "01", NOW.minusSeconds(60), monthlyControls());
        FinalCloseEvidenceSet tampered = new FinalCloseEvidenceSet(valid.evidenceSetId(), valid.calendarId(),
                valid.fiscalPeriodId(), valid.fiscalYear(), valid.fiscalPeriod(), valid.ledgerCutoff(),
                valid.observedAt(), valid.submittedBy(), "f".repeat(64), valid.controls());
        when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(tampered));
        assertRejected("digest");
    }

    @Test
    void maximumAgeBoundaryIsInclusiveAndOneNanosecondOlderIsRejected() {
        calendar.setCloseInitiatedAt(LocalDateTime.ofInstant(NOW.minus(Duration.ofDays(2)), ZoneOffset.UTC));
        FinalCloseEvidenceSet boundary = evidence("age-boundary", "01", NOW.minus(Duration.ofHours(24)), monthlyControls());
        when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(boundary));
        assertThat(service.requireForFinalClose(calendar, period)).isSameAs(boundary);

        FinalCloseEvidenceSet older = evidence("age-too-old", "01",
                NOW.minus(Duration.ofHours(24)).minusNanos(1), monthlyControls());
        when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(older));
        assertRejected("older than");
    }

    @Test
    void boundLookupUsesOnlyOriginalIdAndRevalidatesAtDispatchTime() {
        calendar.setCloseInitiatedAt(LocalDateTime.ofInstant(NOW.minus(Duration.ofDays(2)), ZoneOffset.UTC));
        FinalCloseEvidenceSet bound = evidence("bound", "01", NOW.minus(Duration.ofHours(24)), monthlyControls());
        FinalCloseEvidenceSet newer = evidence("newer", "01", NOW.minusSeconds(1), monthlyControls());
        when(persistence.findByEvidenceSetId("bound")).thenReturn(Optional.of(bound));
        when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(newer));

        assertThat(service.requireBoundForFinalClose(calendar, period, "bound")).isSameAs(bound);
        verify(persistence).findByEvidenceSetId("bound");
        verify(persistence, never()).findLatestByCalendarId(10L);

        service = new FinalCloseEvidenceService(aggregates, persistence, new FinalCloseEvidenceProperties(),
                Clock.fixed(NOW.plusNanos(1), ZoneOffset.UTC));
        assertThatThrownBy(() -> service.requireBoundForFinalClose(calendar, period, "bound"))
                .isInstanceOf(FinalCloseEvidenceValidationException.class).hasMessageContaining("older than");
    }

    @Test
    void boundLookupRejectsAbsentWrongIdentityFailedAndTamperedSnapshot() {
        assertThatThrownBy(() -> service.requireBoundForFinalClose(calendar, period, "missing"))
                .isInstanceOf(FinalCloseEvidenceValidationException.class).hasMessageContaining("does not exist");
        FinalCloseEvidenceSet valid = evidence("bound", "01", NOW.minusSeconds(1), monthlyControls());
        when(persistence.findByEvidenceSetId("bound")).thenReturn(Optional.of(
                evidence("other", "01", NOW.minusSeconds(1), monthlyControls())));
        assertThatThrownBy(() -> service.requireBoundForFinalClose(calendar, period, "bound"))
                .isInstanceOf(FinalCloseEvidenceValidationException.class).hasMessageContaining("bound ID");

        List<FinalCloseEvidenceSet> invalid = List.of(
                evidence("bound", 10L, 20L, "2026", "02", CUTOFF, NOW.minusSeconds(1), monthlyControls()),
                evidence("bound", 10L, 20L, "2026", "01", CUTOFF, NOW.minusSeconds(1),
                        replace(new ArrayList<>(monthlyControls()), 0,
                                control(Type.AP_SUBLEDGER, Outcome.FAIL, 0, totals()))),
                new FinalCloseEvidenceSet(valid.evidenceSetId(), valid.calendarId(), valid.fiscalPeriodId(),
                        valid.fiscalYear(), valid.fiscalPeriod(), valid.ledgerCutoff(), valid.observedAt(),
                        valid.submittedBy(), "f".repeat(64), valid.controls()));
        for (FinalCloseEvidenceSet snapshot : invalid) {
            when(persistence.findByEvidenceSetId("bound")).thenReturn(Optional.of(snapshot));
            assertThatThrownBy(() -> service.requireBoundForFinalClose(calendar, period, "bound"))
                    .as(snapshot.toString()).isInstanceOf(FinalCloseEvidenceValidationException.class);
        }
    }

    @Test
    void nanosecondObservationIsCanonicalizedToDatabaseMicrosecondsBeforeDigesting() {
        Instant nanos = Instant.parse("2026-01-31T23:59:00.123456789Z");
        Submission nanosSubmission = new Submission("precision", 10L, 20L, "2026", "01", CUTOFF,
                nanos, "0".repeat(64), monthlyControls());
        String nanosDigest = FinalCloseEvidenceService.computeContentDigest(nanosSubmission, "provider-A");

        FinalCloseEvidenceSet normalized = new FinalCloseEvidenceSet("precision", 10L, 20L, "2026", "01",
                CUTOFF, nanos, "provider-A", nanosDigest, monthlyControls());
        Submission normalizedSubmission = new Submission("precision", 10L, 20L, "2026", "01", CUTOFF,
                normalized.observedAt(), "0".repeat(64), monthlyControls());

        assertThat(normalized.observedAt()).isEqualTo(Instant.parse("2026-01-31T23:59:00.123456Z"));
        assertThat(FinalCloseEvidenceService.computeContentDigest(normalizedSubmission, "provider-A"))
                .isEqualTo(nanosDigest);
    }

    @Test
    void numeric38Scale18AcceptsSignedMaximumBoundaryAndTrailingFractionalZeros() {
        String maximum = "99999999999999999999.999999999999999999";
        FinalCloseEvidenceTotal positive = total("MAX", "USD", maximum, maximum);
        FinalCloseEvidenceTotal negative = total("MIN", "USD", "-" + maximum, "-" + maximum);
        FinalCloseEvidenceTotal trailingZeros = total(
                "TRAILING", "USD", "1.123456789012345678000", "1.1234567890123456780");

        assertThat(positive.sourceTotal()).isEqualByComparingTo(maximum);
        assertThat(negative.sourceTotal()).isEqualByComparingTo("-" + maximum);
        assertThat(trailingZeros.sourceTotal()).isEqualByComparingTo("1.123456789012345678");
    }

    @Test
    void numeric38Scale18RejectsOverflowAndExcessMeaningfulFractionBeforePersistence() {
        assertThatThrownBy(() -> total(
                "INTEGER_OVERFLOW", "USD", "100000000000000000000.000000000000000000", "0"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NUMERIC(38,18)");
        assertThatThrownBy(() -> total(
                "FRACTION_OVERFLOW", "USD", "0.1234567890123456789", "0.1234567890123456789"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NUMERIC(38,18)");
        verifyNoInteractions(persistence);
    }

    @Test
    void everyRequiredMonthlyTypeRejectsFailAndNonzeroBlockers() {
        for (Type type : Type.values()) {
            if (type == Type.ANNUAL_TRANSFER) continue;
            for (int variant = 0; variant < 2; variant++) {
                List<FinalCloseEvidenceControl> controls = new ArrayList<>(monthlyControls());
                int index = java.util.stream.IntStream.range(0, controls.size())
                        .filter(i -> controls.get(i).type() == type).findFirst().orElseThrow();
                controls.set(index, control(type, variant == 0 ? Outcome.FAIL : Outcome.PASS,
                        variant == 0 ? 0 : 1, totals()));
                FinalCloseEvidenceSet invalid = evidence(type + "-" + variant, "01", NOW.minusSeconds(60), controls);
                when(persistence.findLatestByCalendarId(10L)).thenReturn(Optional.of(invalid));
                assertRejected("did not pass");
            }
        }
    }

    @Test
    void recordUsesTrustedActorAndLockedCalendarIdentityAndState() {
        Submission unsigned = unsigned("recorded", 10L, "2026", "01", CUTOFF, NOW.minusSeconds(60), monthlyControls(), "provider-A");
        when(aggregates.lockCalendar(10L)).thenReturn(Optional.of(calendar));
        when(persistence.append(any())).thenAnswer(call -> call.getArgument(0));

        FinalCloseEvidenceSet recorded = service.record(10L, unsigned, " provider-A ");
        assertThat(recorded.submittedBy()).isEqualTo("provider-A");
        verify(persistence).append(recorded);

        org.mockito.Mockito.clearInvocations(persistence);
        calendar.setStatus(ClosingCalendarStatus.OPEN);
        assertThatThrownBy(() -> service.record(10L, unsigned, "provider-A"))
                .isInstanceOf(FinalCloseEvidenceValidationException.class).hasMessageContaining("IN_PROGRESS");
        verify(persistence, never()).append(any());
    }

    @Test
    void futureObservationCannotPinLatestWhileCurrentFailedSnapshotStillAppends() {
        when(aggregates.lockCalendar(10L)).thenReturn(Optional.of(calendar));
        when(persistence.append(any())).thenAnswer(call -> call.getArgument(0));
        Submission future = unsigned("future-set", 10L, "2026", "01", CUTOFF,
                NOW.plusSeconds(1), monthlyControls(), "provider-A");

        assertThatThrownBy(() -> service.record(10L, future, "provider-A"))
                .isInstanceOf(FinalCloseEvidenceValidationException.class)
                .hasMessageContaining("future");
        verify(persistence, never()).append(any());

        List<FinalCloseEvidenceControl> failed = replace(new ArrayList<>(monthlyControls()), 0,
                control(Type.AP_SUBLEDGER, Outcome.FAIL, 1, totals()));
        Submission currentFailure = unsigned("failed-set", 10L, "2026", "01", CUTOFF,
                NOW, failed, "provider-A");
        FinalCloseEvidenceSet saved = service.record(10L, currentFailure, "provider-A");
        assertThat(saved.controls().stream().filter(control -> control.type() == Type.AP_SUBLEDGER).toList())
                .singleElement().satisfies(control -> {
                    assertThat(control.outcome()).isEqualTo(Outcome.FAIL);
                    assertThat(control.blockingItemCount()).isEqualTo(1);
                });
        verify(persistence).append(saved);
    }

    private void assertRejected(String message) {
        assertThatThrownBy(() -> service.requireForFinalClose(calendar, period))
                .isInstanceOf(FinalCloseEvidenceValidationException.class).hasMessageContaining(message);
    }

    private ClosingCalendar calendar(String fiscalPeriod) {
        ClosingCalendar result = new ClosingCalendar();
        result.setId(10L); result.setFiscalYear("2026"); result.setFiscalPeriod(fiscalPeriod);
        result.setStatus(ClosingCalendarStatus.IN_PROGRESS);
        result.setCloseInitiatedAt(LocalDateTime.ofInstant(NOW.minusSeconds(3600), ZoneOffset.UTC));
        return result;
    }

    private FiscalPeriodRef period(String fiscalPeriod, LocalDate cutoff) {
        return new FiscalPeriodRef(20L, "2026", fiscalPeriod,
                fiscalPeriod.equals("YEAR") ? LocalDate.of(2026, 1, 1) : LocalDate.of(2026, 1, 1), cutoff, "OPEN");
    }

    private FinalCloseEvidenceSet evidence(String id, String code, Instant observedAt, List<FinalCloseEvidenceControl> controls) {
        return evidence(id, 10L, 20L, "2026", code, code.equals("YEAR") ? LocalDate.of(2026, 12, 31) : CUTOFF, observedAt, controls);
    }

    private FinalCloseEvidenceSet evidence(String id, Long calendarId, Long periodId, String year, String code,
            LocalDate cutoff, Instant observedAt, List<FinalCloseEvidenceControl> controls) {
        Submission unsigned = unsigned(id, calendarId, year, code, cutoff, observedAt, controls, "provider-A", periodId);
        return new FinalCloseEvidenceSet(id, calendarId, periodId, year, code, cutoff, observedAt, "provider-A",
                FinalCloseEvidenceService.computeContentDigest(unsigned, "provider-A"), controls);
    }

    private Submission unsigned(String id, Long calendarId, String year, String code, LocalDate cutoff,
            Instant observedAt, List<FinalCloseEvidenceControl> controls, String actor) {
        return unsigned(id, calendarId, year, code, cutoff, observedAt, controls, actor, 20L);
    }

    private Submission unsigned(String id, Long calendarId, String year, String code, LocalDate cutoff,
            Instant observedAt, List<FinalCloseEvidenceControl> controls, String actor, Long periodId) {
        Submission seed = new Submission(id, calendarId, periodId, year, code, cutoff, observedAt, "0".repeat(64), controls);
        return new Submission(id, calendarId, periodId, year, code, cutoff, observedAt,
                FinalCloseEvidenceService.computeContentDigest(seed, actor), controls);
    }

    private List<FinalCloseEvidenceControl> monthlyControls() {
        return Arrays.stream(Type.values()).filter(type -> type != Type.ANNUAL_TRANSFER)
                .map(type -> control(type, Outcome.PASS, 0, totals())).toList();
    }

    private FinalCloseEvidenceControl control(Type type, Outcome outcome, int blockers, List<FinalCloseEvidenceTotal> totals) {
        return new FinalCloseEvidenceControl(type, type.expectedSourceSystem(), "run-" + type, outcome, blockers, totals);
    }

    private List<FinalCloseEvidenceTotal> totals() {
        return List.of(total("1000", "KRW", "0.000", "0"),
                total("2000", "usd", "123456789.123456789012345678", "123456789.1234567890123456780"));
    }

    private FinalCloseEvidenceTotal total(String account, String currency, String source, String posted) {
        return new FinalCloseEvidenceTotal(account, currency, new BigDecimal(source), new BigDecimal(posted));
    }

    private List<FinalCloseEvidenceControl> replace(List<FinalCloseEvidenceControl> controls, int index,
            FinalCloseEvidenceControl replacement) {
        controls.set(index, replacement); return controls;
    }

    private record Case(UnaryOperator<List<FinalCloseEvidenceControl>> mutation, String message) { }
}
