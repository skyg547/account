package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.DailyClosingStatusPersistencePort;
import com.ho.account.closing.domain.DailyClosingStatus;
import com.ho.account.closing.domain.EodState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EodLifecycleServiceTest {

    private static final LocalDate THURSDAY = LocalDate.of(2026, 7, 30);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 7, 31);
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 3);
    private static final Instant NOW = Instant.parse("2026-07-30T23:45:00Z");

    private DailyClosingStatusPersistencePort persistencePort;
    private EodLifecycleService service;

    @BeforeEach
    void setUp() {
        persistencePort = mock(DailyClosingStatusPersistencePort.class);
        Clock nonUtcInputClock = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));
        service = new EodLifecycleService(persistencePort, nonUtcInputClock);
        when(persistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void bootstrapCreatesOnlyTheFirstRecordAndUsesUtcClock() {
        when(persistencePort.findLatestForUpdate()).thenReturn(Optional.empty());

        DailyClosingStatus created = service.bootstrap(FRIDAY, " BOOTSTRAP ");

        assertThat(created.getBusinessDate()).isEqualTo(FRIDAY);
        assertThat(created.getState()).isEqualTo(EodState.OPEN);
        assertThat(created.getCreatedBy()).isEqualTo("BOOTSTRAP");
        assertThat(created.getCreatedAt())
                .isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        verify(persistencePort).save(created);
    }

    @Test
    void bootstrapRetryReturnsInitialRowWithoutDuplicateSave() {
        DailyClosingStatus existing = DailyClosingStatus.bootstrap(
                FRIDAY,
                "BOOTSTRAP",
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        when(persistencePort.findLatestForUpdate()).thenReturn(Optional.of(existing));

        assertThat(service.bootstrap(FRIDAY, "RETRY")).isSameAs(existing);
        verify(persistencePort, never()).save(any());

        assertThatThrownBy(() -> service.bootstrap(MONDAY, "BOOTSTRAP"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only when no");
        verify(persistencePort, never()).save(any());
    }

    @Test
    void coordinatesLegalEodLifecycleAndDoesNotSaveDesiredStateRetries() {
        DailyClosingStatus status = DailyClosingStatus.bootstrap(
                FRIDAY,
                "BOOTSTRAP",
                LocalDateTime.of(2026, 7, 31, 0, 0));
        when(persistencePort.findByBusinessDateForUpdate(FRIDAY))
                .thenReturn(Optional.of(status));

        service.prepareEod(FRIDAY, "PREPARER");
        assertThat(status.getState()).isEqualTo(EodState.PRE_CLOSING);
        service.prepareEod(FRIDAY, "RETRY");

        service.startEod(FRIDAY, "CLOSER");
        assertThat(status.getState()).isEqualTo(EodState.CLOSING_IN_PROGRESS);
        service.startEod(FRIDAY, "RETRY");

        service.completeEod(FRIDAY, "APPROVER");
        assertThat(status.getState()).isEqualTo(EodState.CLOSED);
        service.completeEod(FRIDAY, "RETRY");

        verify(persistencePort, times(3)).save(status);
    }

    @Test
    void cancellationRetryIsIdempotentButFreshOpenCannotSkipPreparation() {
        DailyClosingStatus status = DailyClosingStatus.bootstrap(
                FRIDAY,
                "BOOTSTRAP",
                LocalDateTime.of(2026, 7, 31, 0, 0));
        when(persistencePort.findByBusinessDateForUpdate(FRIDAY))
                .thenReturn(Optional.of(status));

        assertThatThrownBy(() -> service.cancelEodPreparation(FRIDAY, "CANCELER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lineage");
        assertThatThrownBy(() -> service.completeBod(FRIDAY, "BOD"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lineage");

        service.prepareEod(FRIDAY, "PREPARER");
        service.cancelEodPreparation(FRIDAY, "CANCELER");
        service.cancelEodPreparation(FRIDAY, "RETRY");

        assertThat(status.getState()).isEqualTo(EodState.OPEN);
        verify(persistencePort, times(2)).save(status);
    }

    @Test
    void rolloverCreatesExplicitNextBusinessDateAndPreservesPriorClosedRow() {
        DailyClosingStatus prior = closedStatus(FRIDAY);
        when(persistencePort.findByBusinessDateForUpdate(FRIDAY))
                .thenReturn(Optional.of(prior));
        when(persistencePort.findLatestForUpdate()).thenReturn(Optional.of(prior));

        DailyClosingStatus next = service.startBod(FRIDAY, MONDAY, "BOD_STARTER");

        assertThat(next.getBusinessDate()).isEqualTo(MONDAY);
        assertThat(next.getState()).isEqualTo(EodState.BOD_IN_PROGRESS);
        assertThat(next.getBodStartedBy()).isEqualTo("BOD_STARTER");
        assertThat(prior.getBusinessDate()).isEqualTo(FRIDAY);
        assertThat(prior.getState()).isEqualTo(EodState.CLOSED);
        verify(persistencePort).findByBusinessDateForUpdate(FRIDAY);
        verify(persistencePort).save(next);
    }

    @Test
    void rolloverRetryReturnsExistingBodWithoutDuplicateSave() {
        DailyClosingStatus prior = closedStatus(FRIDAY);
        DailyClosingStatus next = DailyClosingStatus.beginBusinessDay(
                MONDAY,
                "BOD_STARTER",
                LocalDateTime.of(2026, 8, 3, 0, 0));
        when(persistencePort.findByBusinessDateForUpdate(FRIDAY))
                .thenReturn(Optional.of(prior));
        when(persistencePort.findLatestForUpdate()).thenReturn(Optional.of(next));
        when(persistencePort.findPreviousForUpdate(MONDAY)).thenReturn(Optional.of(prior));

        assertThat(service.startBod(FRIDAY, MONDAY, "RETRY")).isSameAs(next);
        verify(persistencePort, never()).save(any());
    }

    @Test
    void rolloverRetryRejectsAClosedDateThatIsNotTheDirectPredecessor() {
        DailyClosingStatus staleClosed = closedStatus(THURSDAY);
        DailyClosingStatus actualPredecessor = closedStatus(FRIDAY);
        DailyClosingStatus next = DailyClosingStatus.beginBusinessDay(
                MONDAY,
                "BOD_STARTER",
                LocalDateTime.of(2026, 8, 3, 0, 0));
        when(persistencePort.findByBusinessDateForUpdate(THURSDAY))
                .thenReturn(Optional.of(staleClosed));
        when(persistencePort.findLatestForUpdate()).thenReturn(Optional.of(next));
        when(persistencePort.findPreviousForUpdate(MONDAY))
                .thenReturn(Optional.of(actualPredecessor));

        assertThatThrownBy(() -> service.startBod(THURSDAY, MONDAY, "RETRY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("direct CLOSED predecessor");
        verify(persistencePort, never()).save(any());
    }

    @Test
    void completeBodOpensNewRowAndRetryDoesNotSaveAgain() {
        DailyClosingStatus next = DailyClosingStatus.beginBusinessDay(
                MONDAY,
                "BOD_STARTER",
                LocalDateTime.of(2026, 8, 3, 0, 0));
        when(persistencePort.findByBusinessDateForUpdate(MONDAY))
                .thenReturn(Optional.of(next));

        service.completeBod(MONDAY, "BOD_COMPLETER");
        service.completeBod(MONDAY, "RETRY");

        assertThat(next.getState()).isEqualTo(EodState.OPEN);
        assertThat(next.getOpenedBy()).isEqualTo("BOD_COMPLETER");
        verify(persistencePort).save(next);
    }

    @Test
    void rejectsInvalidRolloverDatesStatesAndHistoricalBranching() {
        assertThatThrownBy(() -> service.startBod(FRIDAY, FRIDAY, "ACTOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after");
        verifyNoInteractions(persistencePort);

        DailyClosingStatus open = DailyClosingStatus.bootstrap(
                FRIDAY,
                "BOOTSTRAP",
                LocalDateTime.of(2026, 7, 31, 0, 0));
        when(persistencePort.findByBusinessDateForUpdate(FRIDAY))
                .thenReturn(Optional.of(open));
        assertThatThrownBy(() -> service.startBod(FRIDAY, MONDAY, "ACTOR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");

        DailyClosingStatus prior = closedStatus(FRIDAY);
        DailyClosingStatus later = DailyClosingStatus.beginBusinessDay(
                MONDAY.plusDays(1),
                "OTHER",
                LocalDateTime.of(2026, 8, 4, 0, 0));
        when(persistencePort.findByBusinessDateForUpdate(FRIDAY))
                .thenReturn(Optional.of(prior));
        when(persistencePort.findLatestForUpdate()).thenReturn(Optional.of(later));
        assertThatThrownBy(() -> service.startBod(FRIDAY, MONDAY, "ACTOR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not the latest");
    }

    @Test
    void reportsMissingStatusAndValidatesInputsBeforePersistence() {
        when(persistencePort.findByBusinessDate(FRIDAY)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findStatus(FRIDAY))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(FRIDAY.toString());

        assertThatThrownBy(() -> service.prepareEod(null, "ACTOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("businessDate");
        assertThatThrownBy(() -> service.prepareEod(FRIDAY, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor");
    }

    private DailyClosingStatus closedStatus(LocalDate date) {
        LocalDateTime base = date.atStartOfDay();
        DailyClosingStatus status = DailyClosingStatus.bootstrap(date, "BOOTSTRAP", base);
        status.prepareEod("PREPARER", base.plusHours(1));
        status.startEod("CLOSER", base.plusHours(2));
        status.completeEod("APPROVER", base.plusHours(3));
        return status;
    }
}
