package com.ho.account.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Non-web Loan job entry point. Dev scans only Loan-owned persistence and HTTP ports;
 * the !dev configuration retains the embedded providers. A named CLI job runs synchronously,
 * then closes its context and returns the Spring Batch execution exit code.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.loan")
@EntityScan(basePackages = "com.ho.account.loan.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.loan.infrastructure.persistence")
public class LoanBatchApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LoanBatchApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        ConfigurableApplicationContext context = application.run(args);

        if (context.getEnvironment().getProperty("spring.batch.job.enabled", Boolean.class, true)
                && !context.getEnvironment().getProperty("spring.batch.job.name", "").isBlank()) {
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
        }
    }

}
