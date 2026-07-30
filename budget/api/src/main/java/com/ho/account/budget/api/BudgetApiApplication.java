package com.ho.account.budget.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Budget HTTP API composition root.
 *
 * <p>The executable lives in the API module, while the use cases and outbound
 * adapters remain in core. Explicit scans make that composition visible and
 * prevent a future package move from silently dropping persistence beans.</p>
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.budget")
@EntityScan(basePackages = "com.ho.account.budget.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "com.ho.account.budget.infrastructure.persistence")
public class BudgetApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetApiApplication.class, args);
    }
}
