package com.ho.account.shared.infrastructure.security.infrastructure.auth;

import com.ho.account.shared.infrastructure.security.application.model.AuthUserRoleAssignmentChange;
import com.ho.account.shared.infrastructure.security.application.port.out.AuthUserRoleAssignmentApplyPort;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class RestClientAuthUserRoleAssignmentAdapter implements AuthUserRoleAssignmentApplyPort {

    private static final String INTERNAL_AUTH_TOKEN_HEADER = "X-Internal-Auth-Token";

    private final RestClient authRestClient;
    private final AuthIntegrationProperties authIntegrationProperties;

    public RestClientAuthUserRoleAssignmentAdapter(
            @Qualifier("authRestClient") RestClient authRestClient,
            AuthIntegrationProperties authIntegrationProperties) {
        this.authRestClient = authRestClient;
        this.authIntegrationProperties = authIntegrationProperties;
    }

    @Override
    public void replaceUserRoles(AuthUserRoleAssignmentChange change) {
        authRestClient.post()
                .uri("/api/auth/internal/users/{username}/role-assignments", change.username())
                .header(INTERNAL_AUTH_TOKEN_HEADER, internalToken())
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

    private String internalToken() {
        String token = authIntegrationProperties.getInternalToken();
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("governance.integrations.auth.internal-token must be configured");
        }
        return token;
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
