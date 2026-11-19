package com.ho.account.auth.core.infrastructure.config;

import com.ho.account.shared.infrastructure.security.infrastructure.persistence.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Only the four governance entities/repositories/adapters join Auth's existing persistence unit. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
@EntityScan(basePackages = {"com.ho.account.auth.core.infrastructure.persistence", "com.ho.account.auth.core.infrastructure.security", "com.ho.account.shared.infrastructure.security.domain"})
@EnableJpaRepositories(basePackages = {"com.ho.account.auth.core.infrastructure.persistence", "com.ho.account.auth.core.infrastructure.security", "com.ho.account.shared.infrastructure.security.repository"})
@Import({MasterApprovalJpaAdapter.class, SystemRoleJpaAdapter.class, AuthorizationJpaAdapter.class, AuditLogJpaAdapter.class})
public class GovernancePersistenceConfiguration {}
