package com.ho.account.auth.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.auth.core.application.model.AuthenticationResult;
import com.ho.account.auth.core.application.port.in.AuthUseCase;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.List;
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
                                {"username":"admin","password":"1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.roleVersion").value(4));

        ArgumentCaptor<AuthUseCase.LoginCommand> commandCaptor =
                ArgumentCaptor.forClass(AuthUseCase.LoginCommand.class);
        verify(authUseCase).login(commandCaptor.capture());
        assertThat(commandCaptor.getValue().username()).isEqualTo("admin");
        assertThat(commandCaptor.getValue().password()).isEqualTo("1234");
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
        assertThat(command.dataScope()).isEqualTo("FIN");
        assertThat(command.approvedBy()).isEqualTo("approver01");
        assertThat(command.approvalTraceId()).isEqualTo("governance-approval-id=42");
    }

    private String validRoleAssignmentBody() {
        return """
                {
                  "roleCodes": ["ROLE_ACCOUNTING_ADMIN"],
                  "dataScope": "FIN",
                  "approvedBy": "approver01",
                  "approvalTraceId": "governance-approval-id=42"
                }
                """;
    }
}
