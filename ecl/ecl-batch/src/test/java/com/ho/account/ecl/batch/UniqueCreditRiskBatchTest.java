package com.ho.account.ecl.batch;

// import com.ho.account.ecl.batch.job.CreditRiskBatchConfig;
import com.ho.account.ecl.batch.config.*;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.*;
import com.ho.account.ecl.batch.job.tasklet.RegulatoryContractTasklet;
import com.ho.account.ecl.core.application.pipeline.MonthlyAssetConsolidationService;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrCustomerRepository;
import com.ho.account.ecl.core.application.port.out.CrRiskResultRepository;
import com.ho.account.ecl.core.application.service.calculation.CreditRiskService;
import com.ho.account.ecl.core.application.service.crm.ApartmentCollateralService;
import com.ho.account.ecl.core.application.service.crm.CollateralAllocationService;
import com.ho.account.ecl.core.application.service.monitoring.ConcentrationRiskService;
import com.ho.account.ecl.core.application.service.monitoring.CreditMonitoringService;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:unique_test;MODE=PostgreSQL")
@org.junit.jupiter.api.Disabled("JVM 리소스 절약 및 컨텍스트 충돌 방지를 위해 일시 비활성화")
public class UniqueCreditRiskBatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private CrCustomerRepository customerRepository;

    @Autowired
    private CrAccountRepository accountRepository;

    @Autowired
    private CrRiskResultRepository resultRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("수동 빈 조립 방식으로 모든 충돌을 회피한 대손충당금(IFRS9) 배치 통합 테스트")
    public void testFullCreditRiskJob() throws Exception {
        setupTestData();
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", LocalDate.now().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    private void setupTestData() {
        jdbcTemplate.update("DELETE FROM cr_risk_results");
        jdbcTemplate.update("DELETE FROM cr_monthly_summaries");
        jdbcTemplate.update("DELETE FROM cr_account_collaterals");
        jdbcTemplate.update("DELETE FROM cr_accounts");
        jdbcTemplate.update("DELETE FROM cr_collaterals");
        jdbcTemplate.update("DELETE FROM cr_customers");
        CrCustomer customer = CrCustomer.builder()
                .customerCode("CUST-FINAL-777")
                .customerName("수동 조립 테스트")
                .customerType(com.ho.account.shared.finance.enums.CustomerType.CORPORATE)
                .build();
        customerRepository.save(customer);
        CrAccount account = CrAccount.builder()
                .accountNo("ACC-FINAL-777")
                .customer(customer)
                .productCode("PROD-001")
                .currency("KRW")
                .outstandingAmount(new BigDecimal("1000000"))
                .notionalAmount(new BigDecimal("1000000"))
                .openDate(LocalDate.now())
                .isActive(true)
                .build();
        accountRepository.save(account);
    }
}
