package com.ho.account.closing.batch;

import com.ho.account.closing.application.pipeline.FxValuationPipeline;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.EclProvisionService;
import com.ho.account.closing.application.service.FxValuationService;
import com.ho.account.closing.infrastructure.local.ClosingLocalExternalPortConfiguration;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithExchangeRateQueryAdapter;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithFiscalPeriodControlAdapter;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithMasterDataQueryAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaAccountSubjectPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaBusinessPartnerPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaDepartmentPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.AccountSubjectMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.DepartmentMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
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
    "com.ho.account.common.adapter"
})
@EntityScan(basePackages = {
    "com.ho.account.closing.domain",
    "com.ho.account.journalledger.domain",
    "com.ho.account.masterdata.core.domain",
    "com.ho.account.masterdata.core.infrastructure.persistence"
})
@EnableJpaRepositories(basePackages = {
    "com.ho.account.journalledger.domain",
    "com.ho.account.journalledger.adapter.out.persistence",
    "com.ho.account.masterdata.core.infrastructure.persistence.repository"
})
@EnableConfigurationProperties(ClosingAccountingProperties.class)
@Import({
    FxValuationService.class,
    FxValuationPipeline.class,
    EclProvisionService.class,
    ClosingLocalExternalPortConfiguration.class,
    MonolithExchangeRateQueryAdapter.class,
    MonolithFiscalPeriodControlAdapter.class,
    MonolithMasterDataQueryAdapter.class,
    JpaFiscalPeriodPersistenceAdapter.class,
    FiscalPeriodMapper.class,
    JpaAccountSubjectPersistenceAdapter.class,
    AccountSubjectMapper.class,
    JpaBusinessPartnerPersistenceAdapter.class,
    JpaDepartmentPersistenceAdapter.class,
    DepartmentMapper.class
})
public class ClosingBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClosingBatchApplication.class, args);
    }
}
