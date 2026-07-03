package com.ho.account.closing.batch;

import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.EclProvisionService;
import com.ho.account.closing.application.service.FxValuationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.ho.account.closing.batch",
    "com.ho.account.journalledger.application.service",
    "com.ho.account.journalledger.infrastructure",
    "com.ho.account.common.adapter",
    "com.ho.account.masterdata.core.infrastructure.adapter"
})
@EntityScan(basePackages = {
    "com.ho.account.closing.domain",
    "com.ho.account.journalledger.domain",
    "com.ho.account.masterdata.core.domain"
})
@EnableJpaRepositories(basePackages = {
    "com.ho.account.journalledger.domain",
    "com.ho.account.journalledger.adapter.out.persistence",
    "com.ho.account.masterdata.core.infrastructure.persistence.repository"
})
@EnableConfigurationProperties(ClosingAccountingProperties.class)
@Import({FxValuationService.class, EclProvisionService.class})
public class ClosingBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClosingBatchApplication.class, args);
    }
}