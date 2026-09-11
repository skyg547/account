package com.ho.account.deposit.batch;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ho.account.deposit")
@EntityScan(basePackages = "com.ho.account.deposit.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.deposit.infrastructure.adapter.out.persistence")
public class DepositBatchApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(DepositBatchApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        // A named CLI job finishes synchronously; release scheduler/connection threads and
        // propagate JobExecutionExitCodeGenerator's failure status to the caller.
        if (context.getEnvironment().getProperty("spring.batch.job.enabled", Boolean.class, true)
                && !context.getEnvironment().getProperty("spring.batch.job.name", "").isBlank()) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
