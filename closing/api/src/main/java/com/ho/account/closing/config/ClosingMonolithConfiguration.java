package com.ho.account.closing.config;

import com.ho.account.masterdata.core.infrastructure.adapter.MonolithFiscalPeriodControlAdapter;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithExchangeRateQueryAdapter;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithMasterDataQueryAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaAccountSubjectPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaDepartmentPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaBusinessPartnerPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.AccountSubjectMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.DepartmentMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Preserves the existing embedded composition outside the isolated dev runtime. */
@Configuration(proxyBeanMethods = false)
@Profile("!dev")
@ComponentScan(basePackages = {
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
@Import({
        MonolithFiscalPeriodControlAdapter.class,
        MonolithExchangeRateQueryAdapter.class,
        MonolithMasterDataQueryAdapter.class,
        JpaFiscalPeriodPersistenceAdapter.class,
        FiscalPeriodMapper.class,
        JpaAccountSubjectPersistenceAdapter.class,
        AccountSubjectMapper.class,
        JpaBusinessPartnerPersistenceAdapter.class,
        JpaDepartmentPersistenceAdapter.class,
        DepartmentMapper.class
})
public class ClosingMonolithConfiguration {
}
