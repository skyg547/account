package com.ho.account.closing.batch.config;

import com.ho.account.closing.application.port.out.EclProvisionSnapshotPort;
import com.ho.account.closing.infrastructure.persistence.JdbcEclProvisionSnapshotAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class EclProvisionSnapshotConfiguration {
    @Bean
    EclProvisionSnapshotPort eclProvisionSnapshotPort(
            JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        return new JdbcEclProvisionSnapshotAdapter(jdbcTemplate, transactionManager);
    }
}
