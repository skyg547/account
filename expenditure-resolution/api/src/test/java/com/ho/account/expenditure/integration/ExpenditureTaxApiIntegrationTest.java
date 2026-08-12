package com.ho.account.expenditure.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.resolution.api.adapter.in.web.APPaymentController;
import com.ho.account.expenditure.resolution.api.adapter.in.web.ExpenditureController;
import com.ho.account.expenditure.resolution.api.adapter.in.web.ExpenditureResolutionDtoAssembler;
import com.ho.account.expenditure.application.port.out.APPaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.application.service.APPaymentService;
import com.ho.account.expenditure.application.service.BudgetService;
import com.ho.account.expenditure.application.service.ExpenditureResolutionService;
import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.APPaymentStatus;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.ExpenditureResolutionStatus;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.tax.api.adapter.in.web.APInvoiceController;
import com.ho.account.tax.adapter.out.external.TaxInvoiceQueryAdapter;
import com.ho.account.tax.adapter.out.persistence.TaxInvoicePersistenceAdapter;
import com.ho.account.tax.application.service.TaxInvoiceService;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.repository.TaxInvoiceRepository;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ExpenditureTaxApiIntegrationTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:exptax;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class ExpenditureTaxApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ExpenditureResolutionPersistencePort resolutionPersistencePort;

    @MockBean
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;
    @MockBean
    private DepartmentPersistencePort departmentPersistencePort;
    @MockBean
    private AccountSubjectPersistencePort accountSubjectPersistencePort;
    @MockBean
    private JournalPostingPort journalPostingPort;
    @MockBean
    private AssetRegistrationPort assetRegistrationPort;
    @MockBean
    private BudgetService budgetService;
    @MockBean
    private MasterDataQueryPort masterDataQueryPort;

    @BeforeEach
    void setUpMasterDataPorts() {
        Department department = new Department();
        department.setCode("D001");
        department.setName("재무팀");

        AccountSubject paymentAccount = new AccountSubject();
        paymentAccount.setCode("PAY001");
        paymentAccount.setName("보통예금");

        AccountSubject expenseAccount = new AccountSubject();
        expenseAccount.setCode("EXP001");
        expenseAccount.setName("복리후생비");

        /*
         * 이 통합 테스트의 경계는 지출결의 API와 master-data outbound port 사이입니다.
         * master-data JPA 엔티티를 스캔하거나 Repository에 fixture를 저장하면 다른 모듈의
         * 테이블 매핑 변경만으로 이 테스트가 깨집니다. 따라서 순수 도메인 factory로 유효한
         * 거래처 fixture를 만들고 port mock이 반환하게 해 저장 기술과 업무 시나리오를 분리합니다.
         */
        BusinessPartner partner = BusinessPartner.create(
                "BP001",
                "테스트거래처",
                null,
                null,
                null,
                null,
                BusinessPartner.PartnerType.VENDOR,
                true,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW,
                LocalDate.of(2026, 1, 1),
                BusinessPartner.OPEN_ENDED_VALID_TO);

        when(departmentPersistencePort.findActiveByCode("D001")).thenReturn(Optional.of(department));
        when(accountSubjectPersistencePort.findByCode("PAY001")).thenReturn(Optional.of(paymentAccount));
        when(accountSubjectPersistencePort.findByCode("EXP001")).thenReturn(Optional.of(expenseAccount));
        when(businessPartnerPersistencePort.findByBusinessPartnerCode("BP001")).thenReturn(Optional.of(partner));
        when(masterDataQueryPort.findDepartment("D001"))
                .thenReturn(Optional.of(new DepartmentRef("D001", "재무팀", null)));
        when(masterDataQueryPort.findAccountSubject("PAY001"))
                .thenReturn(Optional.of(new AccountSubjectRef("PAY001", "보통예금", false, false)));
        when(masterDataQueryPort.findAccountSubject("EXP001"))
                .thenReturn(Optional.of(new AccountSubjectRef("EXP001", "복리후생비", false, false)));
        when(masterDataQueryPort.findBusinessPartner("BP001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("BP001", "테스트거래처", "VENDOR", true)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(100L, "SLIP-100", "DRAFT"));
    }

    @Test
    void purchaseInvoiceToExpenditureAndApPaymentFlow() throws Exception {
        String invoicePayload = """
                {
                  "issueId":"TX-20260429-001",
                  "type":"PURCHASE",
                  "issueDate":"2026-04-29",
                  "businessPartnerCode":"BP001",
                  "supplyAmount":1000.00,
                  "taxAmount":100.00,
                  "totalAmount":1100.00
                }
                """;

        String invoiceResponse = mockMvc.perform(post("/api/ap/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoicePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("PURCHASE")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long taxInvoiceId = objectMapper.readTree(invoiceResponse).get("id").asLong();

        String expenditurePayload = """
                {
                  "title":"통합 시나리오 지출결의",
                  "resolutionDate":"2026-04-29",
                  "paymentDate":"2026-04-30",
                  "departmentCode":"D001",
                  "paymentAccountCode":"PAY001",
                  "taxInvoiceId":%d,
                  "details":[
                    {
                      "accountSubjectCode":"EXP001",
                      "amount":1100.00,
                      "businessPartnerCode":"BP001",
                      "description":"통합 시나리오 테스트"
                    }
                  ]
                }
                """.formatted(taxInvoiceId);

        String expenditureResponse = mockMvc.perform(post("/api/expenditures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenditurePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxInvoiceId", is((int) taxInvoiceId)))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.details[0].accountSubjectCode", is("EXP001")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long expenditureId = objectMapper.readTree(expenditureResponse).get("id").asLong();
        ExpenditureResolution savedResolution = resolutionPersistencePort.findById(expenditureId).orElseThrow();
        assertEquals(ExpenditureResolutionStatus.DRAFT, savedResolution.getStatus());
        assertEquals(taxInvoiceId, savedResolution.getTaxInvoiceId());

        mockMvc.perform(post("/api/expenditures/{id}/request", expenditureId))
                .andExpect(status().isOk());
        ExpenditureResolution requestedResolution = resolutionPersistencePort.findById(expenditureId).orElseThrow();
        assertEquals(ExpenditureResolutionStatus.REQUESTED, requestedResolution.getStatus());

        mockMvc.perform(post("/api/expenditures/{id}/approve", expenditureId))
                .andExpect(status().isOk());
        ExpenditureResolution approvedResolution = resolutionPersistencePort.findById(expenditureId).orElseThrow();
        assertEquals(ExpenditureResolutionStatus.APPROVED, approvedResolution.getStatus());

        String apPaymentPayload = """
                {
                  "expenditureResolutionId":%d,
                  "taxInvoiceId":%d,
                  "paymentDate":"2026-04-30T10:00:00",
                  "amount":1100.00,
                  "paymentMethod":"TRANSFER"
                }
                """.formatted(expenditureId, taxInvoiceId);

        mockMvc.perform(post("/api/ap/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(apPaymentPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenditureResolutionId", is((int) expenditureId)))
                .andExpect(jsonPath("$.taxInvoiceId", is((int) taxInvoiceId)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.unappliedAmount", is(1100.00)));

        List<ExpenditureResolution> resolutions =
                resolutionPersistencePort.findByResolutionDate(LocalDate.of(2026, 4, 29));
        assertFalse(resolutions.isEmpty());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = TaxInvoiceRepository.class)
    @EntityScan(basePackageClasses = {TaxInvoice.class})
    @Import({
            APInvoiceController.class,
            TaxInvoiceService.class,
            TaxInvoicePersistenceAdapter.class,
            TaxInvoiceQueryAdapter.class,
            ExpenditureController.class,
            ExpenditureResolutionDtoAssembler.class,
            APPaymentController.class,
            ExpenditureResolutionService.class,
            APPaymentService.class,
            InMemoryPersistenceConfig.class
    })
    static class TestApplication {
    }

    static class InMemoryPersistenceConfig {

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
                private final AtomicLong sequence = new AtomicLong(0);
                private final Map<Long, APPayment> store = new ConcurrentHashMap<>();

                @Override
                public APPayment save(APPayment apPayment) {
                    if (apPayment.getId() == null) {
                        apPayment.setId(sequence.incrementAndGet());
                    }
                    if (apPayment.getStatus() == null) {
                        apPayment.setStatus(APPaymentStatus.PENDING);
                    }
                    store.put(apPayment.getId(), apPayment);
                    return apPayment;
                }

                @Override
                public Optional<APPayment> findById(Long id) {
                    return Optional.ofNullable(store.get(id));
                }

                @Override
                public List<APPayment> findByExpenditureResolutionId(Long expenditureResolutionId) {
                    List<APPayment> result = new ArrayList<>();
                    for (APPayment payment : store.values()) {
                        if (payment.getExpenditureResolution() != null
                                && payment.getExpenditureResolution().getId() != null
                                && payment.getExpenditureResolution().getId().equals(expenditureResolutionId)) {
                            result.add(payment);
                        }
                    }
                    return result;
                }

                @Override
                public void delete(APPayment apPayment) {
                    if (apPayment != null && apPayment.getId() != null) {
                        store.remove(apPayment.getId());
                    }
                }
            };
        }
    }
}
