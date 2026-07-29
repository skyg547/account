package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes = JpaLoginAttemptAdapterTest.JpaTestConfiguration.class)
class JpaLoginAttemptAdapterTest {

    @Autowired
    private LoginAttemptJpaRepository repository;

    @Test
    void locksAfterConfiguredFailuresUsingSharedRepositoryAndClearsOnSuccess() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC);
        JpaLoginAttemptAdapter firstNode = new JpaLoginAttemptAdapter(repository, 2, 15, clock);
        JpaLoginAttemptAdapter secondNode = new JpaLoginAttemptAdapter(repository, 2, 15, clock);

        firstNode.recordFailure("ADMIN", "INVALID_PASSWORD");
        assertThat(secondNode.isLocked("admin")).isFalse();

        firstNode.recordFailure("admin", "INVALID_PASSWORD");
        assertThat(secondNode.isLocked("admin")).isTrue();

        secondNode.recordSuccess("admin");
        assertThat(firstNode.isLocked("admin")).isFalse();
        assertThat(repository.findById("admin")).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = LoginAttemptJpaRepository.class)
    @EntityScan(basePackageClasses = LoginAttemptJpaEntity.class)
    static class JpaTestConfiguration {
    }
}
