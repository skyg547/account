package com.ho.account.loan.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Preserves the embedded provider composition outside the isolated dev runtime. */
@Configuration(proxyBeanMethods = false)
@Profile("!dev")
@ComponentScan(
        basePackages = {
                "com.ho.account.journalledger",
                "com.ho.account.common",
                "com.ho.account.shared",
                "com.ho.account.masterdata.core"
        },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.ho\\.account\\.shared\\.infrastructure\\.security\\.web\\..*"
                )
        }
)
@EntityScan(basePackages = {
        "com.ho.account.journalledger.domain",
        "com.ho.account.masterdata.core.domain",
        "com.ho.account.masterdata.core.infrastructure.persistence",
        "com.ho.account.masterdata.core.infrastructure.persistence.entity",
        "com.ho.account.shared.infrastructure.security.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence",
        "com.ho.account.masterdata.core.infrastructure.persistence",
        "com.ho.account.shared.infrastructure.security.repository"
})
public class LoanMonolithConfiguration {
}
