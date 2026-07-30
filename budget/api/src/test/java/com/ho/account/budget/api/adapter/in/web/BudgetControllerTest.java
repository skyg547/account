package com.ho.account.budget.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.budget.api.BudgetApiApplication;
import com.ho.account.budget.application.exception.BudgetConflictException;
import com.ho.account.budget.application.exception.BudgetResourceNotFoundException;
import com.ho.account.budget.application.exception.BudgetRuleViolationException;
import com.ho.account.budget.application.port.in.BudgetManagementUseCase;
import com.ho.account.budget.application.port.in.CreateBudgetPlanCommand;
import com.ho.account.budget.application.port.in.ExecuteBudgetCommand;
import com.ho.account.budget.application.port.in.RequestBudgetTransferCommand;
import com.ho.account.budget.domain.BudgetExecution;
import com.ho.account.budget.domain.BudgetExecutionStatus;
import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetPlanStatus;
import com.ho.account.budget.domain.BudgetTransfer;
import com.ho.account.budget.domain.BudgetTransferStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(
        classes = BudgetApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "auth.jwt.secret=modern-account-system-super-secret-key-1234567890",
                "auth.jwt.issuer=auth-service",
                "spring.datasource.url=jdbc:h2:mem:budget_api_controller;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        })
@AutoConfigureMockMvc
class BudgetControllerTest {

    private static final String CREATE_PLAN_BODY = """
            {
              "planCode": "PLAN-202601-OPS-6100",
              "yearMonth": "202601",
              "departmentCode": "OPS",
              "accountCode": "6100",
              "allocationAmount": 100000.00
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetManagementUseCase useCase;

    @Test
    void budgetEndpointsRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/budgets/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PLAN_BODY))
                .andExpect(status().isUnauthorized());

        verify(useCase, never()).createPlan(any());
    }

    @Test
    void managerOperationRejectsAnAuthenticatedUserWithWrongRole() throws Exception {
        mockMvc.perform(post("/api/budgets/plans")
                        .with(authenticatedAs("reader", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PLAN_BODY))
                .andExpect(status().isForbidden());

        verify(useCase, never()).createPlan(any());
    }

    @Test
    void forgedBodyActorIsIgnoredAndJwtSubjectBecomesCommandActor() throws Exception {
        when(useCase.createPlan(any())).thenReturn(draftPlan());

        mockMvc.perform(post("/api/budgets/plans")
                        .with(authenticatedAs("authenticated-maker", "ROLE_BUDGET_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "PLAN-202601-OPS-6100",
                                  "yearMonth": "202601",
                                  "departmentCode": "OPS",
                                  "accountCode": "6100",
                                  "allocationAmount": 100000.00,
                                  "actor": "forged-admin"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planCode").value("PLAN-202601-OPS-6100"));

        ArgumentCaptor<CreateBudgetPlanCommand> command =
                ArgumentCaptor.forClass(CreateBudgetPlanCommand.class);
        verify(useCase).createPlan(command.capture());
        assertThat(command.getValue().actor()).isEqualTo("authenticated-maker");
    }

    @Test
    void approvalsRequireApproverRoleAndUseApproverSubject() throws Exception {
        when(useCase.approvePlan(any(), anyString())).thenReturn(approvedPlan());

        mockMvc.perform(post("/api/budgets/plans/11/approval")
                        .with(authenticatedAs("manager", "ROLE_BUDGET_MANAGER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/budgets/plans/11/approval")
                        .with(authenticatedAs("checker", "ROLE_BUDGET_APPROVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedBy").value("checker"));

        verify(useCase).approvePlan(11L, "checker");
    }

    @Test
    void anyAuthenticatedRoleCanReadAvailableBudget() throws Exception {
        when(useCase.findPlan("202601", "OPS", "6100"))
                .thenReturn(java.util.Optional.of(draftPlan()));

        mockMvc.perform(get("/api/budgets/plans/available")
                        .with(authenticatedAs("reader", "ROLE_USER"))
                        .param("yearMonth", "202601")
                        .param("departmentCode", "OPS")
                        .param("accountCode", "6100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableAmount").value(100000.00));
    }

    @Test
    void managerRoleCanRequestExecuteAndCancelWhileActorAlwaysComesFromJwt() throws Exception {
        BudgetTransfer transfer = BudgetTransfer.restore(
                21L,
                "TRANSFER-77",
                11L,
                12L,
                new BigDecimal("500.00"),
                BudgetTransferStatus.REQUESTED,
                "manager-subject",
                null);
        BudgetExecution executed = BudgetExecution.restore(
                31L,
                11L,
                "EXPENDITURE",
                "EXP-77",
                "LINE-2",
                LocalDate.of(2026, 1, 15),
                new BigDecimal("2500.00"),
                BudgetExecutionStatus.EXECUTED,
                "manager-subject",
                null);
        BudgetExecution cancelled = BudgetExecution.restore(
                31L,
                11L,
                "EXPENDITURE",
                "EXP-77",
                "LINE-2",
                LocalDate.of(2026, 1, 15),
                new BigDecimal("2500.00"),
                BudgetExecutionStatus.CANCELLED,
                "manager-subject",
                "manager-subject");
        when(useCase.requestTransfer(any())).thenReturn(transfer);
        when(useCase.execute(any())).thenReturn(executed);
        when(useCase.cancelExecution(31L, "manager-subject")).thenReturn(cancelled);

        mockMvc.perform(post("/api/budgets/transfers")
                        .with(authenticatedAs("manager-subject", "ROLE_BUDGET_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestKey": "TRANSFER-77",
                                  "fromPlanId": 11,
                                  "toPlanId": 12,
                                  "amount": 500.00,
                                  "requester": "forged-requester"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budgets/executions")
                        .with(authenticatedAs("manager-subject", "ROLE_BUDGET_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": 11,
                                  "sourceType": "EXPENDITURE",
                                  "sourceId": "EXP-77",
                                  "sourceLineId": "LINE-2",
                                  "amount": 2500.00,
                                  "executionDate": "2026-01-15",
                                  "actor": "forged-operator"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budgets/executions/31/cancellation")
                        .with(authenticatedAs("manager-subject", "ROLE_BUDGET_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actor\":\"forged-canceller\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledBy").value("manager-subject"));

        ArgumentCaptor<RequestBudgetTransferCommand> transferCommand =
                ArgumentCaptor.forClass(RequestBudgetTransferCommand.class);
        ArgumentCaptor<ExecuteBudgetCommand> executionCommand =
                ArgumentCaptor.forClass(ExecuteBudgetCommand.class);
        verify(useCase).requestTransfer(transferCommand.capture());
        verify(useCase).execute(executionCommand.capture());
        assertThat(transferCommand.getValue().actor()).isEqualTo("manager-subject");
        assertThat(executionCommand.getValue().actor()).isEqualTo("manager-subject");
        verify(useCase).cancelExecution(31L, "manager-subject");
    }

    @Test
    void typedAndBusinessFailuresMapToStableHttpStatuses() throws Exception {
        when(useCase.findPlan("202601", "OPS", "6100"))
                .thenReturn(java.util.Optional.empty());
        mockMvc.perform(get("/api/budgets/plans/available")
                        .with(authenticatedAs("reader", "ROLE_USER"))
                        .param("yearMonth", "202601")
                        .param("departmentCode", "OPS")
                        .param("accountCode", "6100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BUDGET_NOT_FOUND"));

        when(useCase.createPlan(any())).thenThrow(new BudgetConflictException("duplicate plan"));
        mockMvc.perform(post("/api/budgets/plans")
                        .with(authenticatedAs("manager", "ROLE_BUDGET_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PLAN_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUDGET_CONFLICT"));

        reset(useCase);
        when(useCase.createPlan(any())).thenThrow(new BudgetRuleViolationException("period closed"));
        mockMvc.perform(post("/api/budgets/plans")
                        .with(authenticatedAs("manager", "ROLE_BUDGET_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PLAN_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUDGET_RULE_VIOLATION"));

        mockMvc.perform(post("/api/budgets/plans")
                        .with(authenticatedAs("manager", "ROLE_BUDGET_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PLAN_BODY.replace("202601", "202613")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        reset(useCase);
        mockMvc.perform(post("/api/budgets/transfers")
                        .with(authenticatedAs("manager", "ROLE_BUDGET_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestKey": "%s",
                                  "fromPlanId": 11,
                                  "toPlanId": 12,
                                  "amount": 1.00
                                }
                                """.formatted("X".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verify(useCase, never()).requestTransfer(any());
    }

    private RequestPostProcessor authenticatedAs(String subject, String role) {
        return jwt()
                .jwt(token -> token
                        .subject(subject)
                        .issuer("auth-service")
                        .claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority(role));
    }

    private BudgetPlan draftPlan() {
        return BudgetPlan.restore(
                11L,
                "PLAN-202601-OPS-6100",
                "202601",
                "OPS",
                "6100",
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BudgetPlanStatus.DRAFT,
                "authenticated-maker",
                null,
                null);
    }

    private BudgetPlan approvedPlan() {
        return BudgetPlan.restore(
                11L,
                "PLAN-202601-OPS-6100",
                "202601",
                "OPS",
                "6100",
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BudgetPlanStatus.APPROVED,
                "authenticated-maker",
                "checker",
                null);
    }
}
