package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.AccountSubject.AccountType;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.adapter.JacksonMasterDataChangePayloadDecoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class MasterDataChangeRequestPayloadValidationTest {

    private static final String TARGET_KEY = "MD-668";
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 8, 1);
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 7, 1, 9, 30);
    private static final List<MasterDataType> TARGET_TYPES = List.of(
            MasterDataType.ACCOUNT_SUBJECT, MasterDataType.DEPARTMENT, MasterDataType.PRODUCT);

    private final MasterDataChangeRequestPersistencePort persistence = mock(MasterDataChangeRequestPersistencePort.class);
    private final MasterDataVersionQueryPort versions = mock(MasterDataVersionQueryPort.class);
    private final AccountSubjectUseCase accounts = mock(AccountSubjectUseCase.class);
    private final DepartmentUseCase departments = mock(DepartmentUseCase.class);
    private final ProductUseCase products = mock(ProductUseCase.class);
    private MasterDataChangeRequestService service;

    @BeforeEach
    void setUpRealDecoderAppliersAndService() {
        JacksonMasterDataChangePayloadDecoder decoder = new JacksonMasterDataChangePayloadDecoder(
                new ObjectMapper().registerModule(new JavaTimeModule()));
        service = new MasterDataChangeRequestService(persistence, versions, List.of(
                new AccountSubjectMasterDataChangeApplier(accounts, decoder),
                new DepartmentMasterDataChangeApplier(departments, decoder),
                new ProductMasterDataChangeApplier(products, decoder)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPayloads")
    void rejectsNewRequestBeforeSavingWithoutSourceReference(InvalidPayload input) {
        stubValidVersion(input.type(), input.change());

        assertThatThrownBy(() -> service.requestChange(requestCommand(input, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(input.message());

        verify(persistence, never()).save(any());
        verifyNoInteractions(accounts, departments, products);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPayloads")
    void rejectsFirstSourceReferenceRequestBeforeSaving(InvalidPayload input) {
        stubValidVersion(input.type(), input.change());
        when(persistence.findBySourceReference("source-668")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestChange(requestCommand(input, "source-668")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(input.message());

        verify(persistence, never()).save(any());
        verifyNoInteractions(accounts, departments, products);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPayloads")
    void rejectsApprovalBeforeChangingStatusAuditOrLockVersion(InvalidPayload input) {
        stubValidVersion(input.type(), input.change());
        // 복원된 과거 REQUESTED 행은 생성자의 raw-payload 검사도 우회했을 수 있습니다.
        MasterDataChangeRequest request = pendingRequest(input.type(), input.change(), input.json(), null);
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(668L, "approver"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(input.message());

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(request.getApprovedBy()).isNull();
        assertThat(request.getApprovedAt()).isNull();
        assertThat(request.getLockVersion()).isEqualTo(7L);
        assertThat(request.getRequestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(request.getRequestedBy()).isEqualTo("requester");
        assertThat(request.getAppliedAt()).isNull();
        verify(persistence, never()).save(any());
        verifyNoInteractions(accounts, departments, products);
    }

    @ParameterizedTest
    @EnumSource(value = ChangeType.class, names = {"CREATE", "UPDATE"})
    void rejectsInconsistentAccountCategoryAndTypeBeforeSavingRequest(ChangeType change) {
        String payload = "{\"name\":\"Mismatch\",\"category\":\"ASSETS\","
                + "\"accountType\":\"NON_OPERATING_INCOME\"}";
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, change);

        assertThatThrownBy(() -> service.requestChange(
                requestCommand(MasterDataType.ACCOUNT_SUBJECT, change, payload, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(persistence, never()).save(any());
        verifyClassificationLookup(change, 1);
        verifyNoInteractions(departments, products);
    }

    @ParameterizedTest
    @EnumSource(value = ChangeType.class, names = {"CREATE", "UPDATE"})
    void rejectsPreviouslyStoredInconsistentAccountPayloadBeforeApproval(ChangeType change) {
        String payload = "{\"name\":\"Mismatch\",\"category\":\"ASSETS\","
                + "\"accountType\":\"NON_OPERATING_INCOME\"}";
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, change);
        MasterDataChangeRequest request = pendingRequest(MasterDataType.ACCOUNT_SUBJECT,
                change, payload, null);
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(668L, "approver"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(request.getApprovedBy()).isNull();
        assertThat(request.getApprovedAt()).isNull();
        assertThat(request.getAppliedAt()).isNull();
        assertThat(request.getPayloadJson()).isEqualTo(payload);
        verify(persistence, never()).save(any());
        verifyClassificationLookup(change, 1);
        verifyNoInteractions(departments, products);
    }

    @Test
    void rejectsAccountCreationThatAttemptsToClearMappingBeforeSavingRequest() {
        String payload = "{\"name\":\"New account\",\"clearRegulatoryMappingCode\":true}";
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, ChangeType.CREATE);

        assertThatThrownBy(() -> service.requestChange(requestCommand(
                MasterDataType.ACCOUNT_SUBJECT, ChangeType.CREATE, payload, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(persistence, never()).save(any());
        verifyNoInteractions(accounts, departments, products);
    }

    @Test
    void rejectsStoredAccountCreationMappingClearBeforeApproval() {
        String payload = "{\"name\":\"New account\",\"clearRegulatoryMappingCode\":true}";
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, ChangeType.CREATE);
        MasterDataChangeRequest request = pendingRequest(MasterDataType.ACCOUNT_SUBJECT,
                ChangeType.CREATE, payload, null);
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(668L, "approver"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(request.getApprovedBy()).isNull();
        assertThat(request.getApprovedAt()).isNull();
        assertThat(request.getAppliedAt()).isNull();
        assertThat(request.getPayloadJson()).isEqualTo(payload);
        verify(persistence, never()).save(any());
        verifyNoInteractions(accounts, departments, products);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unresolvableAccountClassifications")
    void rejectsUnresolvableClassificationBeforeSavingRequest(ClassificationPayload input) {
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, input.change());

        assertThatThrownBy(() -> service.requestChange(requestCommand(
                MasterDataType.ACCOUNT_SUBJECT, input.change(), input.json(), null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(persistence, never()).save(any());
        verifyClassificationLookup(input.change(), 1);
        verifyNoInteractions(departments, products);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unresolvableAccountClassifications")
    void rejectsStoredUnresolvableClassificationBeforeApproval(ClassificationPayload input) {
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, input.change());
        MasterDataChangeRequest request = pendingRequest(MasterDataType.ACCOUNT_SUBJECT,
                input.change(), input.json(), null);
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(668L, "approver"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(request.getApprovedBy()).isNull();
        assertThat(request.getApprovedAt()).isNull();
        assertThat(request.getAppliedAt()).isNull();
        assertThat(request.getPayloadJson()).isEqualTo(input.json());
        verify(persistence, never()).save(any());
        verifyClassificationLookup(input.change(), 1);
        verifyNoInteractions(departments, products);
    }

    private static Stream<ClassificationPayload> unresolvableAccountClassifications() {
        return Stream.of(
                new ClassificationPayload(ChangeType.CREATE,
                        "{\"name\":\"New account\",\"accountType\":\"NON_OPERATING_INCOME\"}"),
                new ClassificationPayload(ChangeType.UPDATE,
                        "{\"name\":\"Type only\",\"accountType\":\"NON_OPERATING_INCOME\"}"),
                new ClassificationPayload(ChangeType.UPDATE,
                        "{\"name\":\"Category only\",\"category\":\"REVENUE\"}"));
    }

    private record ClassificationPayload(ChangeType change, String json) {
        @Override
        public String toString() {
            return change + " / " + json;
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("explicitNullClassificationPayloads")
    void rejectsExplicitNullClassificationAtRequest(String payload) {
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, ChangeType.UPDATE);

        assertThatThrownBy(() -> service.requestChange(requestCommand(
                MasterDataType.ACCOUNT_SUBJECT, ChangeType.UPDATE, payload, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(persistence, never()).save(any());
        verifyNoInteractions(departments, products);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("explicitNullClassificationPayloads")
    void rejectsStoredExplicitNullClassificationBeforeApproval(String payload) {
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, ChangeType.UPDATE);
        MasterDataChangeRequest request = pendingRequest(MasterDataType.ACCOUNT_SUBJECT,
                ChangeType.UPDATE, payload, null);
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(668L, "approver"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(request.getRequestedBy()).isEqualTo("requester");
        assertThat(request.getRequestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(request.getApprovedBy()).isNull();
        assertThat(request.getApprovedAt()).isNull();
        assertThat(request.getAppliedAt()).isNull();
        assertThat(request.getReason()).isEqualTo("payload validation");
        assertThat(request.getPayloadJson()).isEqualTo(payload);
        verify(persistence, never()).save(any());
        verifyNoInteractions(departments, products);
    }

    private static Stream<String> explicitNullClassificationPayloads() {
        return Stream.of(
                "{\"name\":\"Explicit null\",\"accountType\":null}",
                "{\"name\":\"Explicit null\",\"regulatoryMappingCode\":null}");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validPayloads")
    void acceptsThenApprovesValidPayloadWithoutBusinessCalls(ValidPayload input) {
        stubValidVersion(input.type(), input.change());
        when(persistence.findBySourceReference("first-valid-source")).thenReturn(Optional.empty());
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MasterDataChangeRequest requested = service.requestChange(requestCommand(
                input.type(), input.change(), input.json(), "first-valid-source"));
        assertThat(requested.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(requested.getApprovedBy()).isNull();
        assertThat(requested.getApprovedAt()).isNull();
        assertThat(requested.getSourceReference()).isEqualTo("first-valid-source");
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(requested));

        MasterDataChangeRequest approved = service.approve(668L, "approver");

        assertThat(approved).isSameAs(requested);
        assertThat(approved.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo("approver");
        assertThat(approved.getApprovedAt()).isNotNull();
        verifyOnlyValidationRead(input.type(), input.change(), 2);
        verifyNoInteractions(departments, products);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validPayloads")
    void appliesOriginalTypedCommandOnlyAfterApproval(ValidPayload input) {
        stubValidVersion(input.type(), input.change());
        MasterDataChangeRequest request = pendingRequest(input.type(), input.change(), input.json(), null);
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.approve(668L, "approver");
        verifyOnlyValidationRead(input.type(), input.change(), 1);
        verifyNoInteractions(departments, products);
        stubProductLookupForApply(input.type(), input.change());

        MasterDataChangeRequest applied = service.applyApprovedChange(668L);

        assertThat(applied.getStatus()).isEqualTo(ChangeStatus.APPLIED);
        assertThat(applied.getAppliedAt()).isNotNull();
        switch (input.type()) {
            case ACCOUNT_SUBJECT -> {
                AccountSubjectCommand expected = (AccountSubjectCommand) input.expectedCommand();
                if (input.change() == ChangeType.CREATE) {
                    verify(accounts).createAccountSubject(expected);
                } else {
                    verify(accounts).updateAccountSubject(TARGET_KEY, expected);
                }
            }
            case DEPARTMENT -> {
                DepartmentCommand expected = (DepartmentCommand) input.expectedCommand();
                if (input.change() == ChangeType.CREATE) {
                    verify(departments).createDepartment(expected);
                } else {
                    verify(departments).updateDepartment(TARGET_KEY, expected);
                }
            }
            case PRODUCT -> {
                ProductCommand expected = (ProductCommand) input.expectedCommand();
                if (input.change() == ChangeType.CREATE) {
                    verify(products).createProduct(expected);
                } else {
                    verify(products).getProductByProductCode(TARGET_KEY);
                    verify(products).updateProduct(900L, expected);
                }
            }
            default -> throw new IllegalArgumentException("Unexpected test type");
        }
        verifyNoMoreInteractions(accounts, departments, products);
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"NON_OPERATING_INCOME", "NON_OPERATING_EXPENSES"})
    void approvedNameOnlyAccountUpdateKeepsClassificationUnspecifiedAndPreservesAudit(AccountType originalType) {
        String payload = "{\"name\":\"Renamed\"}";
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, ChangeType.UPDATE);
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MasterDataChangeRequest request = pendingRequest(MasterDataType.ACCOUNT_SUBJECT,
                ChangeType.UPDATE, payload, "account-755-" + originalType);
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        service.approve(668L, "reviewer");
        verifyClassificationLookup(ChangeType.UPDATE, 1);
        MasterDataChangeRequest applied = service.applyApprovedChange(668L);

        ArgumentCaptor<AccountSubjectCommand> command = ArgumentCaptor.forClass(AccountSubjectCommand.class);
        verify(accounts).updateAccountSubject(org.mockito.ArgumentMatchers.eq(TARGET_KEY), command.capture());
        assertThat(command.getValue().accountType()).isNull();
        assertThat(command.getValue().regulatoryMappingCode()).isNull();
        assertThat(command.getValue().clearRegulatoryMappingCode()).isFalse();
        assertThat(command.getValue().unsettled()).isNull();
        assertThat(command.getValue().fixedAsset()).isNull();
        assertThat(applied.getRequestedBy()).isEqualTo("requester");
        assertThat(applied.getApprovedBy()).isEqualTo("reviewer");
        assertThat(applied.getReason()).isEqualTo("payload validation");
        assertThat(applied.getPayloadJson()).isEqualTo(payload);
        assertThat(applied.getAppliedAt()).isNotNull();
        assertThat(applied.getStatus()).isEqualTo(ChangeStatus.APPLIED);
    }

    @Test
    void approvedAccountUpdateCarriesExplicitClassificationAndMappingChanges() {
        String payload = "{\"name\":\"Reclassified\",\"category\":\"EXPENSES\","
                + "\"accountType\":\"NON_OPERATING_EXPENSES\","
                + "\"regulatoryMappingCode\":\"REG-NEW\",\"unsettled\":false,\"fixedAsset\":true}";
        AccountSubjectCommand command = applyApprovedAccountPayload(payload);
        assertThat(command.accountType()).isEqualTo(AccountType.NON_OPERATING_EXPENSES);
        assertThat(command.regulatoryMappingCode()).isEqualTo("REG-NEW");
        assertThat(command.clearRegulatoryMappingCode()).isFalse();
        assertThat(command.unsettled()).isFalse();
        assertThat(command.fixedAsset()).isTrue();
    }

    @Test
    void approvedAccountUpdateCarriesExplicitMappingClear() {
        AccountSubjectCommand command = applyApprovedAccountPayload(
                "{\"name\":\"Cleared\",\"clearRegulatoryMappingCode\":true}");
        assertThat(command.accountType()).isNull();
        assertThat(command.regulatoryMappingCode()).isNull();
        assertThat(command.clearRegulatoryMappingCode()).isTrue();
    }

    private AccountSubjectCommand applyApprovedAccountPayload(String payload) {
        stubValidVersion(MasterDataType.ACCOUNT_SUBJECT, ChangeType.UPDATE);
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MasterDataChangeRequest request = pendingRequest(MasterDataType.ACCOUNT_SUBJECT,
                ChangeType.UPDATE, payload, null);
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));
        service.approve(668L, "reviewer");
        service.applyApprovedChange(668L);
        ArgumentCaptor<AccountSubjectCommand> command = ArgumentCaptor.forClass(AccountSubjectCommand.class);
        verify(accounts).updateAccountSubject(org.mockito.ArgumentMatchers.eq(TARGET_KEY), command.capture());
        assertThat(request.getStatus()).isEqualTo(ChangeStatus.APPLIED);
        assertThat(request.getRequestedBy()).isEqualTo("requester");
        assertThat(request.getApprovedBy()).isEqualTo("reviewer");
        assertThat(request.getReason()).isEqualTo("payload validation");
        assertThat(request.getPayloadJson()).isEqualTo(payload);
        assertThat(request.getAppliedAt()).isNotNull();
        return command.getValue();
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "DEPARTMENT", "PRODUCT"})
    void deactivateNeedsNeitherPayloadDecodingNorBusinessCallsUntilApply(MasterDataType type) {
        MasterDataChangePayloadDecoder unusedDecoder = mock(MasterDataChangePayloadDecoder.class);
        MasterDataChangeApplier applier = switch (type) {
            case ACCOUNT_SUBJECT -> new AccountSubjectMasterDataChangeApplier(accounts, unusedDecoder);
            case DEPARTMENT -> new DepartmentMasterDataChangeApplier(departments, unusedDecoder);
            case PRODUCT -> new ProductMasterDataChangeApplier(products, unusedDecoder);
            default -> throw new IllegalArgumentException("Unexpected test type");
        };
        MasterDataChangeRequestService deactivateService =
                new MasterDataChangeRequestService(persistence, versions, List.of(applier));
        stubValidVersion(type, ChangeType.DEACTIVATE);
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MasterDataChangeRequest requested = deactivateService.requestChange(
                requestCommand(type, ChangeType.DEACTIVATE, null, null));
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(requested));
        deactivateService.approve(668L, "approver");
        assertThat(requested.getPayloadJson()).isNull();
        verifyNoInteractions(unusedDecoder, accounts, departments, products);

        stubProductLookupForApply(type, ChangeType.DEACTIVATE);
        deactivateService.applyApprovedChange(668L);

        switch (type) {
            case ACCOUNT_SUBJECT -> verify(accounts).deactivateAccountSubject(TARGET_KEY, EFFECTIVE_DATE);
            case DEPARTMENT -> verify(departments).deactivateDepartment(TARGET_KEY, EFFECTIVE_DATE);
            case PRODUCT -> {
                verify(products).getProductByProductCode(TARGET_KEY);
                verify(products).deactivateProduct(900L, EFFECTIVE_DATE);
            }
            default -> throw new IllegalArgumentException("Unexpected test type");
        }
        verifyNoInteractions(unusedDecoder);
        verifyNoMoreInteractions(accounts, departments, products);
        assertThat(requested.getStatus()).isEqualTo(ChangeStatus.APPLIED);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "DEPARTMENT", "PRODUCT"})
    void sameSourceReferenceReplayPreservesEvenPreviouslyApprovedMalformedRequest(MasterDataType type) {
        String validJson = type == MasterDataType.PRODUCT
                ? "{\"name\":\"valid\",\"productType\":\"SERVICE\"}" : "{\"name\":\"valid\"}";
        for (String json : List.of(validJson, "{")) {
            MasterDataChangeRequest existing = pendingRequest(type, ChangeType.CREATE, json, "replayed-source");
            existing.approve("old-approver");
            LocalDateTime originalApproval = existing.getApprovedAt();
            when(persistence.findBySourceReference("replayed-source")).thenReturn(Optional.of(existing));

            // 동일 명령 replay는 새 접수가 아니므로 과거 불량 row 재검증/복구로 의미를 바꾸지 않습니다.
            MasterDataChangeRequest replayed = service.requestChange(
                    requestCommand(type, ChangeType.CREATE, json, "replayed-source"));

            assertThat(replayed).isSameAs(existing);
            assertThat(replayed.getStatus()).isEqualTo(ChangeStatus.APPROVED);
            assertThat(replayed.getApprovedBy()).isEqualTo("old-approver");
            assertThat(replayed.getApprovedAt()).isEqualTo(originalApproval);
            assertThat(replayed.getRequestedAt()).isEqualTo(REQUESTED_AT);
            assertThat(replayed.getLockVersion()).isEqualTo(7L);
            assertThat(replayed.getPayloadJson()).isEqualTo(json);
        }
        verify(persistence, never()).save(any());
        verifyNoInteractions(versions, accounts, departments, products);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "DEPARTMENT", "PRODUCT"})
    void reusedSourceReferenceWithDifferentCommandStillConflicts(MasterDataType type) {
        MasterDataChangeRequest existing = pendingRequest(type, ChangeType.CREATE, "{", "replayed-source");
        when(persistence.findBySourceReference("replayed-source")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.requestChange(
                requestCommand(type, ChangeType.CREATE, "{}", "replayed-source")))
                .isInstanceOf(MasterDataIdempotencyConflictException.class);

        assertThat(existing.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        verify(persistence, never()).save(any());
        verifyNoInteractions(versions, accounts, departments, products);
    }

    private static Stream<ValidPayload> validPayloads() {
        List<ValidPayload> cases = new ArrayList<>();
        for (MasterDataType type : TARGET_TYPES) {
            for (ChangeType change : List.of(ChangeType.CREATE, ChangeType.UPDATE)) {
                String key = type == MasterDataType.PRODUCT ? "productCode" : "code";
                String name = change == ChangeType.UPDATE && type != MasterDataType.ACCOUNT_SUBJECT ? null : "valid";
                String fields = "\"name\":" + (name == null ? "null" : "\"valid\"");
                if (type == MasterDataType.PRODUCT) {
                    fields += ",\"productType\":" + (change == ChangeType.CREATE ? "\"SERVICE\"" : "null");
                }
                for (String keyJson : List.of("", ",\"" + key + "\":null", ",\"" + key + "\":\" \"",
                        ",\"" + key + "\":\"" + TARGET_KEY + "\"")) {
                    for (boolean sameDayEnd : List.of(false, true)) {
                        LocalDate validTo = sameDayEnd ? EFFECTIVE_DATE : null;
                        String dates = sameDayEnd ? ",\"validFrom\":\"2099-01-01\",\"validTo\":\"2026-08-01\"" : "";
                        cases.add(new ValidPayload(type, change, "key=" + keyJson + ", same-day=" + sameDayEnd,
                                "{" + fields + keyJson + dates + "}", minimalExpectedCommand(type, name, validTo)));
                    }
                }
            }
        }
        // 생략과 명시 null 모두 부분 수정 의미를 유지하며, 기본값을 applier가 미리 채우지 않습니다.
        cases.add(new ValidPayload(MasterDataType.DEPARTMENT, ChangeType.UPDATE, "all fields omitted", "{}",
                minimalExpectedCommand(MasterDataType.DEPARTMENT, null, null)));
        cases.add(new ValidPayload(MasterDataType.PRODUCT, ChangeType.UPDATE, "all fields omitted", "{}",
                minimalExpectedCommand(MasterDataType.PRODUCT, null, null)));
        for (ChangeType change : List.of(ChangeType.CREATE, ChangeType.UPDATE)) {
            cases.add(new ValidPayload(MasterDataType.ACCOUNT_SUBJECT, change, "all fields",
                    "{\"code\":\"MD-668\",\"name\":\"account\",\"parentCode\":\"PARENT\",\"category\":\"LIABILITIES\","
                            + "\"balanceType\":\"CREDIT\",\"reportLine\":\"REPORT\",\"unsettled\":true,\"fixedAsset\":true,"
                            + "\"validFrom\":\"2020-01-01\",\"validTo\":\"2027-12-31\"}",
                    new AccountSubjectCommand(TARGET_KEY, "account", "PARENT", AccountSubject.AccountCategory.LIABILITIES,
                            AccountSubject.BalanceType.CREDIT, "REPORT", true, true, EFFECTIVE_DATE, LocalDate.of(2027, 12, 31),
                            null, null, false)));
            cases.add(new ValidPayload(MasterDataType.DEPARTMENT, change, "all fields",
                    "{\"code\":\"MD-668\",\"name\":\"department\",\"parentCode\":\"PARENT\",\"type\":\"COST_CENTER\","
                            + "\"validFrom\":\"2020-01-01\",\"validTo\":\"2027-12-31\"}",
                    new DepartmentCommand(TARGET_KEY, "department", "PARENT", Department.DepartmentType.COST_CENTER,
                            EFFECTIVE_DATE, LocalDate.of(2027, 12, 31))));
            cases.add(new ValidPayload(MasterDataType.PRODUCT, change, "all fields, preserved negative precision",
                    "{\"productCode\":\"MD-668\",\"name\":\"product\",\"description\":\"description\",\"unitOfMeasure\":\"EA\","
                            + "\"price\":-12.3456,\"productType\":\"SERVICE\",\"validFrom\":\"2020-01-01\",\"validTo\":\"2027-12-31\"}",
                    new ProductCommand(TARGET_KEY, "product", "description", "EA", new BigDecimal("-12.3456"),
                            Product.ProductType.SERVICE, EFFECTIVE_DATE, LocalDate.of(2027, 12, 31))));
        }
        for (MasterDataType type : TARGET_TYPES) {
            String json = type == MasterDataType.PRODUCT
                    ? "{\"name\":\" \",\"productType\":\"SERVICE\"}" : "{\"name\":\" \"}";
            cases.add(new ValidPayload(type, ChangeType.CREATE, "no new blank-name policy", json,
                    minimalExpectedCommand(type, " ", null)));
        }
        return cases.stream();
    }

    private static Object minimalExpectedCommand(MasterDataType type, String name, LocalDate validTo) {
        return switch (type) {
            case ACCOUNT_SUBJECT -> new AccountSubjectCommand(
                    TARGET_KEY, name, null, null, null, null, null, null, EFFECTIVE_DATE, validTo,
                    null, null, false);
            case DEPARTMENT -> new DepartmentCommand(TARGET_KEY, name, null, null, EFFECTIVE_DATE, validTo);
            case PRODUCT -> new ProductCommand(TARGET_KEY, name, null, null, null,
                    name == null ? null : Product.ProductType.SERVICE, EFFECTIVE_DATE, validTo);
            default -> throw new IllegalArgumentException("Unexpected test type");
        };
    }

    private void stubProductLookupForApply(MasterDataType type, ChangeType change) {
        if (type == MasterDataType.PRODUCT && change != ChangeType.CREATE) {
            Product current = new Product();
            current.setId(900L);
            current.setProductCode(TARGET_KEY);
            when(products.getProductByProductCode(TARGET_KEY)).thenReturn(Optional.of(current));
        }
    }

    private record ValidPayload(MasterDataType type, ChangeType change, String boundary, String json, Object expectedCommand) {
        @Override
        public String toString() {
            return type + " " + change + " / " + boundary;
        }
    }

    private static Stream<InvalidPayload> invalidPayloads() {
        List<InvalidPayload> cases = new ArrayList<>();
        for (MasterDataType type : TARGET_TYPES) {
            for (ChangeType change : List.of(ChangeType.CREATE, ChangeType.UPDATE)) {
                String decodeError = "Invalid " + type + " change payload";
                String fields = type == MasterDataType.PRODUCT
                        ? "\"name\":\"valid\",\"productType\":\"SERVICE\"" : "\"name\":\"valid\"";
                String key = type == MasterDataType.PRODUCT ? "productCode" : "code";
                String enumField = switch (type) {
                    case ACCOUNT_SUBJECT -> "category";
                    case DEPARTMENT -> "type";
                    case PRODUCT -> "productType";
                    default -> throw new IllegalArgumentException("Unexpected test type");
                };
                cases.add(new InvalidPayload(type, change, "malformed JSON", "{", decodeError));
                cases.add(new InvalidPayload(type, change, "unknown enum",
                        "{\"name\":\"valid\",\"" + enumField + "\":\"NOT_AN_ENUM\"}", decodeError));
                // 객체/배열은 실제 Jackson 타입 오류이며 허용된 scalar coercion을 새로 금지하지 않습니다.
                cases.add(new InvalidPayload(type, change, "object instead of name", "{\"name\":{}}", decodeError));
                cases.add(new InvalidPayload(type, change, "array instead of command", "[]", decodeError));
                cases.add(new InvalidPayload(type, change, "literal null", "null", "payload must not be null"));
                cases.add(new InvalidPayload(type, change, "missing raw payload", null, "Payload is required"));
                cases.add(new InvalidPayload(type, change, "empty raw payload", "", "Payload is required"));
                cases.add(new InvalidPayload(type, change, "blank raw payload", " \t ", "Payload is required"));
                cases.add(new InvalidPayload(type, change, "different key",
                        "{" + fields + ",\"" + key + "\":\"OTHER\"}", "does not match"));
                cases.add(new InvalidPayload(type, change, "end before effective date",
                        "{" + fields + ",\"validFrom\":\"2026-07-01\",\"validTo\":\"2026-07-31\"}",
                        "SCD2 validTo cannot be before validFrom."));
                if (type == MasterDataType.ACCOUNT_SUBJECT || change == ChangeType.CREATE) {
                    String otherFields = type == MasterDataType.PRODUCT ? "\"productType\":\"SERVICE\"" : "";
                    cases.add(new InvalidPayload(type, change, "missing required name",
                            "{" + otherFields + "}", "name is required"));
                    cases.add(new InvalidPayload(type, change, "null required name",
                            "{\"name\":null" + (otherFields.isEmpty() ? "" : "," + otherFields) + "}",
                            "name is required"));
                }
                if (type == MasterDataType.PRODUCT && change == ChangeType.CREATE) {
                    cases.add(new InvalidPayload(type, change, "missing required productType",
                            "{\"name\":\"valid\"}", "productType is required"));
                    cases.add(new InvalidPayload(type, change, "null required productType",
                            "{\"name\":\"valid\",\"productType\":null}", "productType is required"));
                }
            }
        }
        return cases.stream();
    }

    private MasterDataChangeRequestCommand requestCommand(InvalidPayload input, String sourceReference) {
        return requestCommand(input.type(), input.change(), input.json(), sourceReference);
    }

    private MasterDataChangeRequestCommand requestCommand(
            MasterDataType type, ChangeType change, String payload, String sourceReference) {
        return new MasterDataChangeRequestCommand(type, TARGET_KEY, change, EFFECTIVE_DATE,
                requestedVersion(change), "requester", "payload validation", payload, sourceReference);
    }

    private MasterDataChangeRequest pendingRequest(
            MasterDataType type, ChangeType change, String payload, String sourceReference) {
        return MasterDataChangeRequest.reconstitute(668L, type, TARGET_KEY, change, ChangeStatus.REQUESTED,
                7L, EFFECTIVE_DATE, requestedVersion(change), "requester", null, REQUESTED_AT, null,
                "payload validation", payload, sourceReference, null);
    }

    private void stubValidVersion(MasterDataType type, ChangeType change) {
        // 잘못된 version stub 때문에 payload 검증 공백이 가려지지 않도록 정상 이력 수를 제공합니다.
        when(versions.countPersistedVersions(type, TARGET_KEY)).thenReturn(change == ChangeType.CREATE ? 0L : 1L);
        if (type == MasterDataType.ACCOUNT_SUBJECT && change == ChangeType.UPDATE) {
            AccountSubject current = new AccountSubject();
            current.setCode(TARGET_KEY);
            current.setCategory(AccountSubject.AccountCategory.LIABILITIES);
            current.setAccountType(AccountType.LIABILITIES);
            current.setBalanceType(AccountSubject.BalanceType.CREDIT);
            when(accounts.findAccountSubjectByCode(TARGET_KEY)).thenReturn(Optional.of(current));
        }
    }

    private void verifyClassificationLookup(ChangeType change, int expectedCount) {
        if (change == ChangeType.UPDATE) {
            verify(accounts, times(expectedCount)).findAccountSubjectByCode(TARGET_KEY);
            verifyNoMoreInteractions(accounts);
        } else {
            verifyNoInteractions(accounts);
        }
    }

    private void verifyOnlyValidationRead(MasterDataType type, ChangeType change, int expectedCount) {
        if (type == MasterDataType.ACCOUNT_SUBJECT) {
            verifyClassificationLookup(change, expectedCount);
        } else {
            verifyNoInteractions(accounts);
        }
    }

    private static int requestedVersion(ChangeType change) {
        return change == ChangeType.UPDATE ? 2 : 1;
    }

    private record InvalidPayload(MasterDataType type, ChangeType change, String boundary, String json, String message) {
        @Override
        public String toString() {
            return type + " " + change + " / " + boundary;
        }
    }

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
