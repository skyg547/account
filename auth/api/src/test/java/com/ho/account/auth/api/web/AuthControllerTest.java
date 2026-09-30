package com.ho.account.auth.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.auth.core.application.model.AuthenticationResult;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import com.ho.account.auth.core.application.port.out.OtpVerificationPort;
import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import com.ho.account.auth.core.application.port.out.SsoAuthenticationPort;
import com.ho.account.auth.core.application.port.out.TokenIssuerPort;
import com.ho.account.auth.core.application.service.AuthService;
import com.ho.account.auth.core.application.service.AuthUserRoleAssignmentService;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String INTERNAL_AUTH_TOKEN_HEADER = "X-Internal-Auth-Token";

    @Mock
    private AuthUseCase authUseCase;

    @Mock
    private AuthUserRoleAssignmentUseCase authUserRoleAssignmentUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getInternalApi().setToken("secret-token");
        AuthController controller = new AuthController(authUseCase, authUserRoleAssignmentUseCase, properties);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void loginMapsApiRequestToCoreCommandAndCoreResultToResponse() throws Exception {
        String ssoCredential = UUID.randomUUID().toString();
        String otpCode = "%06d".formatted(Math.floorMod(UUID.randomUUID().hashCode(), 1_000_000));
        when(authUseCase.login(any())).thenReturn(new AuthenticationResult(
                "jwt-token",
                3600L,
                "admin",
                "FIN",
                List.of("ROLE_ADMIN"),
                4L));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"admin",
                                  "password":"%s",
                                  "loginType":"SSO",
                                  "otpCode":"%s",
                                  "ssoProvider":"corporate-oidc"
                                }
                                """.formatted(ssoCredential, otpCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.roleVersion").value(4));

        ArgumentCaptor<AuthUseCase.LoginCommand> commandCaptor =
                ArgumentCaptor.forClass(AuthUseCase.LoginCommand.class);
        verify(authUseCase).login(commandCaptor.capture());
        assertThat(commandCaptor.getValue().username()).isEqualTo("admin");
        assertThat(commandCaptor.getValue().password()).isEqualTo(ssoCredential);
        assertThat(commandCaptor.getValue().loginType()).isEqualTo("SSO");
        assertThat(commandCaptor.getValue().otpCode()).isEqualTo(otpCode);
        assertThat(commandCaptor.getValue().ssoProvider()).isEqualTo("corporate-oidc");
    }

    @Test
    void replaceRoleAssignmentsRequiresInternalToken() throws Exception {
        mockMvc.perform(post("/api/auth/internal/users/admin/role-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRoleAssignmentBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));

        verifyNoInteractions(authUserRoleAssignmentUseCase);
    }

    @Test
    void replaceRoleAssignmentsRequiresApprovalTraceId() throws Exception {
        mockMvc.perform(post("/api/auth/internal/users/admin/role-assignments")
                        .header(INTERNAL_AUTH_TOKEN_HEADER, "secret-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCodes": ["ROLE_ACCOUNTING_ADMIN"],
                                  "approvedBy": "approver01"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authUserRoleAssignmentUseCase);
    }

    @Test
    void replaceRoleAssignmentsAppliesWhenInternalTokenMatches() throws Exception {
        when(authUserRoleAssignmentUseCase.replaceRoleAssignments(any()))
                .thenReturn(new AuthUserRoleAssignmentUseCase.RoleAssignmentResult(
                        "admin",
                        3L,
                        List.of("ROLE_ACCOUNTING_ADMIN")));

        mockMvc.perform(post("/api/auth/internal/users/admin/role-assignments")
                        .header(INTERNAL_AUTH_TOKEN_HEADER, "secret-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRoleAssignmentBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roleVersion").value(3))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ACCOUNTING_ADMIN"));

        ArgumentCaptor<AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand> commandCaptor =
                ArgumentCaptor.forClass(AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand.class);
        verify(authUserRoleAssignmentUseCase).replaceRoleAssignments(commandCaptor.capture());

        AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand command = commandCaptor.getValue();
        assertThat(command.username()).isEqualTo("admin");
        assertThat(command.roleCodes()).containsExactly("ROLE_ACCOUNTING_ADMIN");
        assertThat(command.dataScope()).isEqualTo("GLOBAL");
        assertThat(command.approvedBy()).isEqualTo("approver01");
        assertThat(command.approvalTraceId()).isEqualTo("governance-approval-id=42");
    }

    @Test
    void validateTokenVersionRejectsOldTokenForExistingScopedAdministrator() throws Exception {
        AuthUserQueryPort users = mock(AuthUserQueryPort.class);
        AuthUser scopedAdmin = new AuthUser("admin", "stored", "FIN", true, false,
                List.of(new RoleAssignment("ROLE_SYSTEM_ADMIN", "FIN", null, null, true)), 4L);
        when(users.findByUsername("admin")).thenReturn(Optional.of(scopedAdmin));
        AuthService service = new AuthService(
                users,
                mock(DepartmentValidationPort.class),
                mock(PasswordVerifierPort.class),
                mock(OtpVerificationPort.class),
                mock(SsoAuthenticationPort.class),
                mock(TokenIssuerPort.class),
                mock(LoginAttemptPort.class),
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        MockMvc scopedMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        service, authUserRoleAssignmentUseCase, new AuthModuleProperties()))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();

        scopedMvc.perform(post("/api/auth/validate-token-version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"roleVersion\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void replaceRoleAssignmentsRejectsMissingAndBlankScopeBeforeUseCase() throws Exception {
        for (String scopeProperty : List.of("", ",\"dataScope\":\"\"", ",\"dataScope\":\" \"")) {
            mockMvc.perform(post("/api/auth/internal/users/admin/role-assignments")
                            .header(INTERNAL_AUTH_TOKEN_HEADER, "secret-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"roleCodes\":[\"ROLE_SYSTEM_ADMIN\"],"
                                    + "\"approvedBy\":\"approver01\","
                                    + "\"approvalTraceId\":\"governance-approval-id=scope-http\""
                                    + scopeProperty + "}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(authUserRoleAssignmentUseCase);
    }

    @Test
    void replaceRoleAssignmentsRejectsNonGlobalScopeWithoutPersistenceMutation() throws Exception {
        AtomicBoolean persisted = new AtomicBoolean();
        AuthUserRoleAssignmentService service = new AuthUserRoleAssignmentService(replacement -> {
            persisted.set(true);
            throw new AssertionError("Non-GLOBAL scope reached persistence");
        }, Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getInternalApi().setToken("secret-token");
        MockMvc serviceMvc = MockMvcBuilders.standaloneSetup(new AuthController(authUseCase, service, properties))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();

        serviceMvc.perform(post("/api/auth/internal/users/admin/role-assignments")
                        .header(INTERNAL_AUTH_TOKEN_HEADER, "secret-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCodes\":[\"ROLE_SYSTEM_ADMIN\"],"
                                + "\"dataScope\":\"FIN\","
                                + "\"approvedBy\":\"approver01\","
                                + "\"approvalTraceId\":\"governance-approval-id=scoped-http\"}"))
                .andExpect(status().isBadRequest());

        assertThat(persisted).isFalse();
    }

    private String validRoleAssignmentBody() {
        return """
                {
                  "roleCodes": ["ROLE_ACCOUNTING_ADMIN"],
                  "dataScope": "GLOBAL",
                  "approvedBy": "approver01",
                  "approvalTraceId": "governance-approval-id=42"
                }
                """;
    }
}
