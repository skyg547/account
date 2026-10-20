package com.ho.account.masterdata.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MonolithMasterDataQueryAdapterTest {

    private final BusinessPartnerPersistencePort partners = mock(BusinessPartnerPersistencePort.class);
    private final MonolithMasterDataQueryAdapter adapter = new MonolithMasterDataQueryAdapter(
            mock(AccountSubjectPersistencePort.class), partners, mock(DepartmentPersistencePort.class));

    @ParameterizedTest
    @CsvSource({"2026-01-01, 2026-01-01", "2025-01-01, 2026-01-01"})
    void failsClosedForTwoActiveVersionsOfOneRequestedCode(
            LocalDate firstValidFrom, LocalDate secondValidFrom) {
        when(partners.findAllByBusinessPartnerCodeIn(List.of("BP-DUP", "BP-OK")))
                .thenReturn(List.of(
                        partner("BP-DUP", "첫 버전", firstValidFrom),
                        partner("BP-OK", "정상 거래처", LocalDate.of(2026, 1, 1)),
                        partner("BP-DUP", "두 번째 버전", secondValidFrom)));

        // 정상 키가 함께 있어도 손상된 응답 전체를 성공한 Map으로 반환해서는 안 됩니다.
        assertThatThrownBy(() -> adapter.findAllByPartnerCodes(List.of("BP-DUP", "BP-OK")))
                .isInstanceOfSatisfying(DuplicateBusinessPartnerReferenceException.class, error -> {
                    assertThat(error.businessPartnerCode()).isEqualTo("BP-DUP");
                    assertThat(error).hasMessageContaining("BP-DUP");
                });
    }

    @Test
    void repeatedRequestedCodeWithOneStoredVersionSucceeds() {
        when(partners.findAllByBusinessPartnerCodeIn(List.of("BP-ONE", "BP-ONE")))
                .thenReturn(List.of(partner("BP-ONE", "유일 거래처", LocalDate.of(2026, 1, 1))));

        Map<String, BusinessPartnerRef> result = adapter.findAllByPartnerCodes(List.of("BP-ONE", "BP-ONE"));

        assertThat(result).containsOnlyKeys("BP-ONE");
        assertThat(result.get("BP-ONE").name()).isEqualTo("유일 거래처");
    }

    @Test
    void distinctRequestedCodesReturnTheirOwnReferences() {
        when(partners.findAllByBusinessPartnerCodeIn(List.of("BP-A", "BP-B")))
                .thenReturn(List.of(
                        partner("BP-B", "두 번째 거래처", LocalDate.of(2026, 1, 1)),
                        partner("BP-A", "첫 거래처", LocalDate.of(2025, 1, 1))));

        Map<String, BusinessPartnerRef> result = adapter.findAllByPartnerCodes(List.of("BP-A", "BP-B"));

        assertThat(result).containsOnlyKeys("BP-A", "BP-B");
        assertThat(result.get("BP-A").name()).isEqualTo("첫 거래처");
        assertThat(result.get("BP-B").name()).isEqualTo("두 번째 거래처");
    }

    @Test
    void nullAndEmptyRequestsReturnEmptyWithoutQueryingPersistence() {
        assertThat(adapter.findAllByPartnerCodes(null)).isEmpty();
        assertThat(adapter.findAllByPartnerCodes(List.of())).isEmpty();
        verifyNoInteractions(partners);
    }

    private BusinessPartner partner(String code, String name, LocalDate validFrom) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 9, 30);
        return BusinessPartner.reconstitute(
                null, code, name, null, null, null, null,
                BusinessPartner.PartnerType.VENDOR, true,
                BusinessPartner.KycStatus.APPROVED, BusinessPartner.RiskRating.LOW,
                validFrom, BusinessPartner.OPEN_ENDED_VALID_TO,
                timestamp, timestamp, "test", List.of());
    }
}
