package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.closing.application.port.out.DailyClosingStatusPersistencePort;
import com.ho.account.closing.domain.DailyClosingStatus;
import com.ho.account.closing.domain.EodState;
import com.ho.account.closing.infrastructure.persistence.JpaDailyClosingStatusPersistenceAdapter;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(JpaDailyClosingStatusPersistenceAdapter.class)
class DailyClosingStatusPersistenceIntegrationTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 30);
    private static final LocalDateTime OPENED_AT = LocalDateTime.of(2026, 7, 30, 8, 0);

    @Autowired
    private DailyClosingStatusPersistencePort persistencePort;

    @Autowired
    private EntityManager entityManager;

    @Test
    void roundTripUsesLockedLookupAndIncrementsOptimisticVersion() {
        DailyClosingStatus created = persistencePort.save(
                DailyClosingStatus.bootstrap(BUSINESS_DATE, "opener", OPENED_AT));
        Long initialVersion = created.getVersion();
        entityManager.clear();

        DailyClosingStatus locked = persistencePort.findByBusinessDateForUpdate(BUSINESS_DATE)
                .orElseThrow();
        assertThat(locked.getState()).isEqualTo(EodState.OPEN);
        assertThat(locked.getCreatedBy()).isEqualTo("opener");
        assertThat(persistencePort.findPreviousForUpdate(BUSINESS_DATE.plusDays(1)))
                .hasValueSatisfying(previous -> {
                    assertThat(previous.getBusinessDate()).isEqualTo(locked.getBusinessDate());
                    assertThat(previous.getVersion()).isEqualTo(locked.getVersion());
                });

        locked.prepareEod("closer", OPENED_AT.plusHours(10));
        DailyClosingStatus updated = persistencePort.save(locked);
        entityManager.clear();

        DailyClosingStatus reloaded = persistencePort.findByBusinessDate(BUSINESS_DATE)
                .orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(EodState.PRE_CLOSING);
        assertThat(reloaded.getPreparedBy()).isEqualTo("closer");
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        assertThat(reloaded.getVersion()).isEqualTo(updated.getVersion());
        assertThatThrownBy(() -> persistencePort.save(created))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version changed");
    }
}
