package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.infrastructure.adapter.JacksonMasterDataChangePayloadDecoder;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MasterDataChangeRequestPayloadValidationTest {

    @Test
    void approveKeepsRequestPendingWhenBusinessPartnerPayloadIsMalformed() {
        MasterDataChangeRequestPersistencePort persistencePort =
                mock(MasterDataChangeRequestPersistencePort.class);
        MasterDataVersionQueryPort versionQueryPort = mock(MasterDataVersionQueryPort.class);
        BusinessPartnerUseCase businessPartnerUseCase = mock(BusinessPartnerUseCase.class);
        BusinessPartnerMasterDataChangeApplier applier = new BusinessPartnerMasterDataChangeApplier(
                businessPartnerUseCase,
                new JacksonMasterDataChangePayloadDecoder(
                        new ObjectMapper().registerModule(new JavaTimeModule())));
        MasterDataChangeRequestService service = new MasterDataChangeRequestService(
                persistencePort, versionQueryPort, List.of(applier));
        MasterDataChangeRequest request = new MasterDataChangeRequest(
                MasterDataType.BUSINESS_PARTNER,
                "BP-BROKEN",
                ChangeType.CREATE,
                LocalDate.of(2026, 8, 1),
                1,
                "requester",
                "broken payload",
                "{\"businessPartnerCode\":");
        when(persistencePort.findByIdForUpdate(42L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(42L, "approver"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid BUSINESS_PARTNER change payload");

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        verify(persistencePort, never()).save(request);
    }
}
