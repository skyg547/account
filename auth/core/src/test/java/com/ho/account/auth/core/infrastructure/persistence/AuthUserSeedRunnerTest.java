package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

class AuthUserSeedRunnerTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    @DisplayName("목록 전체를 먼저 검증하므로 뒤쪽 null 항목에서 repository를 전혀 호출하지 않는다")
    void validatesWholeListBeforeRepositoryInteraction() {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.setUsers(Arrays.asList(validUser(randomUsername()), null));
        AuthUserJpaRepository repository = mock(AuthUserJpaRepository.class);
        AuthUserSeedRunner runner = new AuthUserSeedRunner(properties, repository);

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.users[1]");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("기존 username이라도 잘못된 credential을 조용히 건너뛰지 않고 repository 조회 전에 거부한다")
    void rejectsInvalidExistingUserInputBeforeExistsCheck() {
        AuthModuleProperties properties = new AuthModuleProperties();
        String username = randomUsername();
        String raw = randomValue();
        AuthModuleProperties.User invalid = validUser(username);
        invalid.setPassword(raw);
        properties.setUsers(List.of(invalid));
        AuthUserJpaRepository repository = mock(AuthUserJpaRepository.class);
        AuthUserSeedRunner runner = new AuthUserSeedRunner(properties, repository);

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.users[0].password")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain(username, raw));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("실제 transaction proxy에서 늦은 DB 실패는 전부 rollback되고 수정 재시도와 멱등 재시도가 성공한다")
    void rollsBackLatePersistenceFailureThenSupportsCorrectedAndIdempotentRetry() {
        contextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            AuthUserSeedRunner runner = context.getBean(AuthUserSeedRunner.class);
            AuthModuleProperties properties = context.getBean(AuthModuleProperties.class);
            AuthUserJpaRepository repository = context.getBean(AuthUserJpaRepository.class);
            assertThat(AopUtils.isAopProxy(runner)).isTrue();

            String firstUsername = randomUsername();
            AuthModuleProperties.User tooLongForDatabase = validUser("u".repeat(81));
            properties.setUsers(List.of(validUser(firstUsername), tooLongForDatabase));

            assertThatThrownBy(() -> runner.run(null)).isInstanceOf(RuntimeException.class);
            assertThat(repository.count()).isZero();

            String secondUsername = randomUsername();
            properties.setUsers(List.of(validUser(firstUsername), validUser(secondUsername)));
            runner.run(null);
            assertThat(repository.count()).isEqualTo(2L);
            assertThat(repository.existsById(firstUsername)).isTrue();
            assertThat(repository.existsById(secondUsername)).isTrue();

            runner.run(null);
            assertThat(repository.count()).isEqualTo(2L);
        });
    }

    private static ApplicationContextRunner contextRunner() {
        String databaseName = "auth_seed_" + UUID.randomUUID();
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        HibernateJpaAutoConfiguration.class,
                        TransactionAutoConfiguration.class))
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.jpa.hibernate.ddl-auto=create-drop",
                        "spring.jpa.show-sql=false")
                .withUserConfiguration(SeedTestConfiguration.class);
    }

    private static AuthModuleProperties.User validUser(String username) {
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername(username);
        user.setPassword(PasswordEncoderPolicy.encode(randomValue(), 4));
        user.setDepartmentCode("T-" + randomValue().substring(0, 8));
        user.setRoles(List.of("ROLE_TEST_" + randomValue().substring(0, 8)));
        return user;
    }

    private static String randomUsername() {
        return "configured-" + randomValue().substring(0, 12);
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackageClasses = AuthUserJpaRepository.class)
    @EntityScan(basePackageClasses = AuthUserJpaEntity.class)
    static class SeedTestConfiguration {

        @Bean
        AuthModuleProperties authModuleProperties() {
            AuthModuleProperties properties = new AuthModuleProperties();
            properties.getJwt().setSecret(randomValue());
            properties.getInternalApi().setToken(randomValue());
            return properties;
        }

        @Bean
        AuthUserSeedRunner authUserSeedRunner(
                AuthModuleProperties properties,
                AuthUserJpaRepository repository) {
            return new AuthUserSeedRunner(properties, repository);
        }
    }
}
