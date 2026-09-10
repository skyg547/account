package com.ho.account.masterdata.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod.ClosingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FiscalPeriodTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);
    private static final LocalDateTime CREATED = LocalDateTime.of(2025, 12, 1, 8, 15, 12);
    private static final LocalDateTime UPDATED = LocalDateTime.of(2026, 8, 31, 17, 45, 23);
    private static final String STORED_ACTOR = "  stored-auditor  ";

    @ParameterizedTest
    @EnumSource(ClosingStatus.class)
    void reconstitutesAllValuesWithoutExecutingACommand(ClosingStatus status) {
        FiscalPeriod restored = storedPeriod(status);

        assertThat(restored.getId()).isEqualTo(683L);
        assertThat(restored.getFiscalYear()).isEqualTo("2026");
        assertThat(restored.getFiscalPeriod()).isEqualTo("09");
        assertThat(restored.getStartDate()).isEqualTo(START);
        assertThat(restored.getEndDate()).isEqualTo(END);
        assertThat(restored.getClosingStatus()).isEqualTo(status);
        assertUnchangedAudit(restored);
    }

    @Test
    void preservesNullValuesOtherThanRequiredStatus() {
        FiscalPeriod restored = FiscalPeriod.reconstitute(
                null, null, null, null, null, ClosingStatus.CLOSED, null, null, null);

        assertThat(restored.getId()).isNull();
        assertThat(restored.getFiscalYear()).isNull();
        assertThat(restored.getFiscalPeriod()).isNull();
        assertThat(restored.getStartDate()).isNull();
        assertThat(restored.getEndDate()).isNull();
        assertThat(restored.getClosingStatus()).isEqualTo(ClosingStatus.CLOSED);
        assertThat(restored.getCreatedAt()).isNull();
        assertThat(restored.getUpdatedAt()).isNull();
        assertThat(restored.getAuditUser()).isNull();
    }

    @Test
    void doesNotAddNormalizationOrDatePolicyDuringReconstitution() {
        FiscalPeriod restored = FiscalPeriod.reconstitute(
                683L, " year ", " period ", END, START, ClosingStatus.CLOSED, CREATED, UPDATED, " ");

        assertThat(restored.getFiscalYear()).isEqualTo(" year ");
        assertThat(restored.getFiscalPeriod()).isEqualTo(" period ");
        assertThat(restored.getStartDate()).isEqualTo(END);
        assertThat(restored.getEndDate()).isEqualTo(START);
        assertThat(restored.getAuditUser()).isEqualTo(" ");
    }

    @Test
    void rejectsMissingStoredStatus() {
        assertThatThrownBy(() -> storedPeriod(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Closing status is required.");
    }

    @ParameterizedTest
    @CsvSource({"OPEN, OPEN", "OPEN, CLOSED", "CLOSED, OPEN", "CLOSED, CLOSED",
            "CLOSED, PERMANENTLY_CLOSED", "PERMANENTLY_CLOSED, PERMANENTLY_CLOSED"})
    void retainsAllLegalTransitionsAndCommandAudit(ClosingStatus current, ClosingStatus next) {
        FiscalPeriod period = storedPeriod(current);
        LocalDateTime beforeCommand = LocalDateTime.now();

        period.changeClosingStatus(next, "  closing-command  ");

        assertThat(period.getClosingStatus()).isEqualTo(next);
        assertThat(period.getAuditUser()).isEqualTo("closing-command");
        assertThat(period.getCreatedAt()).isEqualTo(CREATED);
        assertThat(period.getUpdatedAt()).isBetween(beforeCommand, LocalDateTime.now());
    }

    @ParameterizedTest
    @CsvSource({"OPEN, PERMANENTLY_CLOSED", "PERMANENTLY_CLOSED, OPEN", "PERMANENTLY_CLOSED, CLOSED"})
    void rejectsIllegalTransitionsWithoutChangingStateOrAudit(ClosingStatus current, ClosingStatus next) {
        FiscalPeriod period = storedPeriod(current);

        assertThatThrownBy(() -> period.changeClosingStatus(next, "closing-command"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(period.getClosingStatus()).isEqualTo(current);
        assertUnchangedAudit(period);
    }

    @Test
    void newPeriodStillMustCloseBeforePermanentClosing() {
        FiscalPeriod period = new FiscalPeriod();

        assertThatThrownBy(() -> period.changeClosingStatus(ClosingStatus.PERMANENTLY_CLOSED, "closing-command"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fiscal period must be closed before permanent closing.");
        assertThat(period.getClosingStatus()).isNull();
        assertThat(period.getAuditUser()).isNull();
        assertThat(period.getUpdatedAt()).isNull();

        period.changeClosingStatus(ClosingStatus.CLOSED, "closing-command");
        assertThat(period.getClosingStatus()).isEqualTo(ClosingStatus.CLOSED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsInvalidCommandActorWithoutChangingStateOrAudit(String actor) {
        FiscalPeriod period = storedPeriod(ClosingStatus.CLOSED);

        assertThatThrownBy(() -> period.changeClosingStatus(ClosingStatus.PERMANENTLY_CLOSED, actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Closing status audit user is required.");

        assertThat(period.getClosingStatus()).isEqualTo(ClosingStatus.CLOSED);
        assertUnchangedAudit(period);
    }

    @Test
    void rejectsNullCommandStatusWithoutChangingStateOrAudit() {
        FiscalPeriod period = storedPeriod(ClosingStatus.CLOSED);

        assertThatThrownBy(() -> period.changeClosingStatus(null, "closing-command"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Closing status is required.");

        assertThat(period.getClosingStatus()).isEqualTo(ClosingStatus.CLOSED);
        assertUnchangedAudit(period);
    }

    private FiscalPeriod storedPeriod(ClosingStatus status) {
        return FiscalPeriod.reconstitute(683L, "2026", "09", START, END, status, CREATED, UPDATED, STORED_ACTOR);
    }

    private void assertUnchangedAudit(FiscalPeriod period) {
        assertThat(period.getCreatedAt()).isEqualTo(CREATED);
        assertThat(period.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(period.getAuditUser()).isEqualTo(STORED_ACTOR);
    }
}
