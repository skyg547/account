package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort.RoleAssignmentReplacement;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes = JpaAuthUserPersistenceAdapterTest.JpaTestConfiguration.class)
class JpaAuthUserPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");
    private final String dynamicPassword = PasswordEncoderPolicy.encode(randomSecret(), 4);

    @Autowired
    private JpaAuthUserQueryAdapter adapter;

    @Autowired
    private JpaAuthUserRoleAssignmentAdapter roleAssignmentAdapter;

    @Autowired
    private AuthUserJpaRepository userRepository;

    @Autowired
    private RoleAssignmentApplyLogJpaRepository applyLogRepository;

    @Test
    void findByUsernameLoadsApprovedEffectiveRoleAssignments() {
        userRepository.save(userWithRoles());
        userRepository.flush();

        AuthUser user = adapter.findByUsername(" teller ")
                .orElseThrow();

        assertThat(user.getUsername()).isEqualTo("teller");
        assertThat(user.getStoredPassword()).isEqualTo(dynamicPassword);
        assertThat(user.getDepartmentCode()).isEqualTo("BR001");
        assertThat(user.getRoleVersion()).isEqualTo(3L);
        assertThat(user.getRoleAssignments()).hasSize(3);
        assertThat(user.effectiveRolesAt(NOW)).containsExactly("ROLE_TELLER");
    }

    @Test
    void findAllUsersEagerlyLoadsRolesInUsernameOrder() {
        AuthUserJpaEntity teller = userWithRoles();
        AuthUserJpaEntity admin = new AuthUserJpaEntity(
                "admin",
                dynamicPassword,
                null,
                true,
                false,
                1L);
        admin.addRoleAssignment(RoleAssignmentJpaEntity.from(RoleAssignment.approved("ROLE_ADMIN")));
        userRepository.saveAll(List.of(teller, admin));
        userRepository.flush();

        List<AuthUser> users = adapter.findAllUsers();

        assertThat(users).extracting(AuthUser::getUsername)
                .containsExactly("admin", "teller");
        assertThat(users.get(0).effectiveRolesAt(NOW)).containsExactly("ROLE_ADMIN");
        assertThat(users.get(1).getRoleAssignments()).hasSize(3);
    }

    @Test
    void seedRunnerPersistsConfiguredUsersOnlyWhenMissing() throws Exception {
        AuthModuleProperties properties = new AuthModuleProperties();
        AuthModuleProperties.User configured = new AuthModuleProperties.User();
        configured.setUsername("ops");
        configured.setPassword(dynamicPassword);
        configured.setDepartmentCode("OPS");
        configured.setRoles(List.of("ROLE_OPS", "ROLE_AUDITOR"));
        properties.setUsers(List.of(configured));

        AuthUserSeedRunner seedRunner = new AuthUserSeedRunner(properties, userRepository);
        seedRunner.run(null);
        seedRunner.run(null);

        AuthUser user = adapter.findByUsername("ops")
                .orElseThrow();

        assertThat(userRepository.count()).isEqualTo(1L);
        assertThat(user.getStoredPassword()).isEqualTo(dynamicPassword);
        assertThat(user.effectiveRolesAt(NOW)).containsExactly("ROLE_OPS", "ROLE_AUDITOR");
        assertThat(user.getRoleVersion()).isEqualTo(1L);
    }

    @Test
    void replaceRoleAssignmentsPreservesMetadataAndIncrementsRoleVersion() {
        userRepository.save(userWithRoles());
        userRepository.flush();
        RoleAssignment assignment = new RoleAssignment(
                "ROLE_AUDITOR",
                "BR001",
                NOW.minusSeconds(1),
                NOW.plusSeconds(60),
                true);

        AuthUser updated = roleAssignmentAdapter.replaceRoleAssignments(replacement(
                "governance-approval-id=1",
                "a".repeat(64),
                List.of(assignment)));

        assertThat(updated.getRoleVersion()).isEqualTo(4L);
        assertThat(updated.getRoleAssignments()).containsExactly(assignment);
        assertThat(updated.effectiveRolesAt(NOW)).containsExactly("ROLE_AUDITOR");

        AuthUser reloaded = adapter.findByUsername("teller").orElseThrow();
        assertThat(reloaded.getRoleAssignments()).containsExactly(assignment);
        assertThat(reloaded.getRoleVersion()).isEqualTo(4L);
    }

    @Test
    void duplicateApprovalTraceIsIdempotentAndDoesNotIncrementRoleVersionAgain() {
        userRepository.save(userWithRoles());
        userRepository.flush();
        RoleAssignmentReplacement replacement = replacement(
                "governance-approval-id=2",
                "b".repeat(64),
                List.of(RoleAssignment.approved("ROLE_AUDITOR")));

        AuthUser first = roleAssignmentAdapter.replaceRoleAssignments(replacement);
        AuthUser retried = roleAssignmentAdapter.replaceRoleAssignments(replacement);

        assertThat(first.getRoleVersion()).isEqualTo(4L);
        assertThat(retried.getRoleVersion()).isEqualTo(4L);
        assertThat(applyLogRepository.count()).isEqualTo(1L);
    }

    @Test
    void reusedApprovalTraceWithDifferentPayloadFailsClosed() {
        userRepository.save(userWithRoles());
        userRepository.flush();
        roleAssignmentAdapter.replaceRoleAssignments(replacement(
                "governance-approval-id=3",
                "c".repeat(64),
                List.of(RoleAssignment.approved("ROLE_AUDITOR"))));

        assertThatThrownBy(() -> roleAssignmentAdapter.replaceRoleAssignments(replacement(
                "governance-approval-id=3",
                "d".repeat(64),
                List.of(RoleAssignment.approved("ROLE_ADMIN")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different role assignment request");

        AuthUser reloaded = adapter.findByUsername("teller").orElseThrow();
        assertThat(reloaded.getRoleVersion()).isEqualTo(4L);
        assertThat(reloaded.effectiveRolesAt(NOW)).containsExactly("ROLE_AUDITOR");
    }

    private RoleAssignmentReplacement replacement(
            String traceId,
            String fingerprint,
            List<RoleAssignment> assignments) {
        return new RoleAssignmentReplacement(
                "teller",
                assignments,
                "approver01",
                traceId,
                fingerprint);
    }

    private static String randomSecret() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AuthUserJpaEntity userWithRoles() {
        AuthUserJpaEntity user = new AuthUserJpaEntity(
                "teller",
                dynamicPassword,
                "BR001",
                true,
                false,
                3L);
        user.addRoleAssignment(RoleAssignmentJpaEntity.from(new RoleAssignment(
                "ROLE_TELLER",
                "BR001",
                NOW.minus(1, ChronoUnit.DAYS),
                NOW.plus(1, ChronoUnit.DAYS),
                true)));
        user.addRoleAssignment(RoleAssignmentJpaEntity.from(new RoleAssignment(
                "ROLE_PENDING",
                "BR001",
                null,
                null,
                false)));
        user.addRoleAssignment(RoleAssignmentJpaEntity.from(new RoleAssignment(
                "ROLE_EXPIRED",
                "BR001",
                NOW.minus(5, ChronoUnit.DAYS),
                NOW.minus(1, ChronoUnit.DAYS),
                true)));
        return user;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = AuthUserJpaRepository.class)
    @EntityScan(basePackageClasses = {
            AuthUserJpaEntity.class,
            RoleAssignmentJpaEntity.class,
            RoleAssignmentApplyLogJpaEntity.class
    })
    @Import({
            JpaAuthUserQueryAdapter.class,
            JpaAuthUserRoleAssignmentAdapter.class
    })
    static class JpaTestConfiguration {

        @Bean
        Clock authClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
