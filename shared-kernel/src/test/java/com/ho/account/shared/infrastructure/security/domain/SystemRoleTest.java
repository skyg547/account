package com.ho.account.shared.infrastructure.security.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * RBAC 역할 생성과 권한 부여 팩토리의 입력 정규화/검증 규칙을 검증합니다.
 */
class SystemRoleTest {

    @Test
    void createTrimsCodeAndNameAndKeepsDescription() {
        SystemRole role = SystemRole.create("  ROLE_ACCOUNTANT  ", "  회계 담당  ", "설명", "tester");

        assertThat(role.getRoleCode()).isEqualTo("ROLE_ACCOUNTANT");
        assertThat(role.getRoleName()).isEqualTo("회계 담당");
        assertThat(role.getDescription()).isEqualTo("설명");
        assertThat(role.getAuditUser()).isEqualTo("tester");
        assertThat(role.getAuthorizations()).isEmpty();
    }

    @Test
    void createFallsBackToSystemAuditUserWhenMissing() {
        assertThat(SystemRole.create("ROLE_A", "역할", null, null).getAuditUser()).isEqualTo("SYSTEM");
        assertThat(SystemRole.create("ROLE_A", "역할", null, "   ").getAuditUser()).isEqualTo("SYSTEM");
    }

    @Test
    void createRejectsBlankRoleCode() {
        assertThatThrownBy(() -> SystemRole.create("  ", "역할", null, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role code is required.");
    }

    @Test
    void createRejectsBlankRoleName() {
        assertThatThrownBy(() -> SystemRole.create("ROLE_A", null, null, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role name is required.");
    }

    @Test
    void grantAppendsAuthorizationBoundToTheRole() {
        SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

        Authorization granted = role.grant("JOURNAL.CREATE", AccessType.WRITE, "DEPT_01", "tester");

        assertThat(role.getAuthorizations()).containsExactly(granted);
        assertThat(granted.getRole()).isSameAs(role);
        assertThat(granted.getFunctionCode()).isEqualTo("JOURNAL.CREATE");
        assertThat(granted.getAccessType()).isEqualTo(AccessType.WRITE);
        assertThat(granted.getDataScope()).isEqualTo("DEPT_01");
    }

    @Test
    void grantAccumulatesMultipleAuthorizationsInOrder() {
        SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

        Authorization first = role.grant("JOURNAL.CREATE", AccessType.WRITE, null, "tester");
        Authorization second = role.grant("JOURNAL.READ", AccessType.READ, null, "tester");

        assertThat(role.getAuthorizations()).containsExactly(first, second);
    }

    @Test
    void grantDefaultsDataScopeToGlobalAndAuditUserToSystem() {
        SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

        Authorization granted = role.grant("JOURNAL.CREATE", AccessType.WRITE, "  ", null);

        assertThat(granted.getDataScope()).isEqualTo("GLOBAL");
        assertThat(granted.getAuditUser()).isEqualTo("SYSTEM");
    }

    @Test
    void grantRejectsBlankFunctionCodeWithoutMutatingRole() {
        SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

        assertThatThrownBy(() -> role.grant("  ", AccessType.WRITE, null, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Function code is required.");

        assertThat(role.getAuthorizations()).isEmpty();
    }

    @Test
    void grantRejectsNullAccessTypeWithoutMutatingRole() {
        SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

        assertThatThrownBy(() -> role.grant("JOURNAL.CREATE", null, null, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access type is required.");

        assertThat(role.getAuthorizations()).isEmpty();
    }

    @Test
    void authorizationGrantRejectsNullRole() {
        assertThatThrownBy(() -> Authorization.grant(null, "JOURNAL.CREATE", AccessType.WRITE, null, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role is required.");
    }

    @Test
    void authorizationGrantTrimsFunctionCodeAndDataScope() {
        SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

        Authorization granted = Authorization.grant(role, "  JOURNAL.CREATE  ", AccessType.WRITE, "  DEPT_01  ", "  tester  ");

        assertThat(granted.getFunctionCode()).isEqualTo("JOURNAL.CREATE");
        assertThat(granted.getDataScope()).isEqualTo("DEPT_01");
        assertThat(granted.getAuditUser()).isEqualTo("tester");
    }
}
