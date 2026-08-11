package com.ho.account.masterdata.batch.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.batch.MasterDataBatchApplication;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.ProductPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실제 배치 실행 파일부터 H2 데이터베이스까지 연결되는 헥사고날 전체 경로를 검증합니다.
 *
 * <p>초보자 설명: 단위 테스트에서 Repository를 가짜 객체로 바꾸면 Job이 core pipeline을
 * 호출했다는 사실까지만 알 수 있고, 출력 포트가 실제 JPA adapter와 COUNT 쿼리에 올바르게
 * 연결됐는지는 알 수 없습니다. 이 테스트는 {@code Batch Job -> Tasklet -> Batch
 * Orchestrator -> Core Pipeline -> Statistics Port -> JPA Adapter -> Repository -> H2}
 * 순서로 실제 빈을 통과시킵니다. 기준일에 유효한 행과 이미 만료된 행을 종류별로 함께
 * 저장하므로, 단순히 전체 행 수를 세는 잘못된 구현도 잡아낼 수 있습니다. 또한 Job과 Step의
 * 완료 상태 및 execution context를 함께 확인해 운영 재시작 시 사용하는 기준일과 결과 요약이
 * Spring Batch 메타데이터에 남는 계약까지 한 번에 보호합니다.</p>
 */
@SpringBatchTest
@SpringBootTest(
        classes = MasterDataBatchApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.batch.job.enabled=false",
                "spring.cloud.vault.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "spring.config.on-not-found=ignore",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.flyway.enabled=false",
                "spring.batch.jdbc.initialize-schema=always",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class MasterDataValidityJobIntegrationTest {

    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 29);

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier(MasterDataValidityJobConfiguration.JOB_NAME)
    private Job masterDataValidityJob;

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Autowired
    private AccountSubjectPersistencePort accountSubjectPersistencePort;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentPersistencePort departmentPersistencePort;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPersistencePort productPersistencePort;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        businessPartnerRepository.deleteAll();
        productRepository.deleteAll();
        departmentRepository.deleteAll();
        accountSubjectRepository.deleteAll();
        jobLauncherTestUtils.setJob(masterDataValidityJob);
    }

    @Test
    void executesValidityJobThroughCorePortAndJpaAdapter() throws Exception {
        accountSubjectPersistencePort.save(accountSubject(
                "ACCOUNT-ACTIVE", AS_OF_DATE.minusDays(1), AS_OF_DATE));
        accountSubjectPersistencePort.save(accountSubject(
                "ACCOUNT-EXPIRED", AS_OF_DATE.minusDays(2), AS_OF_DATE.minusDays(1)));
        departmentPersistencePort.save(department(
                "DEPT-ACTIVE", AS_OF_DATE, AS_OF_DATE.plusDays(1)));
        departmentPersistencePort.save(department(
                "DEPT-EXPIRED", AS_OF_DATE.minusDays(2), AS_OF_DATE.minusDays(1)));
        productPersistencePort.save(product(
                "PRODUCT-ACTIVE", AS_OF_DATE.minusDays(1), AS_OF_DATE.plusDays(1)));
        productPersistencePort.save(product(
                "PRODUCT-EXPIRED", AS_OF_DATE.minusDays(2), AS_OF_DATE.minusDays(1)));
        // 거래처 aggregate는 순수 도메인 객체이므로 Spring Data repository에 직접
        // 전달하지 않습니다. 운영 코드와 같은 output port를 거쳐 JPA entity 매핑까지
        // 포함해야 이 통합 테스트가 헥사고날 경계를 실제로 검증합니다.
        businessPartnerPersistencePort.save(businessPartner(
                "PARTNER-ACTIVE", AS_OF_DATE.minusDays(1), AS_OF_DATE.plusDays(1)));
        businessPartnerPersistencePort.save(businessPartner(
                "PARTNER-EXPIRED", AS_OF_DATE.minusDays(2), AS_OF_DATE.minusDays(1)));


        JobExecution execution = jobLauncherTestUtils.launchJob(
                new JobParametersBuilder()
                        .addString(MasterDataValidityJobConfiguration.AS_OF_DATE_PARAMETER, AS_OF_DATE.toString())
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .containsExactly(MasterDataValidityJobConfiguration.STEP_NAME);

        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getExecutionContext()
                .getString(MasterDataValidityJobConfiguration.AS_OF_DATE_PARAMETER))
                .isEqualTo(AS_OF_DATE.toString());
        assertThat(stepExecution.getExecutionContext().getLong("activeAccountSubjects")).isEqualTo(1L);
        assertThat(stepExecution.getExecutionContext().getLong("activeDepartments")).isEqualTo(1L);
        assertThat(stepExecution.getExecutionContext().getLong("activeProducts")).isEqualTo(1L);
        assertThat(stepExecution.getExecutionContext().getLong("activeBusinessPartners")).isEqualTo(1L);
    }

    private AccountSubject accountSubject(String code, LocalDate validFrom, LocalDate validTo) {
        AccountSubject accountSubject = new AccountSubject();
        accountSubject.setCode(code);
        accountSubject.setName(code);
        accountSubject.setValidFrom(validFrom);
        accountSubject.setValidTo(validTo);
        return accountSubject;
    }

    private Department department(String code, LocalDate validFrom, LocalDate validTo) {
        Department department = new Department();
        department.setCode(code);
        department.setName(code);
        department.setValidFrom(validFrom);
        department.setValidTo(validTo);
        return department;
    }

    private Product product(String code, LocalDate validFrom, LocalDate validTo) {
        Product product = new Product();
        product.setProductCode(code);
        product.setName(code);
        product.setPrice(BigDecimal.ONE);
        product.setProductType(Product.ProductType.SERVICE);
        product.setValidFrom(validFrom);
        product.setValidTo(validTo);
        return product;
    }

    private BusinessPartner businessPartner(String code, LocalDate validFrom, LocalDate validTo) {
        BusinessPartner partner = new BusinessPartner();
        partner.setBusinessPartnerCode(code);
        partner.setBusinessPartnerName(code);
        partner.setUseYn(true);
        partner.setValidFrom(validFrom);
        partner.setValidTo(validTo);
        return partner;
    }
}
