package com.ho.account.asset.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=" + AssetLeaseApiPostgresqlSchemaContextTest.DATABASE_URL,
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.profiles.active=dev",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.kafka.listener.auto-startup=false"
        })
@ContextConfiguration(initializers = AssetLeaseApiPostgresqlSchemaContextTest.SchemaInitializer.class)
class AssetLeaseApiPostgresqlSchemaContextTest {

    static final String DATABASE_URL = "jdbc:h2:mem:asset-lease-api-pg-schema"
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    @Test
    void startsWithJpaValidateAgainstPostgresqlBaseline(ConfigurableApplicationContext context) {
        assertThat(context.isActive()).isTrue();
    }

    static class SchemaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            Flyway.configure()
                    .dataSource(DATABASE_URL, "sa", "")
                    .locations("classpath:db/postgresql-migration")
                    .load()
                    .migrate();
        }
    }
}
