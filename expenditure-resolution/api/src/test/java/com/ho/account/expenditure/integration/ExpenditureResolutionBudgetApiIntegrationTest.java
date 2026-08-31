package com.ho.account.expenditure.integration;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.expenditure.application.port.out.APPaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.application.service.APPaymentService;
import com.ho.account.expenditure.application.service.BudgetService;
import com.ho.account.expenditure.application.service.ExpenditureResolutionService;
import com.ho.account.expenditure.domain.Budget;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.ExpenditureResolutionStatus;
import com.ho.account.expenditure.resolution.api.adapter.in.web.APPaymentController;
import com.ho.account.expenditure.resolution.api.adapter.in.web.ExpenditureController;
import com.ho.account.expenditure.resolution.api.adapter.in.web.ExpenditureResolutionDtoAssembler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = ExpenditureResolutionBudgetApiIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:expbudget;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false",
                "spring.flyway.enabled=false",
                "spring.cloud.vault.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "management.health.vault.enabled=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.cloud.vault.config.VaultAutoConfiguration,"
                        + "org.springframework.cloud.vault.config.VaultReactiveAutoConfiguration,"
                        + "org.springframework.cloud.config.client.ConfigClientAutoConfiguration,"
                        + "org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration,"
                        + "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
        }
)
@AutoConfigureMockMvc
class ExpenditureResolutionBudgetApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private ExpenditureResolutionPersistencePort resolutionPersistencePort;

    @MockBean
    private MasterDataQueryPort masterDataQueryPort;
    @MockBean
    private JournalPostingPort journalPostingPort;
    @MockBean
    private AssetRegistrationPort assetRegistrationPort;
    @MockBean
    private TaxInvoiceQueryPort taxInvoiceQueryPort;

    @BeforeEach
    void setUp() {
        when(masterDataQueryPort.findDepartment("D001"))
                .thenReturn(Optional.of(new DepartmentRef("D001", "재무팀", "COST_CENTER")));
        when(masterDataQueryPort.findAccountSubject("PAY001"))
                .thenReturn(Optional.of(new AccountSubjectRef("PAY001", "보통예금", false, false)));
        when(masterDataQueryPort.findAccountSubject("EXP001"))
                .thenReturn(Optional.of(new AccountSubjectRef("EXP001", "복리후생비", false, false)));
        when(masterDataQueryPort.findBusinessPartner("BP001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("BP001", "테스트거래처", "VENDOR", true)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(100L, "SLIP-100", "DRAFT"));
        when(taxInvoiceQueryPort.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void createResolutionWithAllocatedBudgetDeductsBudgetCorrectly() throws Exception {
        // 1. 202604 D001 EXP001 예산 10,000 배정
        budgetService.assignBudget("202604", "D001", "EXP001", new BigDecimal("10000.00"));

        String payload = """
                {
                  "title":"예산 통제 테스트 결의서",
                  "resolutionDate":"2026-04-15",
                  "paymentDate":"2026-04-30",
                  "departmentCode":"D001",
                  "paymentAccountCode":"PAY001",
                  "details":[
                    {
                      "accountSubjectCode":"EXP001",
                      "amount":4000.00,
                      "businessPartnerCode":"BP001",
                      "description":"부서 회식비"
                    }
                  ]
                }
                """;

        String response = mockMvc.perform(post("/api/expenditures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.totalAmount", is(4000.00)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();
        ExpenditureResolution saved = resolutionPersistencePort.findById(id).orElseThrow();
        assertEquals(ExpenditureResolutionStatus.DRAFT, saved.getStatus());

        // 잔여 예산 6,000 확인
        assertEquals(0, new BigDecimal("6000.00").compareTo(
                budgetService.getRemainingBudget("202604", "D001", "EXP001")));
    }

    @Test
    void createResolutionWithoutPreconfiguredBudgetSucceedsViaFallback() throws Exception {
        String payload = """
                {
                  "title":"미지정 예산 결의서",
                  "resolutionDate":"2026-04-15",
                  "paymentDate":"2026-04-30",
                  "departmentCode":"D001",
                  "paymentAccountCode":"PAY001",
                  "details":[
                    {
                      "accountSubjectCode":"EXP001",
                      "amount":5000.00,
                      "businessPartnerCode":"BP001",
                      "description":"미지정 예산 테스트"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/expenditures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            ExpenditureController.class,
            ExpenditureResolutionDtoAssembler.class,
            APPaymentController.class,
            ExpenditureResolutionService.class,
            BudgetService.class,
            APPaymentService.class,
            TestPersistenceConfig.class
    })
    static class TestApplication {
    }

    static class TestPersistenceConfig {

        @Bean
        BudgetPersistencePort budgetPersistencePort() {
            return new BudgetPersistencePort() {
                private final Map<String, Budget> store = new ConcurrentHashMap<>();
                private final AtomicLong seq = new AtomicLong(1);

                @Override
                public Optional<Budget> findByYearMonthAndDepartmentCodeAndAccountCode(
                        String yearMonth, String departmentCode, String accountCode) {
                    return Optional.ofNullable(store.get(yearMonth + "-" + departmentCode + "-" + accountCode));
                }

                @Override
                public Budget save(Budget budget) {
                    if (budget.getId() == null) {
                        budget.setId(seq.getAndIncrement());
                    }
                    store.put(budget.getYearMonth() + "-" + budget.getDeptCode() + "-" + budget.getAccountCode(), budget);
                    return budget;
                }
            };
        }

        @Bean
        ExpenditureResolutionPersistencePort expenditureResolutionPersistencePort() {
            return new ExpenditureResolutionPersistencePort() {
                private final AtomicLong sequence = new AtomicLong(0);
                private final Map<Long, ExpenditureResolution> store = new ConcurrentHashMap<>();

                @Override
                public ExpenditureResolution save(ExpenditureResolution resolution) {
                    if (resolution.getId() == null) {
                        ReflectionTestUtils.setField(resolution, "id", sequence.incrementAndGet());
                    }
                    store.put(resolution.getId(), resolution);
                    return resolution;
                }

                @Override
                public Optional<ExpenditureResolution> findById(Long id) {
                    return Optional.ofNullable(store.get(id));
                }

                @Override
                public List<ExpenditureResolution> findByResolutionDate(LocalDate date) {
                    return store.values().stream()
                            .filter(r -> date.equals(r.getResolutionDate()))
                            .sorted(Comparator.comparing(ExpenditureResolution::getId))
                            .toList();
                }

                @Override
                public List<ExpenditureResolution> findByResolutionDateBetween(LocalDate startDate, LocalDate endDate) {
                    return store.values().stream()
                            .filter(r -> {
                                LocalDate d = r.getResolutionDate();
                                return (d.isEqual(startDate) || d.isAfter(startDate))
                                        && (d.isEqual(endDate) || d.isBefore(endDate));
                            })
                            .sorted(Comparator.comparing(ExpenditureResolution::getId))
                            .toList();
                }
            };
        }

        @Bean
        APPaymentPersistencePort apPaymentPersistencePort() {
            return new APPaymentPersistencePort() {
                @Override
                public com.ho.account.expenditure.domain.APPayment save(com.ho.account.expenditure.domain.APPayment apPayment) {
                    return apPayment;
                }

                @Override
                public Optional<com.ho.account.expenditure.domain.APPayment> findById(Long id) {
                    return Optional.empty();
                }

                @Override
                public List<com.ho.account.expenditure.domain.APPayment> findByExpenditureResolutionId(Long id) {
                    return List.of();
                }

                @Override
                public void delete(com.ho.account.expenditure.domain.APPayment apPayment) {}
            };
        }
    }
}
