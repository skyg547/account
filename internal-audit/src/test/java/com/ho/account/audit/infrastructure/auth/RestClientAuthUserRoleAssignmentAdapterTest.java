package com.ho.account.audit.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ho.account.audit.application.model.AuthUserRoleAssignmentChange;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestClientAuthUserRoleAssignmentAdapterTest {

    @Test
    void replaceUserRoles_sendsInternalAuthTokenHeader() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://auth-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AuthIntegrationProperties properties = new AuthIntegrationProperties();
        properties.setInternalToken("secret-token");
        RestClientAuthUserRoleAssignmentAdapter adapter = new RestClientAuthUserRoleAssignmentAdapter(
                restClientBuilder.build(),
                properties);
        Instant validFrom = Instant.parse("2026-06-01T00:00:00Z");

        server.expect(once(), requestTo("http://auth-service/api/auth/internal/users/admin/role-assignments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Auth-Token", "secret-token"))
                .andRespond(withSuccess());

        adapter.replaceUserRoles(new AuthUserRoleAssignmentChange(
                "admin",
                List.of("ROLE_ACCOUNTING_ADMIN"),
                "FIN",
                validFrom,
                null,
                "approver01",
                "governance-approval-id=42"));

        server.verify();
    }

    @Test
    void replaceUserRoles_rejectsBlankInternalTokenConfiguration() {
        RestClientAuthUserRoleAssignmentAdapter adapter = new RestClientAuthUserRoleAssignmentAdapter(
                RestClient.builder().baseUrl("http://auth-service").build(),
                blankTokenProperties());

        assertThatThrownBy(() -> adapter.replaceUserRoles(new AuthUserRoleAssignmentChange(
                        "admin",
                        List.of("ROLE_ACCOUNTING_ADMIN"),
                        "FIN",
                        Instant.parse("2026-06-01T00:00:00Z"),
                        null,
                        "approver01",
                        "governance-approval-id=42")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("governance.integrations.auth.internal-token must be configured");
    }

    private AuthIntegrationProperties blankTokenProperties() {
        AuthIntegrationProperties properties = new AuthIntegrationProperties();
        properties.setInternalToken(" ");
        return properties;
    }
}
