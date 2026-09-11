package com.ho.account.closing.infrastructure.source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Explicit opt-in: ordinary Closing API startup never opens foreign database connections. */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
@ConditionalOnProperty(name = "closing.sources.enabled", havingValue = "true")
public class ClosingReadOnlySourceConfiguration {
    @Bean(destroyMethod = "close")
    public ClosingReadOnlySources closingReadOnlySources(
            @Value("${closing.sources.journal.url}") String journalUrl,
            @Value("${closing.sources.journal.username}") String journalUsername,
            @Value("${closing.sources.journal.password}") String journalPassword,
            @Value("${closing.sources.ecl.url}") String eclUrl,
            @Value("${closing.sources.ecl.username}") String eclUsername,
            @Value("${closing.sources.ecl.password}") String eclPassword,
            @Value("${closing.sources.master-data.url}") String masterUrl,
            @Value("${closing.sources.master-data.username}") String masterUsername,
            @Value("${closing.sources.master-data.password}") String masterPassword) {
        return new ClosingReadOnlySources(journalUrl, journalUsername, journalPassword,
                eclUrl, eclUsername, eclPassword, masterUrl, masterUsername, masterPassword);
    }
}
