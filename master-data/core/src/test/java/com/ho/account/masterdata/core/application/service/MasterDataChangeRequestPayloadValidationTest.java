package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
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
    private final BusinessPartnerUseCase partners = mock(BusinessPartnerUseCase.class);
    private final MasterDataBusinessKeyLockPort locks = mock(MasterDataBusinessKeyLockPort.class);
    private final AccountSubjectPersistencePort accountRows = mock(AccountSubjectPersistencePort.class);
    private final DepartmentPersistencePort departmentRows = mock(DepartmentPersistencePort.class);
    private final BusinessPartnerPersistencePort partnerRows = mock(BusinessPartnerPersistencePort.class);
    private final ProductPersistencePort productRows = mock(ProductPersistencePort.class);
    private MasterDataChangeRequestService service;

    @BeforeEach
    void setUpRealDecoderAppliersAndService() {
        JacksonMasterDataChangePayloadDecoder decoder = new JacksonMasterDataChangePayloadDecoder(
                new ObjectMapper().registerModule(new JavaTimeModule()));
        service = new MasterDataChangeRequestService(persistence, versions, List.of(
                new AccountSubjectMasterDataChangeApplier(accounts, decoder, accountRows),
                new BusinessPartnerMasterDataChangeApplier(partners, decoder, partnerRows),
                new DepartmentMasterDataChangeApplier(departments, decoder, departmentRows),
                new ProductMasterDataChangeApplier(products, decoder, productRows)), locks);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPayloads")
    void rejectsNewRequestBeforeSavingWithoutSourceReference(InvalidPayload input) {
        stubValidVersion(input.type(), input.change());

        assertThatThrownBy(() -> service.requestChange(requestCommand(input, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(input.message());

        verify(persistence, never()).save(any());
        verifyNoInteractions(accounts, departments, products, accountRows, departmentRows, partnerRows, productRows);
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
        verifyNoInteractions(accounts, departments, products, accountRows, departmentRows, partnerRows, productRows);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPayloads")
    void rejectsApprovalBeforeChangingStatusAuditOrLockVersion(InvalidPayload input) {
        stubValidVersion(input.type(), input.change());
        // 복원된 과거 REQUESTED 행은 생성자의 raw-payload 검사도 우회했을 수 있습니다.
        MasterDataChangeRequest request = pendingRequest(input.type(), input.change(), input.json(), null);
        when(persistence.findById(668L)).thenReturn(Optional.of(request));
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
        verifyNoInteractions(accounts, departments, products, accountRows, departmentRows, partnerRows, productRows);
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
        when(persistence.findById(668L)).thenReturn(Optional.of(requested));
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(requested));

        MasterDataChangeRequest approved = service.approve(668L, "approver");

        assertThat(approved).isSameAs(requested);
        assertThat(approved.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo("approver");
        assertThat(approved.getApprovedAt()).isNotNull();
        verifyNoInteractions(accounts, departments, products, accountRows, departmentRows, partnerRows, productRows);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validPayloads")
    void appliesOriginalTypedCommandOnlyAfterApproval(ValidPayload input) {
        stubValidVersion(input.type(), input.change());
        MasterDataChangeRequest request = pendingRequest(input.type(), input.change(), input.json(), null);
        when(persistence.findById(668L)).thenReturn(Optional.of(request));
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.approve(668L, "approver");
        verifyNoInteractions(accounts, departments, products, accountRows, departmentRows, partnerRows, productRows);
        stubCurrentLookupForApply(input.type(), input.change());

        MasterDataChangeRequest applied = service.applyApprovedChange(668L);

        assertThat(applied.getStatus()).isEqualTo(ChangeStatus.APPLIED);
        assertThat(applied.getAppliedAt()).isNotNull();
        switch (input.type()) {
            case ACCOUNT_SUBJECT -> {
                AccountSubjectCommand expected = (AccountSubjectCommand) input.expectedCommand();
                if (input.change() == ChangeType.CREATE) {
                    verify(accounts).createAccountSubject(expected);
                } else {
                    verify(accountRows).findByCodeForUpdate(TARGET_KEY);
                    verify(accounts).updateAccountSubject(TARGET_KEY, expected);
                }
            }
            case DEPARTMENT -> {
                DepartmentCommand expected = (DepartmentCommand) input.expectedCommand();
                if (input.change() == ChangeType.CREATE) {
                    verify(departments).createDepartment(expected);
                } else {
                    verify(departmentRows).findActiveByCodeForUpdate(TARGET_KEY);
                    verify(departments).updateDepartment(TARGET_KEY, expected);
                }
            }
            case PRODUCT -> {
                ProductCommand expected = (ProductCommand) input.expectedCommand();
                if (input.change() == ChangeType.CREATE) {
                    verify(products).createProduct(expected);
                } else {
                    verify(productRows).findActiveByProductCodeForUpdate(TARGET_KEY);
                    verify(products).updateProduct(900L, expected);
                }
            }
            default -> throw new IllegalArgumentException("Unexpected test type");
        }
        verifyNoMoreInteractions(accounts, departments, products, accountRows, departmentRows, partnerRows, productRows);
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class, names = {"ACCOUNT_SUBJECT", "DEPARTMENT", "PRODUCT"})
    void deactivateNeedsNeitherPayloadDecodingNorBusinessCallsUntilApply(MasterDataType type) {
        MasterDataChangePayloadDecoder unusedDecoder = mock(MasterDataChangePayloadDecoder.class);
        MasterDataChangeApplier applier = switch (type) {
            case ACCOUNT_SUBJECT -> new AccountSubjectMasterDataChangeApplier(accounts, unusedDecoder, accountRows);
            case DEPARTMENT -> new DepartmentMasterDataChangeApplier(departments, unusedDecoder, departmentRows);
            case PRODUCT -> new ProductMasterDataChangeApplier(products, unusedDecoder, productRows);
            default -> throw new IllegalArgumentException("Unexpected test type");
        };
        MasterDataChangeRequestService deactivateService =
                new MasterDataChangeRequestService(persistence, versions, List.of(applier), locks);
        stubValidVersion(type, ChangeType.DEACTIVATE);
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MasterDataChangeRequest requested = deactivateService.requestChange(
                requestCommand(type, ChangeType.DEACTIVATE, null, null));
        when(persistence.findById(668L)).thenReturn(Optional.of(requested));
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(requested));
        deactivateService.approve(668L, "approver");
        assertThat(requested.getPayloadJson()).isNull();
        verifyNoInteractions(unusedDecoder, accounts, departments, products);

        stubCurrentLookupForApply(type, ChangeType.DEACTIVATE);
        deactivateService.applyApprovedChange(668L);

        switch (type) {
            case ACCOUNT_SUBJECT -> {
                verify(accountRows).findByCodeForUpdate(TARGET_KEY);
                verify(accounts).deactivateAccountSubject(TARGET_KEY, EFFECTIVE_DATE);
            }
            case DEPARTMENT -> {
                verify(departmentRows).findActiveByCodeForUpdate(TARGET_KEY);
                verify(departments).deactivateDepartment(TARGET_KEY, EFFECTIVE_DATE);
            }
            case PRODUCT -> {
                verify(productRows).findActiveByProductCodeForUpdate(TARGET_KEY);
                verify(products).deactivateProduct(900L, EFFECTIVE_DATE);
            }
            default -> throw new IllegalArgumentException("Unexpected test type");
        }
        verifyNoInteractions(unusedDecoder);
        verifyNoMoreInteractions(accounts, departments, products, accountRows, departmentRows, partnerRows, productRows);
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
                            AccountSubject.BalanceType.CREDIT, "REPORT", true, true, EFFECTIVE_DATE, LocalDate.of(2027, 12, 31))));
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
                    TARGET_KEY, name, null, null, null, null, false, false, EFFECTIVE_DATE, validTo);
            case DEPARTMENT -> new DepartmentCommand(TARGET_KEY, name, null, null, EFFECTIVE_DATE, validTo);
            case PRODUCT -> new ProductCommand(TARGET_KEY, name, null, null, null,
                    name == null ? null : Product.ProductType.SERVICE, EFFECTIVE_DATE, validTo);
            default -> throw new IllegalArgumentException("Unexpected test type");
        };
    }

    private void stubCurrentLookupForApply(MasterDataType type, ChangeType change) {
        stubCurrentLookupForApply(type, change, LocalDate.of(9999, 12, 31));
    }

    private void stubCurrentLookupForApply(MasterDataType type, ChangeType change, LocalDate currentValidTo) {
        if (change != ChangeType.CREATE && type == MasterDataType.ACCOUNT_SUBJECT) {
            AccountSubject current = new AccountSubject();
            current.setValidFrom(EFFECTIVE_DATE.minusDays(1));
            current.setValidTo(currentValidTo);
            when(accountRows.findByCodeForUpdate(TARGET_KEY)).thenReturn(Optional.of(current));
        }
        if (change != ChangeType.CREATE && type == MasterDataType.DEPARTMENT) {
            Department current = new Department();
            current.setValidFrom(EFFECTIVE_DATE.minusDays(1));
            current.setValidTo(currentValidTo);
            when(departmentRows.findActiveByCodeForUpdate(TARGET_KEY)).thenReturn(Optional.of(current));
        }
        if (change != ChangeType.CREATE && type == MasterDataType.BUSINESS_PARTNER) {
            BusinessPartner current = BusinessPartner.create(TARGET_KEY, "valid", null, null, null, null,
                    null, true, null, null, EFFECTIVE_DATE.minusDays(1), currentValidTo);
            when(partnerRows.findByBusinessPartnerCodeForUpdate(TARGET_KEY)).thenReturn(Optional.of(current));
        }
        if (type == MasterDataType.PRODUCT && change != ChangeType.CREATE) {
            Product current = new Product();
            current.setId(900L);
            current.setProductCode(TARGET_KEY);
            current.setValidFrom(EFFECTIVE_DATE.minusDays(1));
            current.setValidTo(currentValidTo);
            when(productRows.findActiveByProductCodeForUpdate(TARGET_KEY)).thenReturn(Optional.of(current));
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
    void applyRechecksVersionAfterWaitingForBusinessKeyBeforeAnyMutation() {
        MasterDataChangeApplier applier = mock(MasterDataChangeApplier.class);
        when(applier.targetType()).thenReturn(MasterDataType.ACCOUNT_SUBJECT);
        MasterDataChangeRequestService applying = new MasterDataChangeRequestService(
                persistence, versions, List.of(applier), locks);
        MasterDataChangeRequest request = pendingRequest(MasterDataType.ACCOUNT_SUBJECT, ChangeType.UPDATE, "{}", null);
        request.approve("approver");
        when(persistence.findById(668L)).thenReturn(Optional.of(request));
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));
        when(versions.countPersistedVersions(MasterDataType.ACCOUNT_SUBJECT, TARGET_KEY)).thenReturn(1L);
        // 잠금을 기다리는 동안 승자가 저장한 새 이력을 노출해 잠금 전 검사 회귀를 잡습니다.
        doAnswer(invocation -> {
            when(versions.countPersistedVersions(MasterDataType.ACCOUNT_SUBJECT, TARGET_KEY)).thenReturn(2L);
            return null;
        }).when(locks).lock(MasterDataType.ACCOUNT_SUBJECT, TARGET_KEY);

        assertThatThrownBy(() -> applying.applyApprovedChange(668L))
                .isInstanceOf(MasterDataVersionConflictException.class);

        var order = inOrder(persistence, locks, versions);
        order.verify(persistence).findById(668L);
        order.verify(locks).lock(MasterDataType.ACCOUNT_SUBJECT, TARGET_KEY);
        order.verify(persistence).findByIdForUpdate(668L);
        order.verify(versions).countPersistedVersions(MasterDataType.ACCOUNT_SUBJECT, TARGET_KEY);
        verify(applier, never()).apply(any());
        verify(persistence, never()).save(any());
        assertThat(request.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(request.getAppliedAt()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class,
            names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void approvedUpdateConflictsWhenDeactivationShortenedCurrentWindowWithoutAddingHistory(MasterDataType type) {
        stubValidVersion(type, ChangeType.UPDATE);
        stubCurrentLookupForApply(type, ChangeType.UPDATE, EFFECTIVE_DATE);
        String nameField = type == MasterDataType.BUSINESS_PARTNER ? "businessPartnerName" : "name";
        for (String endField : List.of("", ",\"validTo\":\"9999-12-31\"")) {
            MasterDataChangeRequest request = pendingRequest(type, ChangeType.UPDATE,
                    "{\"" + nameField + "\":\"valid\"" + endField + "}", null);
            request.approve("approver");
            when(persistence.findById(668L)).thenReturn(Optional.of(request));
            when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.applyApprovedChange(668L))
                    .isInstanceOf(MasterDataVersionConflictException.class);

            assertThat(request.getStatus()).isEqualTo(ChangeStatus.APPROVED);
            assertThat(request.getAppliedAt()).isNull();
        }
        verify(persistence, never()).save(any());
        verify(accounts, never()).updateAccountSubject(any(), any());
        verify(departments, never()).updateDepartment(any(), any());
        verify(products, never()).updateProduct(any(), any());
        verify(partners, never()).updateBusinessPartner(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class,
            names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void malformedApprovedUpdateDatesRemainInvalidInputBeforeReadingCurrentWindow(MasterDataType type) {
        stubValidVersion(type, ChangeType.UPDATE);
        String nameField = type == MasterDataType.BUSINESS_PARTNER ? "businessPartnerName" : "name";
        MasterDataChangeRequest request = pendingRequest(type, ChangeType.UPDATE,
                "{\"" + nameField + "\":\"valid\",\"validTo\":\"2026-07-31\"}", null);
        request.approve("approver");
        when(persistence.findById(668L)).thenReturn(Optional.of(request));
        when(persistence.findByIdForUpdate(668L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.applyApprovedChange(668L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(request.getAppliedAt()).isNull();
        verifyNoInteractions(accounts, departments, products, partners, accountRows, departmentRows, partnerRows, productRows);
        verify(persistence, never()).save(any());
    }

    @Test
    void dueApplyLocksDistinctBusinessKeysInOrderBeforeRequestRows() {
        MasterDataChangeApplier applier = mock(MasterDataChangeApplier.class);
        when(applier.targetType()).thenReturn(MasterDataType.ACCOUNT_SUBJECT);
        MasterDataChangeRequestService applying = new MasterDataChangeRequestService(
                persistence, versions, List.of(applier), locks);
        MasterDataChangeRequest first = MasterDataChangeRequest.reconstitute(
                1L, MasterDataType.ACCOUNT_SUBJECT, "Z-KEY", ChangeType.CREATE, ChangeStatus.APPROVED,
                0L, EFFECTIVE_DATE, 1, "maker", "checker", REQUESTED_AT, REQUESTED_AT,
                null, "{}", null, null);
        MasterDataChangeRequest second = MasterDataChangeRequest.reconstitute(
                2L, MasterDataType.ACCOUNT_SUBJECT, "A-KEY", ChangeType.CREATE, ChangeStatus.APPROVED,
                0L, EFFECTIVE_DATE, 1, "maker", "checker", REQUESTED_AT, REQUESTED_AT,
                null, "{}", null, null);
        when(persistence.findReadyToApply(LocalDate.now(), 500)).thenReturn(List.of(first, second));
        when(persistence.findByIdForUpdate(1L)).thenReturn(Optional.of(first));
        when(persistence.findByIdForUpdate(2L)).thenReturn(Optional.of(second));
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(applying.applyDueApprovedChanges()).containsExactly(first, second);

        var order = inOrder(locks, persistence);
        order.verify(persistence).findReadyToApply(LocalDate.now(), 500);
        order.verify(locks).lock(MasterDataType.ACCOUNT_SUBJECT, "A-KEY");
        order.verify(locks).lock(MasterDataType.ACCOUNT_SUBJECT, "Z-KEY");
        order.verify(persistence).findByIdForUpdate(1L);
        order.verify(persistence).save(first);
        order.verify(persistence).findByIdForUpdate(2L);
        order.verify(persistence).save(second);
        verifyNoMoreInteractions(locks);
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
                        new ObjectMapper().registerModule(new JavaTimeModule())), partnerRows);
        MasterDataChangeRequestService service = new MasterDataChangeRequestService(
                persistencePort, versionQueryPort, List.of(applier), locks);
        MasterDataChangeRequest request = new MasterDataChangeRequest(
                MasterDataType.BUSINESS_PARTNER,
                "BP-BROKEN",
                ChangeType.CREATE,
                LocalDate.of(2026, 8, 1),
                1,
                "requester",
                "broken payload",
                "{\"businessPartnerCode\":");
        when(persistencePort.findById(42L)).thenReturn(Optional.of(request));
        when(persistencePort.findByIdForUpdate(42L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(42L, "approver"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid BUSINESS_PARTNER change payload");

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        verify(persistencePort, never()).save(request);
    }
}
