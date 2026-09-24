package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.PeriodLock;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClosingAdmissionServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31);

    @Mock
    private FiscalPeriodControlPort fiscalPeriodControlPort;
    @Mock
    private ClosingCalendarPersistencePort closingCalendarPersistencePort;
    @Mock
    private PeriodLockPersistencePort periodLockPersistencePort;

    private ClosingAdmissionService service;

    @BeforeEach
    void setUp() {
        service = new ClosingAdmissionService(
                fiscalPeriodControlPort, closingCalendarPersistencePort, periodLockPersistencePort);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 15, 31})
    void matchingOpenPeriodAndCalendarWithoutLockAllowEntryIncludingBoundaries(int day) {
        openMasterAndCalendar();
        when(periodLockPersistencePort.findByFiscalPeriodId(1L)).thenReturn(Optional.empty());

        assertThat(service.isClosed(DATE.withDayOfMonth(day))).isFalse();

        verify(periodLockPersistencePort).findByFiscalPeriodId(1L);
        verify(fiscalPeriodControlPort, never()).updateClosingStatus(any(), any(), any());
        verify(closingCalendarPersistencePort, never()).save(any());
        verify(periodLockPersistencePort, never()).save(any());
        verify(periodLockPersistencePort, never()).delete(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"CLOSED", "PERMANENTLY_CLOSED", "IN_PROGRESS", "UNKNOWN", "open", " OPEN "})
    void onlyExplicitMasterOpenCanAllowEntry(String status) {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(period(status)));

        assertThat(service.isClosed(DATE)).isTrue();

        verifyNoInteractions(closingCalendarPersistencePort, periodLockPersistencePort);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = ClosingCalendarStatus.class, names = "OPEN", mode = EnumSource.Mode.EXCLUDE)
    void onlyExplicitCalendarOpenCanAllowEntry(ClosingCalendarStatus status) {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(period("OPEN")));
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar("2026", "01", status)));

        assertThat(service.isClosed(DATE)).isTrue();

        verifyNoInteractions(periodLockPersistencePort);
    }

    @Test
    void missingCalendarBlocksEntry() {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(period("OPEN")));
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.empty());

        assertThat(service.isClosed(DATE)).isTrue();
    }

    @Test
    void nullCalendarLookupCannotProveOpen() {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(period("OPEN")));
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(null);

        assertThat(service.isClosed(DATE)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("mismatchedCalendars")
    void wrongCalendarSnapshotCannotAuthorizeEntry(ClosingCalendar calendar) {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(period("OPEN")));
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar));

        assertThat(service.isClosed(DATE)).isTrue();

        verifyNoInteractions(periodLockPersistencePort);
    }

    @Test
    void evenLockWithoutRecognizedTypeBlocksEntry() {
        openMasterAndCalendar();
        PeriodLock lock = new PeriodLock();
        lock.setFiscalPeriodId(1L);
        when(periodLockPersistencePort.findByFiscalPeriodId(1L)).thenReturn(Optional.of(lock));

        assertThat(service.isClosed(DATE)).isTrue();
    }

    @Test
    void missingMasterPeriodRejectsOperation() {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.isClosed(DATE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Fiscal period is missing");

        verifyNoInteractions(closingCalendarPersistencePort, periodLockPersistencePort);
    }

    @Test
    void nullMasterLookupRejectsOperation() {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(null);

        assertThatThrownBy(() -> service.isClosed(DATE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Fiscal period is missing");
    }

    @ParameterizedTest
    @MethodSource("invalidPeriods")
    void malformedOrMismatchedMasterSnapshotRejectsBeforeLockLookup(FiscalPeriodRef period) {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.isClosed(DATE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Invalid fiscal period");

        verifyNoInteractions(closingCalendarPersistencePort, periodLockPersistencePort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"master", "calendar", "lock"})
    void lookupFailuresPropagateAndCannotAuthorizeEntry(String failedLookup) {
        IllegalStateException failure = new IllegalStateException("Synthetic lookup failure");
        if ("master".equals(failedLookup)) {
            when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenThrow(failure);
        } else if ("calendar".equals(failedLookup)) {
            when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01"))
                    .thenReturn(Optional.of(period("OPEN")));
            when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                    .thenThrow(failure);
        } else {
            openMasterAndCalendar();
            when(periodLockPersistencePort.findByFiscalPeriodId(1L)).thenThrow(failure);
        }

        assertThatThrownBy(() -> service.isClosed(DATE)).isSameAs(failure);
    }

    @Test
    void nullLockLookupCannotBeTreatedAsUnlocked() {
        openMasterAndCalendar();
        when(periodLockPersistencePort.findByFiscalPeriodId(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.isClosed(DATE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Period lock lookup");
    }

    @Test
    void nullAccountingDateRejectsBeforeLookups() {
        assertThatThrownBy(() -> service.isClosed(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("accountingDate");

        verifyNoInteractions(fiscalPeriodControlPort, closingCalendarPersistencePort, periodLockPersistencePort);
    }

    @Test
    @ResourceLock("java.util.Locale.default")
    void monthLookupUsesAsciiDigitsWhenDefaultLocaleUsesDifferentDigits() {
        openMasterAndCalendar();
        when(periodLockPersistencePort.findByFiscalPeriodId(1L)).thenReturn(Optional.empty());
        Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ar-EG"));
            assertThat(service.isClosed(DATE)).isFalse();
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }

        verify(fiscalPeriodControlPort).findFiscalPeriod("2026", "01");
    }

    private void openMasterAndCalendar() {
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "01")).thenReturn(Optional.of(period("OPEN")));
        when(closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod("2026", "01"))
                .thenReturn(Optional.of(calendar("2026", "01", ClosingCalendarStatus.OPEN)));
    }

    private static FiscalPeriodRef period(String status) {
        return new FiscalPeriodRef(1L, "2026", "01", START, END, status);
    }

    private static ClosingCalendar calendar(String year, String month, ClosingCalendarStatus status) {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(10L);
        calendar.setFiscalYear(year);
        calendar.setFiscalPeriod(month);
        calendar.setStatus(status);
        return calendar;
    }

    private static Stream<ClosingCalendar> mismatchedCalendars() {
        return Stream.of(
                calendar("2025", "01", ClosingCalendarStatus.OPEN),
                calendar("2026", "02", ClosingCalendarStatus.OPEN),
                calendar("2026", "1", ClosingCalendarStatus.OPEN),
                calendar(null, "01", ClosingCalendarStatus.OPEN),
                calendar("2026", null, ClosingCalendarStatus.OPEN));
    }

    private static Stream<FiscalPeriodRef> invalidPeriods() {
        return Stream.of(
                new FiscalPeriodRef(null, "2026", "01", START, END, "OPEN"),
                new FiscalPeriodRef(0L, "2026", "01", START, END, "OPEN"),
                new FiscalPeriodRef(-1L, "2026", "01", START, END, "OPEN"),
                new FiscalPeriodRef(1L, null, "01", START, END, "OPEN"),
                new FiscalPeriodRef(1L, "2025", "01", START, END, "OPEN"),
                new FiscalPeriodRef(1L, "2026", null, START, END, "OPEN"),
                new FiscalPeriodRef(1L, "2026", "02", START, END, "OPEN"),
                new FiscalPeriodRef(1L, "2026", "1", START, END, "OPEN"),
                new FiscalPeriodRef(1L, "2026", "01", null, END, "OPEN"),
                new FiscalPeriodRef(1L, "2026", "01", START, null, "OPEN"),
                new FiscalPeriodRef(1L, "2026", "01", END, START, "OPEN"),
                new FiscalPeriodRef(1L, "2026", "01", DATE.plusDays(1), END, "OPEN"),
                new FiscalPeriodRef(1L, "2026", "01", START, DATE.minusDays(1), "OPEN"));
    }
}
