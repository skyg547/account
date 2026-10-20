package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.infrastructure.adapter.JacksonMasterDataChangePayloadDecoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Schema column boundaries exercised through the real governance decoder and appliers. */
class MasterDataFieldBoundaryTest {
    private static final LocalDate EFFECTIVE = LocalDate.of(2026, 8, 1);
    private static final String KEY = "MD-913";

    private final MasterDataChangeRequestPersistencePort requests = mock(MasterDataChangeRequestPersistencePort.class);
    private final MasterDataVersionQueryPort versions = mock(MasterDataVersionQueryPort.class);
    private final AccountSubjectPersistencePort accountRows = mock(AccountSubjectPersistencePort.class);
    private final BusinessPartnerPersistencePort partnerRows = mock(BusinessPartnerPersistencePort.class);
    private final DepartmentPersistencePort departmentRows = mock(DepartmentPersistencePort.class);
    private final ProductPersistencePort productRows = mock(ProductPersistencePort.class);
    private final AccountSubjectUseCase accounts = mock(AccountSubjectUseCase.class);
    private final BusinessPartnerUseCase partners = mock(BusinessPartnerUseCase.class);
    private final DepartmentUseCase departments = mock(DepartmentUseCase.class);
    private final ProductUseCase products = mock(ProductUseCase.class);
    private MasterDataChangeRequestService service;

    @BeforeEach
    void setUp() {
        var decoder = new JacksonMasterDataChangePayloadDecoder(
                new ObjectMapper().registerModule(new JavaTimeModule()));
        service = new MasterDataChangeRequestService(requests, versions, List.of(
                new AccountSubjectMasterDataChangeApplier(accounts, decoder, accountRows),
                new BusinessPartnerMasterDataChangeApplier(partners, decoder, partnerRows),
                new DepartmentMasterDataChangeApplier(departments, decoder, departmentRows),
                new ProductMasterDataChangeApplier(products, decoder, productRows)),
                mock(MasterDataBusinessKeyLockPort.class));
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest(name = "{0}.{1} max {2}")
    @MethodSource("storedTextFields")
    void acceptsExactlyColumnMaximum(MasterDataType type, String field, int maximum) {
        when(versions.countPersistedVersions(type, KEY)).thenReturn(0L);

        MasterDataChangeRequest request = service.requestChange(command(type, KEY, payload(type, field, "X".repeat(maximum))));

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        verify(requests).save(any());
    }

    @ParameterizedTest(name = "{0}.{1} max+1 {2}")
    @MethodSource("storedTextFields")
    void rejectsColumnOverflowBeforeRequestPersistence(MasterDataType type, String field, int maximum) {
        when(versions.countPersistedVersions(type, KEY)).thenReturn(0L);
        String oversized = "X".repeat(maximum + 1);

        assertThatThrownBy(() -> service.requestChange(command(type, KEY, payload(type, field, oversized))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining(oversized);

        verify(requests, never()).save(any());
    }

    @ParameterizedTest(name = "{0}.{1} stored max+1 {2}")
    @MethodSource("storedTextFields")
    void rejectsPreviouslyStoredOverflowBeforeApproval(MasterDataType type, String field, int maximum) {
        String oversized = "X".repeat(maximum + 1);
        MasterDataChangeRequest request = MasterDataChangeRequest.reconstitute(
                913L, type, KEY, ChangeType.CREATE, ChangeStatus.REQUESTED, 3L,
                EFFECTIVE, 1, "maker", null, LocalDateTime.of(2026, 7, 1, 9, 0), null,
                "boundary", payload(type, field, oversized), null, null);
        when(versions.countPersistedVersions(type, KEY)).thenReturn(0L);
        when(requests.findById(913L)).thenReturn(Optional.of(request));
        when(requests.findByIdForUpdate(913L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(913L, "checker"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining(oversized);

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(request.getApprovedBy()).isNull();
        assertThat(request.getLockVersion()).isEqualTo(3L);
        verify(requests, never()).save(any());
    }

    @ParameterizedTest(name = "{0} target key max {1}")
    @MethodSource("targetKeyLimits")
    void targetKeyAcceptsMaximumAndRejectsOverflow(MasterDataType type, int maximum) {
        String valid = "K".repeat(maximum);
        String invalid = valid + "K";
        when(versions.countPersistedVersions(type, valid)).thenReturn(0L);

        assertThat(service.requestChange(command(type, valid, basePayload(type))).getTargetKey()).isEqualTo(valid);
        assertThatThrownBy(() -> service.requestChange(command(type, invalid, basePayload(type))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining(invalid);
        verify(requests).save(any());
    }

    @ParameterizedTest(name = "product price {0} accepted={1}")
    @MethodSource("priceBoundaries")
    void productPriceHonorsNumericNineteenFour(String price, boolean accepted) {
        when(versions.countPersistedVersions(MasterDataType.PRODUCT, KEY)).thenReturn(0L);
        String payload = "{\"name\":\"Valid\",\"productType\":\"SERVICE\",\"price\":" + price + "}";

        if (accepted) {
            assertThat(service.requestChange(command(MasterDataType.PRODUCT, KEY, payload)).getStatus())
                    .isEqualTo(ChangeStatus.REQUESTED);
        } else {
            assertThatThrownBy(() -> service.requestChange(command(MasterDataType.PRODUCT, KEY, payload)))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(requests, never()).save(any());
        }
    }

    private static Stream<Arguments> priceBoundaries() {
        return Stream.of(
                Arguments.of("999999999999999.9999", true),
                Arguments.of("-999999999999999.9999", false),
                Arguments.of("1000000000000000.0000", false),
                Arguments.of("1.0000", true),
                Arguments.of("1.00001", false));
    }

    private static Stream<Arguments> targetKeyLimits() {
        return Stream.of(
                Arguments.of(MasterDataType.ACCOUNT_SUBJECT, 20),
                Arguments.of(MasterDataType.BUSINESS_PARTNER, 20),
                Arguments.of(MasterDataType.DEPARTMENT, 20),
                Arguments.of(MasterDataType.PRODUCT, 50));
    }

    private static Stream<Arguments> storedTextFields() {
        return Stream.of(
                Arguments.of(MasterDataType.ACCOUNT_SUBJECT, "name", 100),
                Arguments.of(MasterDataType.ACCOUNT_SUBJECT, "parentCode", 20),
                Arguments.of(MasterDataType.ACCOUNT_SUBJECT, "reportLine", 100),
                Arguments.of(MasterDataType.ACCOUNT_SUBJECT, "regulatoryMappingCode", 100),
                Arguments.of(MasterDataType.DEPARTMENT, "name", 100),
                Arguments.of(MasterDataType.DEPARTMENT, "parentCode", 20),
                Arguments.of(MasterDataType.PRODUCT, "name", 200),
                Arguments.of(MasterDataType.PRODUCT, "description", 500),
                Arguments.of(MasterDataType.PRODUCT, "unitOfMeasure", 20),
                Arguments.of(MasterDataType.BUSINESS_PARTNER, "businessPartnerName", 100),
                Arguments.of(MasterDataType.BUSINESS_PARTNER, "registrationNumber", 20),
                Arguments.of(MasterDataType.BUSINESS_PARTNER, "ceoName", 50),
                Arguments.of(MasterDataType.BUSINESS_PARTNER, "businessType", 50),
                Arguments.of(MasterDataType.BUSINESS_PARTNER, "businessItem", 50));
    }

    private static MasterDataChangeRequestCommand command(MasterDataType type, String key, String payload) {
        return new MasterDataChangeRequestCommand(type, key, ChangeType.CREATE, EFFECTIVE,
                1, "maker", "boundary", payload, null);
    }

    private static String payload(MasterDataType type, String field, String value) {
        return basePayload(type).replaceFirst("}$", ",\"" + field + "\":\"" + value + "\"}");
    }

    private static String basePayload(MasterDataType type) {
        return switch (type) {
            case ACCOUNT_SUBJECT, DEPARTMENT -> "{\"name\":\"Valid\"}";
            case PRODUCT -> "{\"name\":\"Valid\",\"productType\":\"SERVICE\",\"price\":0}";
            case BUSINESS_PARTNER -> "{\"businessPartnerName\":\"Valid\"}";
            default -> throw new IllegalArgumentException("Unexpected type: " + type);
        };
    }
}
