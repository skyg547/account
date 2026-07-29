package com.ho.account.closing.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyClosingStatusTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 30);
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 30, 9, 0);

    @Test
    void recordsEodTransitionActorsAndUtcCompatibleTimestamps() {
        DailyClosingStatus status = DailyClosingStatus.bootstrap(DATE, " BOOT ", T0);

        status.prepareEod(" PREPARER ", T0.plusHours(1));
        status.startEod("CLOSER", T0.plusHours(2));
        status.completeEod("APPROVER", T0.plusHours(3));

        assertThat(status.getBusinessDate()).isEqualTo(DATE);
        assertThat(status.getState()).isEqualTo(EodState.CLOSED);
        assertThat(status.getCreatedBy()).isEqualTo("BOOT");
        assertThat(status.getOpenedBy()).isEqualTo("BOOT");
        assertThat(status.getPreparedBy()).isEqualTo("PREPARER");
        assertThat(status.getPreparedAt()).isEqualTo(T0.plusHours(1));
        assertThat(status.getClosingStartedBy()).isEqualTo("CLOSER");
        assertThat(status.getClosingStartedAt()).isEqualTo(T0.plusHours(2));
        assertThat(status.getClosedBy()).isEqualTo("APPROVER");
        assertThat(status.getClosedAt()).isEqualTo(T0.plusHours(3));
        assertThat(status.getUpdatedBy()).isEqualTo("APPROVER");
        assertThat(status.getUpdatedAt()).isEqualTo(T0.plusHours(3));
    }

    @Test
    void cancellationAndBodOpeningUseOnlyTheirLegalOrigins() {
        DailyClosingStatus cancellation = DailyClosingStatus.bootstrap(DATE, "BOOT", T0);
        cancellation.prepareEod("PREPARER", T0.plusMinutes(1));
        cancellation.cancelEodPreparation("CANCELER", T0.plusMinutes(2));

        assertThat(cancellation.getState()).isEqualTo(EodState.OPEN);
        assertThat(cancellation.getOpenedBy()).isEqualTo("CANCELER");

        DailyClosingStatus bod = DailyClosingStatus.beginBusinessDay(
                DATE.plusDays(4),
                "BOD_STARTER",
                T0.plusDays(4));
        bod.completeBod("BOD_COMPLETER", T0.plusDays(4).plusMinutes(10));

        assertThat(bod.getState()).isEqualTo(EodState.OPEN);
        assertThat(bod.getBodStartedBy()).isEqualTo("BOD_STARTER");
        assertThat(bod.getOpenedBy()).isEqualTo("BOD_COMPLETER");
    }

    @Test
    void rejectsEveryIllegalSkipAndKeepsClosedRowTerminal() {
        DailyClosingStatus open = DailyClosingStatus.bootstrap(DATE, "BOOT", T0);
        assertThatThrownBy(() -> open.startEod("ACTOR", T0.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPEN")
                .hasMessageContaining("CLOSING_IN_PROGRESS");
        assertThatThrownBy(() -> open.completeEod("ACTOR", T0.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> open.completeBod("ACTOR", T0.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class);

        open.prepareEod("ACTOR", T0.plusMinutes(1));
        assertThatThrownBy(() -> open.completeEod("ACTOR", T0.plusMinutes(2)))
                .isInstanceOf(IllegalStateException.class);

        open.startEod("ACTOR", T0.plusMinutes(2));
        open.completeEod("ACTOR", T0.plusMinutes(3));
        assertThatThrownBy(() -> open.prepareEod("ACTOR", T0.plusMinutes(4)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> open.cancelEodPreparation("ACTOR", T0.plusMinutes(4)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> open.startEod("ACTOR", T0.plusMinutes(4)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> open.completeBod("ACTOR", T0.plusMinutes(4)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatesActorAndDoesNotExposePublicStateMutation() {
        assertThatThrownBy(() -> DailyClosingStatus.bootstrap(DATE, " ", T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor");
        assertThatThrownBy(() -> DailyClosingStatus.beginBusinessDay(
                DATE,
                "A".repeat(81),
                T0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("80");

        assertThat(Arrays.stream(DailyClosingStatus.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("set")))
                .isEmpty();
    }
}
