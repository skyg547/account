package com.ho.account.mart.api;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [IFRS 9 Allowance Mart API Service]
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.mart")
@EntityScan(basePackages = {
        "com.ho.account.mart.core.domain",
        "com.ho.account.mart.core.infrastructure.persistence.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.mart.core.infrastructure.persistence.jpa"
})
@Import(ProductionPostgresqlTlsGuard.class)
public class AllowanceMartApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllowanceMartApiApplication.class, args);
    }
}
