package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = JpaAuthUserPersistenceAdapterTest.JpaTestConfiguration.class)
class JpaAuthUserPersistenceAdapterTest {

    @Autowired
    private JpaAuthUserQueryAdapter adapter;

    @Autowired
    private JpaAuthUserRoleAssignmentAdapter roleAssignmentAdapter;

    @Autowired
    private AuthUserJpaRepository repository;

    @Test
    void findByUsername_loadsApprovedEffectiveRoleAssignments() {
        repository.save(userWithRoles());
        repository.flush();

        AuthUser user = adapter.findByUsername(" teller ")
                .orElseThrow();

        assertThat(user.getUsername()).isEqualTo("teller");
        assertThat(user.getStoredPassword()).isEqualTo("{noop}1234");
        assertThat(user.getDepartmentCode()).isEqualTo("BR001");
        assertThat(user.getRoleVersion()).isEqualTo(3L);
        assertThat(user.getRoleAssignments()).hasSize(3);
        assertThat(user.getRoles()).containsExactly("ROLE_TELLER");
    }

    @Test
    void seedRunner_persistsConfiguredUsersOnlyWhenMissing() throws Exception {
        AuthModuleProperties properties = new AuthModuleProperties();
        AuthModuleProperties.User configured = new AuthModuleProperties.User();
        configured.setUsername("ops");
        configured.setPassword("{noop}ops");
        configured.setDepartmentCode("OPS");
        configured.setRoles(List.of("ROLE_OPS", "ROLE_AUDITOR"));
        properties.setUsers(List.of(configured));

        AuthUserSeedRunner seedRunner = new AuthUserSeedRunner(properties, repository);
        seedRunner.run(null);
        seedRunner.run(null);

        AuthUser user = adapter.findByUsername("ops")
                .orElseThrow();

        assertThat(repository.count()).isEqualTo(1L);
        assertThat(user.getRoles()).containsExactly("ROLE_OPS", "ROLE_AUDITOR");
        assertThat(user.getRoleVersion()).isEqualTo(1L);
    }

    @Test
    void replaceRoleAssignments_replacesRolesAndIncrementsRoleVersion() {
        repository.save(userWithRoles());
        repository.flush();

        AuthUser updated = roleAssignmentAdapter.replaceRoleAssignments(
                "teller",
                List.of(new RoleAssignment("ROLE_AUDITOR", "GLOBAL", null, null, true)),
                "approver01");

        assertThat(updated.getRoleVersion()).isEqualTo(4L);
        assertThat(updated.getRoles()).containsExactly("ROLE_AUDITOR");

        AuthUser reloaded = adapter.findByUsername("teller").orElseThrow();
        assertThat(reloaded.getRoleAssignments()).hasSize(1);
        assertThat(reloaded.getRoles()).containsExactly("ROLE_AUDITOR");
        assertThat(reloaded.getRoleVersion()).isEqualTo(4L);
    }

    private AuthUserJpaEntity userWithRoles() {
        Instant now = Instant.now();
        AuthUserJpaEntity user = new AuthUserJpaEntity(
                "teller",
                "{noop}1234",
                "BR001",
                true,
                false,
                3L);
        user.addRoleAssignment(RoleAssignmentJpaEntity.from(new RoleAssignment(
                "ROLE_TELLER",
                "BR001",
                now.minus(1, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.DAYS),
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
                now.minus(5, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS),
                true)));
        return user;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = AuthUserJpaRepository.class)
    @EntityScan(basePackageClasses = {
            AuthUserJpaEntity.class,
            RoleAssignmentJpaEntity.class
    })
    @Import({
            JpaAuthUserQueryAdapter.class,
            JpaAuthUserRoleAssignmentAdapter.class
    })
    static class JpaTestConfiguration {
    }
}
