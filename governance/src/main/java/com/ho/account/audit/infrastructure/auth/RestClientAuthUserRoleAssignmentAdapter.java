package com.ho.account.audit.infrastructure.auth;

import com.ho.account.audit.application.model.AuthUserRoleAssignmentChange;
import com.ho.account.audit.application.port.out.AuthUserRoleAssignmentApplyPort;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientAuthUserRoleAssignmentAdapter implements AuthUserRoleAssignmentApplyPort {

    private final RestClient authRestClient;

    public RestClientAuthUserRoleAssignmentAdapter(@Qualifier("authRestClient") RestClient authRestClient) {
        this.authRestClient = authRestClient;
    }

    @Override
    public void replaceUserRoles(AuthUserRoleAssignmentChange change) {
        authRestClient.post()
                .uri("/api/auth/internal/users/{username}/role-assignments", change.username())
                .body(new RoleAssignmentApplyRequest(
                        change.roleCodes(),
                        change.dataScope(),
                        change.validFrom(),
                        change.validTo(),
                        change.approvedBy(),
                        change.approvalTraceId()))
                .retrieve()
                .toBodilessEntity();
    }

    private record RoleAssignmentApplyRequest(
            List<String> roleCodes,
            String dataScope,
            Instant validFrom,
            Instant validTo,
            String approvedBy,
            String approvalTraceId) {
    }
}
