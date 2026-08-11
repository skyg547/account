package com.ho.account.loan;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest(
        classes = LoanApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=loan-api",
                "spring.data.redis.repositories.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "management.tracing.enabled=false",
                "account.loan.accounting.cash-account-code=101000",
                "account.loan.accounting.loan-receivable-account-code=131000",
                "account.loan.accounting.deferred-asset-account-code=118000",
                "account.loan.accounting.recognized-income-account-code=410000",
                "account.loan.accounting.accrued-interest-receivable-account-code=115010",
                "account.loan.accounting.interest-income-account-code=410100"
        })
class LoanApplicationLocalProfileTest {

    @Autowired
    private Environment environment;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void localProfileStartsWithMasterDataPersistenceEntitiesManaged() {
        assertThat(environment.matchesProfiles("local")).isTrue();
        assertThat(entityManagerFactory.getMetamodel().getManagedTypes())
                .extracting(type -> type.getJavaType().getName())
                .contains(
                        "com.ho.account.masterdata.core.infrastructure.persistence.BusinessPartnerJpaEntity",
                        "com.ho.account.masterdata.core.infrastructure.persistence.BusinessPartnerAccountJpaEntity");
    }
}
