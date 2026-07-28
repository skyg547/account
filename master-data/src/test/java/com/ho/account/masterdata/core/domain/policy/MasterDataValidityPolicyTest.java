package com.ho.account.masterdata.core.domain.policy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterDataValidityPolicyTest {

    @Test
    void reportsActiveWhenDateFallsWithinWindow() {
        boolean active = MasterDataValidityPolicy.isActiveAt(
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30));

        assertThat(active).isTrue();
    }

    @Test
    void acceptsTerminationDateInsideCurrentValidityWindow() {
        LocalDate terminationDate = MasterDataValidityPolicy.requireTerminationDate(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(9999, 12, 31));

        assertThat(terminationDate).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void rejectsTerminationDateThatWouldReverseOrExtendHistory() {
        assertThatThrownBy(() -> MasterDataValidityPolicy.requireTerminationDate(
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(9999, 12, 31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before validFrom");

        assertThatThrownBy(() -> MasterDataValidityPolicy.requireTerminationDate(
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 7, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot extend");
    }

    @Test
    void rejectsReversedScd2WindowBeforePersistence() {
        assertThatThrownBy(() -> MasterDataValidityPolicy.requireValidityWindow(
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validTo cannot be before validFrom");
    }
}
