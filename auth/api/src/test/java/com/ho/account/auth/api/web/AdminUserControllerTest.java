package com.ho.account.auth.api.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.auth.core.application.model.AdminUserView;
import com.ho.account.auth.core.application.exception.UserAccessDeniedException;
import com.ho.account.auth.core.application.port.in.AdminUserQueryUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserQueryUseCase adminUserQueryUseCase;

    private MockMvc mockMvc;
    private static final String AUTH_USER_HEADER = "X-Auth-User";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserQueryUseCase))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_SYSTEM_ADMIN", "ROLE_AUDITOR, SYSTEM_ADMIN"})
    void findAllUsersAllowsOnlyTrustedSystemAdminRoles(String authenticatedRoles) throws Exception {
        when(adminUserQueryUseCase.findAllUsers("admin")).thenReturn(List.of(new AdminUserView(
                281_474_976_710_656L,
                "alpha@example.com",
                "alpha@example.com",
                "ACCOUNTING_ADMIN",
                "ACTIVE",
                "",
                "FIN")));

        mockMvc.perform(get("/api/admin/users")
                        .header(AUTH_USER_HEADER, "admin")
                        .header(AdminUserController.AUTH_ROLES_HEADER, authenticatedRoles))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(281_474_976_710_656L))
                .andExpect(jsonPath("$[0].name").value("alpha@example.com"))
                .andExpect(jsonPath("$[0].email").value("alpha@example.com"))
                .andExpect(jsonPath("$[0].role").value("ACCOUNTING_ADMIN"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].lastLogin").value(""))
                .andExpect(jsonPath("$[0].dept").value("FIN"));
        verify(adminUserQueryUseCase).findAllUsers("admin");
    }

    @Test
    void findAllUsersRejectsMissingTrustedRolesHeader() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));

        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void findAllUsersRejectsNonAdminCaller() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(AUTH_USER_HEADER, "admin")
                        .header(AdminUserController.AUTH_ROLES_HEADER, "ROLE_AUDITOR,ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));

        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void findAllUsersRejectsRoleHeaderWithoutAuthenticatedIdentity() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(AdminUserController.AUTH_ROLES_HEADER, "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));

        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void findAllUsersRejectsBlankAuthenticatedIdentity() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(AUTH_USER_HEADER, " ")
                        .header(AdminUserController.AUTH_ROLES_HEADER, "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void findAllUsersRejectsAdminRoleHeaderWhenCurrentStoredAssignmentIsScoped() throws Exception {
        when(adminUserQueryUseCase.findAllUsers("admin"))
                .thenThrow(new UserAccessDeniedException("Global system administrator role is required."));

        mockMvc.perform(get("/api/admin/users")
                        .header(AUTH_USER_HEADER, "admin")
                        .header(AdminUserController.AUTH_ROLES_HEADER, "ROLE_SYSTEM_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));

        verify(adminUserQueryUseCase).findAllUsers("admin");
    }
}
