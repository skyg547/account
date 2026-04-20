package com.ho.account.masterdata.core.domain.policy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    void closesValidityWindowUsingTodayWhenStillOpen() {
        Holder holder = new Holder(LocalDate.of(9999, 12, 31));

        MasterDataValidityPolicy.closeIfActive(holder::getDate, holder::setDate);

        assertThat(holder.getDate()).isEqualTo(LocalDate.now());
    }

    private static final class Holder {
        private LocalDate date;

        private Holder(LocalDate date) {
            this.date = date;
        }

        private LocalDate getDate() {
            return date;
        }

        private void setDate(LocalDate date) {
            this.date = date;
        }
    }
}
