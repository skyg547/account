package com.ho.account.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        classes = MasterDataPostgresqlSchemaContextTest.SchemaValidationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.name=schema-validation",
                "spring.profiles.active=local",
                "spring.datasource.url=jdbc:h2:mem:master-api-pg-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.target=8",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.clean-disabled=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        })
class MasterDataPostgresqlSchemaContextTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Test
    void cleanBaselineMigratesValidatesAndSupportsCoreRepositories() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version IN ('1', '2', '3', '4', '5', '6') AND success = TRUE
                """, Integer.class)).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'account_subjects', 'business_partners', 'business_partner_accounts',
                    'currencies', 'departments', 'exchange_rates', 'fiscal_periods',
                    'master_data_change_requests', 'products', 'tax_profiles')
                """, Integer.class)).isEqualTo(10);
        assertThat(accountSubjectRepository.count()).isZero();
        assertThat(businessPartnerRepository.count()).isZero();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = {
            "com.ho.account.masterdata.core.domain.model",
            "com.ho.account.masterdata.core.domain.changerequest",
            "com.ho.account.masterdata.core.infrastructure.persistence"
    })
    @EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
    static class SchemaValidationApplication {
    }
}
