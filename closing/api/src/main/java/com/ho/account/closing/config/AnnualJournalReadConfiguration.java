package com.ho.account.closing.config;

import com.ho.account.closing.application.port.out.AnnualJournalReadPort;
import com.ho.account.closing.infrastructure.source.JdbcAnnualJournalReadAdapter;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** The embedded non-dev composition reads Journal from the same primary PostgreSQL database. */
@Configuration(proxyBeanMethods = false)
@Profile("!dev & !local")
public class AnnualJournalReadConfiguration {
    @Bean
    AnnualJournalReadPort embeddedAnnualJournalReadPort(DataSource dataSource) {
        return new JdbcAnnualJournalReadAdapter(dataSource);
    }
}
