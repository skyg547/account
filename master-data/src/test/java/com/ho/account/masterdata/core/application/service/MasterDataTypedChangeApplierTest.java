package com.ho.account.masterdata.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.adapter.JacksonMasterDataChangePayloadDecoder;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterDataTypedChangeApplierTest {

    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 7, 1);

    @Mock
    private AccountSubjectUseCase accountSubjectUseCase;

    @Mock
    private BusinessPartnerUseCase businessPartnerUseCase;

    @Mock
    private DepartmentUseCase departmentUseCase;

    @Mock
    private ProductUseCase productUseCase;

    private MasterDataChangePayloadDecoder payloadDecoder;

    @BeforeEach
    void setUp() {
        payloadDecoder = new JacksonMasterDataChangePayloadDecoder(
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void accountSubjectCreateUsesTargetKeyAndApprovedEffectiveDate() {
        AccountSubjectMasterDataChangeApplier applier =
                new AccountSubjectMasterDataChangeApplier(accountSubjectUseCase, payloadDecoder);
        MasterDataChangeRequest request = request(
                MasterDataType.ACCOUNT_SUBJECT,
                "1000",
                ChangeType.CREATE,
                """
                {
                  "name": "현금",
                  "category": "ASSETS",
                  "balanceType": "DEBIT",
                  "reportLine": "현금및현금성자산",
                  "unsettled": false,
                  "fixedAsset": false
                }
                """);

        applier.apply(request);

        ArgumentCaptor<AccountSubjectCommand> commandCaptor =
                ArgumentCaptor.forClass(AccountSubjectCommand.class);
        verify(accountSubjectUseCase).createAccountSubject(commandCaptor.capture());
        assertThat(commandCaptor.getValue().code()).isEqualTo("1000");
        assertThat(commandCaptor.getValue().validFrom()).isEqualTo(EFFECTIVE_DATE);
    }

    @Test
    void departmentDeactivateDoesNotRequirePayloadAndUsesApprovedEffectiveDate() {
        DepartmentMasterDataChangeApplier applier =
                new DepartmentMasterDataChangeApplier(departmentUseCase, payloadDecoder);
        MasterDataChangeRequest request = request(
                MasterDataType.DEPARTMENT,
                "D-OLD",
                ChangeType.DEACTIVATE,
                null);

        applier.apply(request);

        verify(departmentUseCase).deactivateDepartment("D-OLD", EFFECTIVE_DATE);
    }

    @Test
    void businessPartnerUpdateResolvesCurrentTechnicalIdByBusinessKey() {
        BusinessPartner current = new BusinessPartner();
        current.setId(41L);
        current.setBusinessPartnerCode("BP-001");
        when(businessPartnerUseCase.getBusinessPartnerByCode("BP-001"))
                .thenReturn(Optional.of(current));
        BusinessPartnerMasterDataChangeApplier applier =
                new BusinessPartnerMasterDataChangeApplier(businessPartnerUseCase, payloadDecoder);
        MasterDataChangeRequest request = request(
                MasterDataType.BUSINESS_PARTNER,
                "BP-001",
                ChangeType.UPDATE,
                """
                {
                  "businessPartnerCode": "BP-001",
                  "businessPartnerName": "새 거래처명",
                  "partnerType": "VENDOR"
                }
                """);

        applier.apply(request);

        ArgumentCaptor<BusinessPartnerCommand> commandCaptor =
                ArgumentCaptor.forClass(BusinessPartnerCommand.class);
        verify(businessPartnerUseCase).updateBusinessPartner(
                org.mockito.ArgumentMatchers.eq(41L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().businessPartnerCode()).isEqualTo("BP-001");
        assertThat(commandCaptor.getValue().validFrom()).isEqualTo(EFFECTIVE_DATE);
    }

    @Test
    void productDeactivateResolvesCurrentVersionAndUsesApprovedEffectiveDate() {
        Product current = new Product();
        current.setId(72L);
        current.setProductCode("LOAN-001");
        when(productUseCase.getProductByProductCode("LOAN-001")).thenReturn(Optional.of(current));
        ProductMasterDataChangeApplier applier =
                new ProductMasterDataChangeApplier(productUseCase, payloadDecoder);
        MasterDataChangeRequest request = request(
                MasterDataType.PRODUCT,
                "LOAN-001",
                ChangeType.DEACTIVATE,
                null);

        applier.apply(request);

        verify(productUseCase).deactivateProduct(72L, EFFECTIVE_DATE);
    }

    @Test
    void rejectsPayloadWhoseBusinessKeyDiffersFromApprovedTargetKey() {
        AccountSubjectMasterDataChangeApplier applier =
                new AccountSubjectMasterDataChangeApplier(accountSubjectUseCase, payloadDecoder);
        MasterDataChangeRequest request = request(
                MasterDataType.ACCOUNT_SUBJECT,
                "1000",
                ChangeType.CREATE,
                """
                {
                  "code": "9999",
                  "name": "잘못된 계정",
                  "category": "ASSETS",
                  "balanceType": "DEBIT"
                }
                """);

        assertThatThrownBy(() -> applier.apply(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match change-request targetKey");
        verify(accountSubjectUseCase, never()).createAccountSubject(
                org.mockito.ArgumentMatchers.any(AccountSubjectCommand.class));
    }

    private MasterDataChangeRequest request(
            MasterDataType targetType,
            String targetKey,
            ChangeType changeType,
            String payloadJson) {
        return new MasterDataChangeRequest(
                targetType,
                targetKey,
                changeType,
                EFFECTIVE_DATE,
                1,
                "requester",
                "typed applier test",
                payloadJson);
    }
}