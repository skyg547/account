package com.ho.account.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.domain.model.Product.ProductType;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.ProductEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real PostgreSQL gates use synthetic, isolated schemas and never modify existing tables. */
@EnabledIfEnvironmentVariable(named = "MASTER_DATA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
@SpringBootTest(classes = MasterDataProductPricePostgresqlTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
                "spring.config.name=product-price-postgresql-test",
                "spring.profiles.active=local",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.clean-disabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        })
class MasterDataProductPricePostgresqlTest {

    private static final String SCHEMA = "product_price_test_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired private ProductRepository products;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MASTER_DATA_TEST_POSTGRES_URL"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
    }

    @ParameterizedTest
    @MethodSource("validPrices")
    void jpaSaveAndReloadPreservesValidatedNumericValue(BigDecimal price) {
        assertThat(jdbc.queryForObject("SELECT version()", String.class)).startsWith("PostgreSQL");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM flyway_schema_history WHERE version = '10' AND success
                """, Integer.class)).isEqualTo(1);

        BigDecimal validated = Product.requireValidPrice(price);
        ProductEntity entity = product("JPA-" + UUID.randomUUID(), validated);
        Long id = new TransactionTemplate(transactionManager).execute(status -> {
            Long savedId = products.saveAndFlush(entity).getId();
            // Force a database read instead of accepting the first-level cached entity.
            entityManager.clear();
            BigDecimal reloaded = products.findById(savedId).orElseThrow().getPrice();
            assertThat(reloaded).isEqualTo(validated);
            return savedId;
        });
        BigDecimal reloaded = products.findById(id).orElseThrow().getPrice();
        BigDecimal stored = jdbc.queryForObject("SELECT price FROM products WHERE id = ?", BigDecimal.class, id);

        // PostgreSQL NUMERIC(19,4) materializes four fractional places, including harmless zeros.
        assertThat(reloaded).isEqualTo(validated);
        assertThat(stored).isEqualTo(validated);
    }

    @Test
    void databaseConstraintRejectsNegativePrice() {
        String key = "NEG-" + UUID.randomUUID();
        Throwable failure = catchThrowable(() -> insertProduct(jdbc, key, new BigDecimal("-0.0001")));

        assertThat(sqlState(failure)).isEqualTo("23514");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM products WHERE product_code = ?",
                Integer.class, key)).isZero();
    }

    @Test
    void legacyNegativePreflightPreservesProductAndAuditOnFailureThenAllowsApprovedRepair() {
        String schema = "product_price_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        migrate(schema, "9");
        JdbcTemplate legacy = jdbcFor(schema);
        String key = "LEGACY-" + UUID.randomUUID();
        insertProduct(legacy, key, new BigDecimal("-1.0000"));
        legacy.update("""
                INSERT INTO master_data_change_requests
                    (target_type, target_key, change_type, status, effective_date,
                     requested_version, requested_by, requested_at, payload_json)
                VALUES ('PRODUCT', ?, 'CREATE', 'APPLIED', CURRENT_DATE,
                        1, 'synthetic-test', CURRENT_TIMESTAMP, '{}')
                """, key);
        List<Map<String, Object>> productsBefore = legacy.queryForList("SELECT * FROM products ORDER BY id");
        List<Map<String, Object>> auditBefore = legacy.queryForList(
                "SELECT * FROM master_data_change_requests ORDER BY id");

        assertThatThrownBy(() -> migrate(schema, "10")).isInstanceOf(FlywayException.class);
        assertThat(legacy.queryForList("SELECT * FROM products ORDER BY id")).isEqualTo(productsBefore);
        assertThat(legacy.queryForList("SELECT * FROM master_data_change_requests ORDER BY id"))
                .isEqualTo(auditBefore);
        assertThat(legacy.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version = '10'",
                Integer.class)).isZero();
        assertThat(legacy.queryForObject("""
                SELECT count(*) FROM pg_constraint WHERE conname = 'ck_products_price_nonnegative'
                  AND conrelid = 'products'::regclass
                """, Integer.class)).isZero();

        // The test models a separately approved repair; V10 never rewrites legacy prices itself.
        legacy.update("UPDATE products SET price = 0 WHERE product_code = ?", key);
        migrate(schema, "10");
        assertThat(legacy.queryForList("SELECT * FROM master_data_change_requests ORDER BY id"))
                .isEqualTo(auditBefore);
        assertThat(sqlState(catchThrowable(() -> insertProduct(legacy,
                "AFTER-" + UUID.randomUUID(), new BigDecimal("-0.0001"))))).isEqualTo("23514");
    }

    private static Stream<BigDecimal> validPrices() {
        return Stream.of(new BigDecimal("0"), new BigDecimal("1.2345"),
                new BigDecimal("1.230000"), new BigDecimal("999999999999999.9999"));
    }

    private static ProductEntity product(String key, BigDecimal price) {
        ProductEntity entity = new ProductEntity();
        entity.setProductCode(key);
        entity.setName("Synthetic product");
        entity.setProductType(ProductType.PHYSICAL);
        entity.setPrice(price);
        entity.setValidFrom(LocalDate.of(2025, 1, 1));
        entity.setValidTo(LocalDate.of(9999, 12, 31));
        return entity;
    }

    private static void insertProduct(JdbcTemplate target, String key, BigDecimal price) {
        target.update("""
                INSERT INTO products
                    (product_code, name, price, product_type, valid_from, valid_to, created_at, updated_at)
                VALUES (?, 'Synthetic product', ?, 'PHYSICAL', CURRENT_DATE, DATE '9999-12-31',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, key, price);
    }

    private static JdbcTemplate jdbcFor(String schema) {
        String url = System.getenv("MASTER_DATA_TEST_POSTGRES_URL");
        return new JdbcTemplate(new DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                "postgres", ""));
    }

    private static void migrate(String schema, String target) {
        Flyway.configure().dataSource(System.getenv("MASTER_DATA_TEST_POSTGRES_URL"), "postgres", "")
                .schemas(schema).defaultSchema(schema).locations("classpath:db/migration")
                .target(target).cleanDisabled(true).load().migrate();
    }

    private static String sqlState(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) return sqlException.getSQLState();
        }
        return null;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = {
            "com.ho.account.masterdata.core.domain.model",
            "com.ho.account.masterdata.core.domain.changerequest",
            "com.ho.account.masterdata.core.infrastructure.persistence"
    })
    @EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
    static class TestApplication {
    }
}
