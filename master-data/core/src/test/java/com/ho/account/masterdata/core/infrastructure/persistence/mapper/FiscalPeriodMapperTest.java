package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod.ClosingStatus;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.FiscalPeriodEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FiscalPeriodMapperTest {

    private final FiscalPeriodMapper mapper = new FiscalPeriodMapper();

    @ParameterizedTest
    @EnumSource(ClosingStatus.class)
    void restoresEveryStoredStatusWithoutChangingAnyValue(ClosingStatus status) {
        FiscalPeriodEntity stored = storedPeriod(status);

        FiscalPeriod restored = mapper.toDomain(stored);

        assertSameValues(restored, stored);
    }

    @ParameterizedTest
    @EnumSource(ClosingStatus.class)
    void roundTripsEveryStatusWithoutChangingAnyValue(ClosingStatus status) {
        FiscalPeriodEntity stored = storedPeriod(status);

        FiscalPeriodEntity roundTripped = mapper.toEntity(mapper.toDomain(stored));

        assertSameValues(mapper.toDomain(roundTripped), stored);
    }

    @Test
    void roundTripsLegalPermanentClosingCommand() {
        FiscalPeriod period = mapper.toDomain(storedPeriod(ClosingStatus.CLOSED));
        period.changeClosingStatus(ClosingStatus.PERMANENTLY_CLOSED, "closing-command");

        FiscalPeriodEntity saved = mapper.toEntity(period);

        assertSameValues(mapper.toDomain(saved), saved);
        assertThat(saved.getClosingStatus()).isEqualTo(ClosingStatus.PERMANENTLY_CLOSED);
        assertThat(saved.getAuditUser()).isEqualTo("closing-command");
        assertThat(saved.getUpdatedAt()).isEqualTo(period.getUpdatedAt());
    }

    @Test
    void preservesNullMetadataWithoutCreatingAuditValues() {
        FiscalPeriodEntity stored = storedPeriod(ClosingStatus.CLOSED);
        stored.setCreatedAt(null);
        stored.setUpdatedAt(null);
        stored.setAuditUser(null);

        assertSameValues(mapper.toDomain(mapper.toEntity(mapper.toDomain(stored))), stored);
    }

    @Test
    void rejectsMissingStoredStatusInsteadOfDefaultingToOpen() {
        assertThatThrownBy(() -> mapper.toDomain(storedPeriod(null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Closing status is required.");
    }

    @Test
    void preservesNullMappingContract() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }

    private FiscalPeriodEntity storedPeriod(ClosingStatus status) {
        FiscalPeriodEntity stored = new FiscalPeriodEntity();
        stored.setId(683L);
        stored.setFiscalYear("2026");
        stored.setFiscalPeriod("09");
        stored.setStartDate(LocalDate.of(2026, 9, 1));
        stored.setEndDate(LocalDate.of(2026, 9, 30));
        stored.setClosingStatus(status);
        stored.setCreatedAt(LocalDateTime.of(2025, 12, 1, 8, 15, 12));
        stored.setUpdatedAt(LocalDateTime.of(2026, 8, 31, 17, 45, 23));
        stored.setAuditUser("  stored-auditor  ");
        return stored;
    }

    private void assertSameValues(FiscalPeriod actual, FiscalPeriodEntity expected) {
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getFiscalYear()).isEqualTo(expected.getFiscalYear());
        assertThat(actual.getFiscalPeriod()).isEqualTo(expected.getFiscalPeriod());
        assertThat(actual.getStartDate()).isEqualTo(expected.getStartDate());
        assertThat(actual.getEndDate()).isEqualTo(expected.getEndDate());
        assertThat(actual.getClosingStatus()).isEqualTo(expected.getClosingStatus());
        assertThat(actual.getCreatedAt()).isEqualTo(expected.getCreatedAt());
        assertThat(actual.getUpdatedAt()).isEqualTo(expected.getUpdatedAt());
        assertThat(actual.getAuditUser()).isEqualTo(expected.getAuditUser());
    }
}
