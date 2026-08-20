package com.ho.account.shared.infrastructure.security.application.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecurityModelsTest {

    @Test
    @DisplayName("AuditActor 레코드가 올바르게 생성되고 필드를 유지한다")
    void testAuditActor() {
        AuditActor actor = new AuditActor("usr_123", "192.168.1.10");

        assertThat(actor.userId()).isEqualTo("usr_123");
        assertThat(actor.ipAddress()).isEqualTo("192.168.1.10");
    }

    @Test
    @DisplayName("AuthUserRoleAssignmentChange 레코드가 올바르게 생성되고 필드를 유지한다")
    void testAuthUserRoleAssignmentChange() {
        Instant from = Instant.now();
        Instant to = from.plusSeconds(3600);
        AuthUserRoleAssignmentChange change = new AuthUserRoleAssignmentChange(
                "user_lee",
                List.of("ROLE_ACCOUNTANT", "ROLE_AUDITOR"),
                "DEPT-100",
                from,
                to,
                "approver_park",
                "TRACE-999"
        );

        assertThat(change.username()).isEqualTo("user_lee");
        assertThat(change.roleCodes()).containsExactly("ROLE_ACCOUNTANT", "ROLE_AUDITOR");
        assertThat(change.dataScope()).isEqualTo("DEPT-100");
        assertThat(change.validFrom()).isEqualTo(from);
        assertThat(change.validTo()).isEqualTo(to);
        assertThat(change.approvedBy()).isEqualTo("approver_park");
        assertThat(change.approvalTraceId()).isEqualTo("TRACE-999");
    }
}
