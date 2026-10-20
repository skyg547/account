package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.infrastructure.adapter.JacksonMasterDataChangePayloadDecoder;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProductPriceApprovedChangeTest {

    private final ProductUseCase useCase = mock(ProductUseCase.class);
    private final ProductPersistencePort persistence = mock(ProductPersistencePort.class);
    private final ProductMasterDataChangeApplier applier = new ProductMasterDataChangeApplier(
            useCase, new JacksonMasterDataChangePayloadDecoder(
                    new ObjectMapper().registerModule(new JavaTimeModule())), persistence);

    @ParameterizedTest
    @EnumSource(value = ChangeType.class, names = {"CREATE", "UPDATE"})
    void invalidApprovedPriceFailsValidationAndApplyBeforeReadingOrWritingOldVersion(ChangeType changeType) {
        MasterDataChangeRequest request = request(changeType, "1.23456");
        request.approve("checker");

        assertThatThrownBy(() -> applier.validate(request)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> applier.apply(request)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(persistence);
        verify(useCase, never()).createProduct(any());
        verify(useCase, never()).updateProduct(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = ChangeType.class, names = {"CREATE", "UPDATE"})
    void negativeApprovedPriceFailsValidation(ChangeType changeType) {
        MasterDataChangeRequest request = request(changeType, "-0.0001");

        assertThatThrownBy(() -> applier.validate(request)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(persistence, useCase);
    }

    private static MasterDataChangeRequest request(ChangeType changeType, String price) {
        String payload = "{\"productCode\":\"P-7\",\"name\":\"product\","
                + "\"productType\":\"PHYSICAL\",\"price\":" + price + "}";
        return new MasterDataChangeRequest(MasterDataType.PRODUCT, "P-7", changeType,
                LocalDate.now().plusDays(1), 1, "maker", null, payload);
    }
}
