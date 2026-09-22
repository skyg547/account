package com.ho.account.masterdata.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MasterDataDirectWriteAuthorizationTest {

    private static final String ACCOUNT_SUBJECT_BODY = """
            {
              "code": "1100",
              "name": "Cash",
              "category": "ASSETS",
              "balanceType": "DEBIT",
              "reportLine": "CURRENT_ASSETS",
              "unsettled": false,
              "fixedAsset": false,
              "validFrom": "2026-01-01",
              "validTo": "9999-12-31"
            }
            """;
    private static final String BUSINESS_PARTNER_BODY = """
            {
              "businessPartnerCode": "BP-001",
              "businessPartnerName": "Partner One",
              "registrationNumber": "123-45-67890",
              "ceoName": "Owner",
              "businessType": "Wholesale",
              "businessItem": "Parts",
              "partnerType": "VENDOR",
              "useYn": true,
              "kycStatus": "APPROVED",
              "riskRating": "LOW",
              "validFrom": "2026-01-01",
              "validTo": "9999-12-31"
            }
            """;
    private static final String PRODUCT_BODY = """
            {
              "productCode": "PRD-001",
              "name": "Product One",
              "description": "Authorization fixture",
              "unitOfMeasure": "EA",
              "price": 1000.00,
              "productType": "PHYSICAL",
              "validFrom": "2026-01-01",
              "validTo": "9999-12-31"
            }
            """;
    private static final String DEPARTMENT_BODY = """
            {
              "code": "D001",
              "name": "Finance",
              "type": "COST_CENTER",
              "validFrom": "2026-01-01",
              "validTo": "9999-12-31"
            }
            """;

    private AccountSubjectUseCase accountSubjectUseCase;
    private BusinessPartnerUseCase businessPartnerUseCase;
    private ProductUseCase productUseCase;
    private DepartmentUseCase departmentUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountSubjectUseCase = mock(AccountSubjectUseCase.class);
        businessPartnerUseCase = mock(BusinessPartnerUseCase.class);
        productUseCase = mock(ProductUseCase.class);
        departmentUseCase = mock(DepartmentUseCase.class);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AccountSubjectController(accountSubjectUseCase),
                        new BusinessPartnerController(businessPartnerUseCase),
                        new ProductController(productUseCase),
                        new DepartmentController(departmentUseCase))
                .setControllerAdvice(new MasterDataDirectWritePolicy(), new MasterDataExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @ParameterizedTest(name = "{0}: missing roles header is forbidden")
    @MethodSource("mutations")
    void directMutationWithoutRolesHeaderIsForbiddenAndDoesNotCallUseCase(Mutation mutation) throws Exception {
        mockMvc.perform(requestFor(mutation, null))
                .andExpect(status().isForbidden());

        verifyNoUseCaseInteractions();
    }

    @ParameterizedTest(name = "{0}: blank roles header is forbidden")
    @MethodSource("mutations")
    void directMutationWithBlankRolesHeaderIsForbiddenAndDoesNotCallUseCase(Mutation mutation) throws Exception {
        mockMvc.perform(requestFor(mutation, "   "))
                .andExpect(status().isForbidden());

        verifyNoUseCaseInteractions();
    }

    @ParameterizedTest(name = "{0}: disallowed role is forbidden")
    @MethodSource("mutations")
    void directMutationWithDisallowedRoleIsForbiddenAndDoesNotCallUseCase(Mutation mutation) throws Exception {
        mockMvc.perform(requestFor(mutation, mutation.disallowedRole()))
                .andExpect(status().isForbidden());

        verifyNoUseCaseInteractions();
    }

    @ParameterizedTest(name = "{0}: allowed admin role invokes the mutation")
    @MethodSource("mutations")
    void directMutationWithAllowedAdminRoleSucceedsAndCallsExpectedUseCase(Mutation mutation) throws Exception {
        stubSuccessfulMutation(mutation);

        mockMvc.perform(requestFor(mutation, mutation.allowedRole()))
                .andExpect(status().is(mutation.successStatus()));

        verifyExpectedMutation(mutation);
    }

    @Test
    void getRemainsAvailableWithoutRolesHeader() throws Exception {
        when(accountSubjectUseCase.findAllActiveAccountSubjects()).thenReturn(List.of());

        mockMvc.perform(get("/api/basic/account-subjects"))
                .andExpect(status().isOk());

        verify(accountSubjectUseCase).findAllActiveAccountSubjects();
        verifyNoMoreInteractions(accountSubjectUseCase);
        verifyNoInteractions(businessPartnerUseCase, productUseCase, departmentUseCase);
    }

    private static Stream<Arguments> mutations() {
        // One explicit endpoint inventory drives every authorization branch so the four matrices cannot drift.
        return Stream.of(
                Arguments.of(new Mutation("account-subject POST", Target.ACCOUNT_SUBJECT_CREATE,
                        HttpMethod.POST, "/api/basic/account-subjects", ACCOUNT_SUBJECT_BODY,
                        "ROLE_READER", "ADMIN", 200)),
                Arguments.of(new Mutation("account-subject PUT", Target.ACCOUNT_SUBJECT_UPDATE,
                        HttpMethod.PUT, "/api/basic/account-subjects/1100", ACCOUNT_SUBJECT_BODY,
                        "ROLE_USER", "ROLE_SYSTEM_ADMIN", 200)),
                Arguments.of(new Mutation("account-subject DELETE", Target.ACCOUNT_SUBJECT_DELETE,
                        HttpMethod.DELETE, "/api/basic/account-subjects/1100", null,
                        "ROLE_READER", " ROLE_READER, MASTER_MANAGER ", 204)),
                Arguments.of(new Mutation("business-partner POST", Target.BUSINESS_PARTNER_CREATE,
                        HttpMethod.POST, "/api/basic/businesspartners", BUSINESS_PARTNER_BODY,
                        "ROLE_USER", "ROLE_ACCOUNTING_ADMIN", 200)),
                Arguments.of(new Mutation("business-partner PUT", Target.BUSINESS_PARTNER_UPDATE,
                        HttpMethod.PUT, "/api/basic/businesspartners/42", BUSINESS_PARTNER_BODY,
                        "ROLE_READER", "PARTNER_MANAGER", 200)),
                Arguments.of(new Mutation("business-partner DELETE", Target.BUSINESS_PARTNER_DELETE,
                        HttpMethod.DELETE, "/api/basic/businesspartners/42", null,
                        "ROLE_USER", "ROLE_ADMIN", 204)),
                Arguments.of(new Mutation("product POST", Target.PRODUCT_CREATE,
                        HttpMethod.POST, "/api/basic/products", PRODUCT_BODY,
                        "ROLE_READER", "SYSTEM_ADMIN", 200)),
                Arguments.of(new Mutation("product PUT", Target.PRODUCT_UPDATE,
                        HttpMethod.PUT, "/api/basic/products/7", PRODUCT_BODY,
                        "ROLE_USER", "ROLE_MASTER_MANAGER", 200)),
                Arguments.of(new Mutation("product DELETE", Target.PRODUCT_DELETE,
                        HttpMethod.DELETE, "/api/basic/products/7", null,
                        "ROLE_READER", "ACCOUNTING_ADMIN", 204)),
                Arguments.of(new Mutation("department POST", Target.DEPARTMENT_CREATE,
                        HttpMethod.POST, "/api/basic/departments", DEPARTMENT_BODY,
                        "ROLE_USER", " ROLE_USER, ROLE_PARTNER_MANAGER ", 200)));
    }

    private MockHttpServletRequestBuilder requestFor(Mutation mutation, String roles) {
        MockHttpServletRequestBuilder request = request(mutation.method(), mutation.path());
        if (mutation.body() != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(mutation.body());
        }
        if (roles != null) {
            request.header("X-Auth-Roles", roles);
        }
        return request;
    }

    private void stubSuccessfulMutation(Mutation mutation) {
        switch (mutation.target()) {
            case ACCOUNT_SUBJECT_CREATE -> when(accountSubjectUseCase.createAccountSubject(any(AccountSubjectCommand.class)))
                    .thenReturn(mock(AccountSubject.class));
            case ACCOUNT_SUBJECT_UPDATE -> when(accountSubjectUseCase.updateAccountSubject(
                    eq("1100"), any(AccountSubjectCommand.class))).thenReturn(mock(AccountSubject.class));
            case BUSINESS_PARTNER_CREATE -> when(businessPartnerUseCase.createBusinessPartner(
                    any(BusinessPartnerCommand.class))).thenReturn(mock(BusinessPartner.class));
            case BUSINESS_PARTNER_UPDATE -> when(businessPartnerUseCase.updateBusinessPartner(
                    eq(42L), any(BusinessPartnerCommand.class))).thenReturn(mock(BusinessPartner.class));
            case PRODUCT_CREATE -> when(productUseCase.createProduct(any(ProductCommand.class)))
                    .thenReturn(mock(Product.class));
            case PRODUCT_UPDATE -> when(productUseCase.updateProduct(eq(7L), any(ProductCommand.class)))
                    .thenReturn(mock(Product.class));
            case DEPARTMENT_CREATE -> when(departmentUseCase.createDepartment(any(DepartmentCommand.class)))
                    .thenReturn(mock(Department.class));
            case ACCOUNT_SUBJECT_DELETE, BUSINESS_PARTNER_DELETE, PRODUCT_DELETE -> {
                // Void mutations need no stubbing; their successful return proves the call completed.
            }
        }
    }

    private void verifyExpectedMutation(Mutation mutation) {
        switch (mutation.target()) {
            case ACCOUNT_SUBJECT_CREATE -> verify(accountSubjectUseCase)
                    .createAccountSubject(any(AccountSubjectCommand.class));
            case ACCOUNT_SUBJECT_UPDATE -> verify(accountSubjectUseCase)
                    .updateAccountSubject(eq("1100"), any(AccountSubjectCommand.class));
            case ACCOUNT_SUBJECT_DELETE -> verify(accountSubjectUseCase).deactivateAccountSubject("1100");
            case BUSINESS_PARTNER_CREATE -> verify(businessPartnerUseCase)
                    .createBusinessPartner(any(BusinessPartnerCommand.class));
            case BUSINESS_PARTNER_UPDATE -> verify(businessPartnerUseCase)
                    .updateBusinessPartner(eq(42L), any(BusinessPartnerCommand.class));
            case BUSINESS_PARTNER_DELETE -> verify(businessPartnerUseCase).deleteBusinessPartner(42L);
            case PRODUCT_CREATE -> verify(productUseCase).createProduct(any(ProductCommand.class));
            case PRODUCT_UPDATE -> verify(productUseCase).updateProduct(eq(7L), any(ProductCommand.class));
            case PRODUCT_DELETE -> verify(productUseCase).deactivateProduct(7L);
            case DEPARTMENT_CREATE -> verify(departmentUseCase).createDepartment(any(DepartmentCommand.class));
        }

        verifyOnlyTargetUseCaseWasCalled(mutation.target());
    }

    private void verifyOnlyTargetUseCaseWasCalled(Target target) {
        switch (target) {
            case ACCOUNT_SUBJECT_CREATE, ACCOUNT_SUBJECT_UPDATE, ACCOUNT_SUBJECT_DELETE -> {
                verifyNoMoreInteractions(accountSubjectUseCase);
                verifyNoInteractions(businessPartnerUseCase, productUseCase, departmentUseCase);
            }
            case BUSINESS_PARTNER_CREATE, BUSINESS_PARTNER_UPDATE, BUSINESS_PARTNER_DELETE -> {
                verifyNoMoreInteractions(businessPartnerUseCase);
                verifyNoInteractions(accountSubjectUseCase, productUseCase, departmentUseCase);
            }
            case PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE -> {
                verifyNoMoreInteractions(productUseCase);
                verifyNoInteractions(accountSubjectUseCase, businessPartnerUseCase, departmentUseCase);
            }
            case DEPARTMENT_CREATE -> {
                verifyNoMoreInteractions(departmentUseCase);
                verifyNoInteractions(accountSubjectUseCase, businessPartnerUseCase, productUseCase);
            }
        }
    }

    private void verifyNoUseCaseInteractions() {
        verifyNoInteractions(accountSubjectUseCase, businessPartnerUseCase, productUseCase, departmentUseCase);
    }

    private enum Target {
        ACCOUNT_SUBJECT_CREATE,
        ACCOUNT_SUBJECT_UPDATE,
        ACCOUNT_SUBJECT_DELETE,
        BUSINESS_PARTNER_CREATE,
        BUSINESS_PARTNER_UPDATE,
        BUSINESS_PARTNER_DELETE,
        PRODUCT_CREATE,
        PRODUCT_UPDATE,
        PRODUCT_DELETE,
        DEPARTMENT_CREATE
    }

    private record Mutation(
            String name,
            Target target,
            HttpMethod method,
            String path,
            String body,
            String disallowedRole,
            String allowedRole,
            int successStatus) {

        @Override
        public String toString() {
            return name;
        }
    }
}
