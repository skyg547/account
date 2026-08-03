package com.ho.account.ecl.api;

import com.ho.account.ecl.batch.AllowanceEclBatchApplication;
import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [Service] 대손충당금(IFRS9) API 서비스 애플리케이션 진입점.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.ho.account.ecl.api",
                "com.ho.account.ecl.core",
                "com.ho.account.ecl.batch"
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AllowanceEclBatchApplication.class))
@EntityScan(basePackages = {
        "com.ho.account.ecl.core.domain",
        "com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa"
})
@EnableJpaRepositories(basePackages =
        "com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa")
@Import(ProductionPostgresqlTlsGuard.class)
public class AllowanceEclApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllowanceEclApiApplication.class, args);
    }
}
