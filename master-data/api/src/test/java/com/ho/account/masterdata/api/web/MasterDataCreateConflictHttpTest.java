package com.ho.account.masterdata.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** The four direct write controllers share the same duplicate-key conflict response. */
class MasterDataCreateConflictHttpTest {

    private AccountSubjectUseCase accounts;
    private BusinessPartnerUseCase partners;
    private DepartmentUseCase departments;
    private ProductUseCase products;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountSubjectUseCase.class);
        partners = mock(BusinessPartnerUseCase.class);
        departments = mock(DepartmentUseCase.class);
        products = mock(ProductUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AccountSubjectController(accounts),
                        new BusinessPartnerController(partners),
                        new DepartmentController(departments),
                        new ProductController(products))
                .setControllerAdvice(new MasterDataDirectWritePolicy(), new MasterDataExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper().registerModule(new JavaTimeModule())))
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = MasterDataType.class,
            names = {"ACCOUNT_SUBJECT", "BUSINESS_PARTNER", "DEPARTMENT", "PRODUCT"})
    void duplicateDirectCreateReturnsConflict(MasterDataType type) throws Exception {
        MasterDataVersionConflictException conflict =
                new MasterDataVersionConflictException("이미 이력이 존재하는 업무 코드입니다");
        String path;
        String body;
        switch (type) {
            case ACCOUNT_SUBJECT -> {
                when(accounts.createAccountSubject(any(AccountSubjectCommand.class))).thenThrow(conflict);
                path = "/api/basic/account-subjects";
                body = """
                        {"code":"A-754","name":"Test Account","category":"ASSETS",
                         "balanceType":"DEBIT","validFrom":"2026-11-01","validTo":"2026-11-30"}
                        """;
            }
            case BUSINESS_PARTNER -> {
                when(partners.createBusinessPartner(any(BusinessPartnerCommand.class))).thenThrow(conflict);
                path = "/api/basic/businesspartners";
                body = """
                        {"businessPartnerCode":"BP-754","businessPartnerName":"Test Partner",
                         "partnerType":"VENDOR","validFrom":"2026-11-01","validTo":"2026-11-30"}
                        """;
            }
            case DEPARTMENT -> {
                when(departments.createDepartment(any(DepartmentCommand.class))).thenThrow(conflict);
                path = "/api/basic/departments";
                body = """
                        {"code":"D-754","name":"Test Department","type":"COST_CENTER",
                         "validFrom":"2026-11-01","validTo":"2026-11-30"}
                        """;
            }
            case PRODUCT -> {
                when(products.createProduct(any(ProductCommand.class))).thenThrow(conflict);
                path = "/api/basic/products";
                body = """
                        {"productCode":"P-754","name":"Test Product","price":1,
                         "productType":"SERVICE","validFrom":"2026-11-01","validTo":"2026-11-30"}
                        """;
            }
            default -> throw new AssertionError("Unsupported type: " + type);
        }

        mockMvc.perform(post(path)
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(conflict.getMessage()));
    }
}
