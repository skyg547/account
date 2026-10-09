package com.ho.account.closing.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.closing.application.port.in.AnnualClosingUseCase;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.domain.ClosingAdjustment;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.PeriodLock;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ValuationBatch;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ClosingControllerTest {

    private static final String TRUSTED_ACTOR = "gateway-operator";
    private static final String FORGED_ACTOR = "forged-body-user";

    private ClosingUseCase closingUseCase;
    private AnnualClosingUseCase annualClosingUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        closingUseCase = mock(ClosingUseCase.class);
        annualClosingUseCase = mock(AnnualClosingUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ClosingController(closingUseCase, annualClosingUseCase))
                .setControllerAdvice(new ClosingExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modules(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mutationRequests")
    void everyMutationRejectsMissingActorAndMissingOrInvalidRoleBeforeUseCase(
            String description, MutationRequest mutation) throws Exception {
        mockMvc.perform(mutation.request().header("X-Auth-Roles", "CLOSING_MANAGER"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(mutation.request().header("X-Auth-User", TRUSTED_ACTOR))
                .andExpect(status().isForbidden());
        mockMvc.perform(mutation.request()
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "ROLE_AUDITOR"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(closingUseCase, annualClosingUseCase);
    }

    @Test
    void commandRejectsActorLongerThanPersistedAuditLimitBeforeUseCase() throws Exception {
        mockMvc.perform(post("/api/closing/calendars/determine-status")
                        .header("X-Auth-User", "a".repeat(51))
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendarId\":9}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(closingUseCase, annualClosingUseCase);
    }

    @Test
    void creationCommandsBindTrustedActorToAggregateAuditUser() throws Exception {
        ClosingCalendar parent = new ClosingCalendar();
        parent.setId(7L);
        when(closingUseCase.findClosingCalendarById(7L)).thenReturn(parent);
        when(closingUseCase.createClosingCalendar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(closingUseCase.createClosingTask(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(closingUseCase.createClosingGate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(authorized(post("/api/closing/calendars"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fiscalYear\":\"2026\",\"fiscalPeriod\":\"01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditUser").value(TRUSTED_ACTOR));
        mockMvc.perform(authorized(post("/api/closing/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendarId\":7,\"name\":\"Reconcile\",\"category\":\"PRE_CLOSING\",\"taskOrder\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditUser").value(TRUSTED_ACTOR));
        mockMvc.perform(authorized(post("/api/closing/gates"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendarId\":7,\"name\":\"Checklist gate\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditUser").value(TRUSTED_ACTOR));

        ArgumentCaptor<ClosingCalendar> calendar = ArgumentCaptor.forClass(ClosingCalendar.class);
        ArgumentCaptor<ClosingTask> task = ArgumentCaptor.forClass(ClosingTask.class);
        ArgumentCaptor<ClosingGate> gate = ArgumentCaptor.forClass(ClosingGate.class);
        verify(closingUseCase).createClosingCalendar(calendar.capture());
        verify(closingUseCase).createClosingTask(task.capture());
        verify(closingUseCase).createClosingGate(gate.capture());
        assertThat(calendar.getValue().getAuditUser()).isEqualTo(TRUSTED_ACTOR);
        assertThat(task.getValue().getAuditUser()).isEqualTo(TRUSTED_ACTOR);
        assertThat(gate.getValue().getAuditUser()).isEqualTo(TRUSTED_ACTOR);
    }

    @Test
    void trustedHeaderActorOverridesEveryLegacyBodyAndQueryActor() throws Exception {
        when(closingUseCase.updateClosingCalendarStatus(9L, ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS,
                TRUSTED_ACTOR)).thenReturn(mock(ClosingCalendar.class));
        when(closingUseCase.updateClosingTaskStatus(10L, ClosingTask.ClosingTaskStatus.COMPLETED,
                TRUSTED_ACTOR)).thenReturn(mock(ClosingTask.class));
        when(closingUseCase.checkAndPassClosingGate(11L, TRUSTED_ACTOR)).thenReturn(mock(ClosingGate.class));
        when(closingUseCase.lockPeriod(12L, PeriodLock.PeriodLockType.ALL_TRANSACTIONS, TRUSTED_ACTOR,
                "close period")).thenReturn(mock(PeriodLock.class));
        when(closingUseCase.requestPeriodReopen(12L, TRUSTED_ACTOR, "material correction"))
                .thenReturn(mock(ReopenApproval.class));
        when(closingUseCase.updateReopenApprovalStatus(13L, ReopenApproval.ReopenApprovalStatus.APPROVED,
                TRUSTED_ACTOR)).thenReturn(mock(ReopenApproval.class));
        when(closingUseCase.runValuationBatch(12L, ValuationBatch.ValuationType.FX_RATE, TRUSTED_ACTOR))
                .thenReturn(mock(ValuationBatch.class));
        when(closingUseCase.runProvisionBatch(12L, ProvisionBatch.ProvisionType.ECL, TRUSTED_ACTOR))
                .thenReturn(mock(ProvisionBatch.class));
        when(closingUseCase.createClosingAdjustment(12L, 14L, ClosingAdjustment.AdjustmentType.ACCRUAL,
                "closing accrual", TRUSTED_ACTOR)).thenReturn(mock(ClosingAdjustment.class));
        when(closingUseCase.determineClosingStatus(9L, TRUSTED_ACTOR)).thenReturn(mock(ClosingCalendar.class));

        mockMvc.perform(authorized(put("/api/closing/calendars/9/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"status\":\"IN_PROGRESS\",\"user\":\"%s\"}")))
                .andExpect(status().isOk());
        mockMvc.perform(authorized(put("/api/closing/tasks/10/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"status\":\"COMPLETED\",\"user\":\"%s\"}")))
                .andExpect(status().isOk());
        mockMvc.perform(authorized(put("/api/closing/gates/11/check").param("user", FORGED_ACTOR)))
                .andExpect(status().isOk());
        mockMvc.perform(authorized(post("/api/closing/period-locks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"fiscalPeriodId\":12,\"lockType\":\"ALL_TRANSACTIONS\",\"user\":\"%s\",\"reason\":\"close period\"}")))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(delete("/api/closing/period-locks/12").param("user", FORGED_ACTOR)))
                .andExpect(status().isNoContent());
        mockMvc.perform(authorized(post("/api/closing/reopen-approvals"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"fiscalPeriodId\":12,\"requestedBy\":\"%s\",\"reason\":\"material correction\"}")))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(put("/api/closing/reopen-approvals/13/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"status\":\"APPROVED\",\"approvedBy\":\"%s\"}")))
                .andExpect(status().isOk());
        mockMvc.perform(authorized(post("/api/closing/valuation-batches/run"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"fiscalPeriodId\":12,\"valuationType\":\"FX_RATE\",\"runBy\":\"%s\"}")))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/closing/provision-batches/run"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"fiscalPeriodId\":12,\"provisionType\":\"ECL\",\"runBy\":\"%s\"}")))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/closing/adjustments"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"fiscalPeriodId\":12,\"journalEntryId\":14,\"adjustmentType\":\"ACCRUAL\",\"description\":\"closing accrual\",\"approvedBy\":\"%s\"}")))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(post("/api/closing/calendars/determine-status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actorJson("{\"calendarId\":9,\"user\":\"%s\"}")))
                .andExpect(status().isOk());

        verify(closingUseCase).updateClosingCalendarStatus(
                9L, ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS, TRUSTED_ACTOR);
        verify(closingUseCase).updateClosingTaskStatus(10L, ClosingTask.ClosingTaskStatus.COMPLETED, TRUSTED_ACTOR);
        verify(closingUseCase).checkAndPassClosingGate(11L, TRUSTED_ACTOR);
        verify(closingUseCase).lockPeriod(
                12L, PeriodLock.PeriodLockType.ALL_TRANSACTIONS, TRUSTED_ACTOR, "close period");
        verify(closingUseCase).unlockPeriod(12L, TRUSTED_ACTOR);
        verify(closingUseCase).requestPeriodReopen(12L, TRUSTED_ACTOR, "material correction");
        verify(closingUseCase).updateReopenApprovalStatus(
                13L, ReopenApproval.ReopenApprovalStatus.APPROVED, TRUSTED_ACTOR);
        verify(closingUseCase).runValuationBatch(12L, ValuationBatch.ValuationType.FX_RATE, TRUSTED_ACTOR);
        verify(closingUseCase).runProvisionBatch(12L, ProvisionBatch.ProvisionType.ECL, TRUSTED_ACTOR);
        verify(closingUseCase).createClosingAdjustment(
                12L, 14L, ClosingAdjustment.AdjustmentType.ACCRUAL, "closing accrual", TRUSTED_ACTOR);
        verify(closingUseCase).determineClosingStatus(9L, TRUSTED_ACTOR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ROLE_ACCOUNTING_ADMIN", "closing_manager"})
    void everySupportedClosingRoleCanInvokeAnnualCloseWithoutChangingCoreSignature(String role) throws Exception {
        mockMvc.perform(post("/api/closing/annual/perform-income-statement-closing")
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "ROLE_AUDITOR, " + role)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026}"))
                .andExpect(status().isOk());

        verify(annualClosingUseCase).performIncomeStatementClosing(2026);
    }

    @Test
    void getClosingTasksByCalendarIdReturnsMappedTaskDtosInUseCaseOrder() throws Exception {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(42L);
        ClosingTask reconciliation = task(101L, calendar, "Bank reconciliation",
                ClosingTask.ClosingTaskCategory.PRE_CLOSING, ClosingTask.ClosingTaskStatus.IN_PROGRESS, 1, true);
        reconciliation.setDescription("Reconcile operating accounts");
        reconciliation.setDueDate(LocalDateTime.of(2026, 9, 30, 18, 0));
        reconciliation.setAssignedTo("closing-team");
        reconciliation.setCompletionConditionJson("{\"status\":\"SUCCESS\"}");
        reconciliation.setCreatedAt(LocalDateTime.of(2026, 9, 1, 9, 0));
        reconciliation.setUpdatedAt(LocalDateTime.of(2026, 9, 24, 10, 30));
        reconciliation.setAuditUser("closer");
        ClosingTask reporting = task(102L, calendar, "Publish reports",
                ClosingTask.ClosingTaskCategory.REPORTING, ClosingTask.ClosingTaskStatus.PENDING, 2, false);
        when(closingUseCase.findClosingTasksByCalendarId(42L)).thenReturn(List.of(reconciliation, reporting));

        mockMvc.perform(get("/api/closing/calendars/42/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].calendarId").value(42))
                .andExpect(jsonPath("$[0].name").value("Bank reconciliation"))
                .andExpect(jsonPath("$[0].description").value("Reconcile operating accounts"))
                .andExpect(jsonPath("$[0].category").value("PRE_CLOSING"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-09-30T18:00:00"))
                .andExpect(jsonPath("$[0].assignedTo").value("closing-team"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].completionConditionJson").value("{\"status\":\"SUCCESS\"}"))
                .andExpect(jsonPath("$[0].mandatory").value(true))
                .andExpect(jsonPath("$[0].taskOrder").value(1))
                .andExpect(jsonPath("$[0].createdAt").value("2026-09-01T09:00:00"))
                .andExpect(jsonPath("$[0].updatedAt").value("2026-09-24T10:30:00"))
                .andExpect(jsonPath("$[0].auditUser").value("closer"))
                .andExpect(jsonPath("$[1].id").value(102))
                .andExpect(jsonPath("$[1].calendarId").value(42))
                .andExpect(jsonPath("$[1].name").value("Publish reports"))
                .andExpect(jsonPath("$[1].category").value("REPORTING"))
                .andExpect(jsonPath("$[1].status").value("PENDING"))
                .andExpect(jsonPath("$[1].mandatory").value(false))
                .andExpect(jsonPath("$[1].taskOrder").value(2));

        verify(closingUseCase).findClosingTasksByCalendarId(42L);
    }

    @Test
    void getClosingTasksByCalendarIdReturnsEmptyArrayWhenCalendarHasNoTasks() throws Exception {
        when(closingUseCase.findClosingTasksByCalendarId(8L)).thenReturn(List.of());

        mockMvc.perform(get("/api/closing/calendars/8/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(closingUseCase).findClosingTasksByCalendarId(8L);
    }

    @Test
    void calendarReadsRemainAvailableWithoutCommandHeaders() throws Exception {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(9L);
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("01");
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.OPEN);
        when(closingUseCase.findClosingCalendarById(9L)).thenReturn(calendar);
        when(closingUseCase.findClosingCalendarByFiscalPeriod("2026", "01")).thenReturn(calendar);

        mockMvc.perform(get("/api/closing/calendars/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
        mockMvc.perform(get("/api/closing/calendars/by-period")
                        .param("fiscalYear", "2026")
                        .param("fiscalPeriod", "01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(closingUseCase).findClosingCalendarById(9L);
        verify(closingUseCase).findClosingCalendarByFiscalPeriod("2026", "01");
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header("X-Auth-User", "  " + TRUSTED_ACTOR + "  ")
                .header("X-Auth-Roles", "ROLE_CLOSING_MANAGER");
    }

    private String actorJson(String template) {
        return template.formatted(FORGED_ACTOR);
    }

    private static Stream<Arguments> mutationRequests() {
        return Stream.of(
                Arguments.of("create calendar", new MutationRequest("POST", "/api/closing/calendars",
                        "{\"fiscalYear\":\"2026\",\"fiscalPeriod\":\"01\"}")),
                Arguments.of("update calendar", new MutationRequest("PUT", "/api/closing/calendars/9/status",
                        "{\"status\":\"IN_PROGRESS\",\"user\":\"forged\"}")),
                Arguments.of("create task", new MutationRequest("POST", "/api/closing/tasks",
                        "{\"calendarId\":7,\"name\":\"Task\",\"category\":\"PRE_CLOSING\",\"taskOrder\":0}")),
                Arguments.of("update task", new MutationRequest("PUT", "/api/closing/tasks/10/status",
                        "{\"status\":\"COMPLETED\",\"user\":\"forged\"}")),
                Arguments.of("create gate", new MutationRequest("POST", "/api/closing/gates",
                        "{\"calendarId\":7,\"name\":\"Gate\"}")),
                Arguments.of("pass gate", new MutationRequest("PUT", "/api/closing/gates/11/check?user=forged", null)),
                Arguments.of("lock period", new MutationRequest("POST", "/api/closing/period-locks",
                        "{\"fiscalPeriodId\":12,\"lockType\":\"ALL_TRANSACTIONS\",\"user\":\"forged\"}")),
                Arguments.of("unlock period", new MutationRequest("DELETE", "/api/closing/period-locks/12?user=forged", null)),
                Arguments.of("request reopen", new MutationRequest("POST", "/api/closing/reopen-approvals",
                        "{\"fiscalPeriodId\":12,\"requestedBy\":\"forged\",\"reason\":\"correction\"}")),
                Arguments.of("decide reopen", new MutationRequest("PUT", "/api/closing/reopen-approvals/13/status",
                        "{\"status\":\"APPROVED\",\"approvedBy\":\"forged\"}")),
                Arguments.of("run valuation", new MutationRequest("POST", "/api/closing/valuation-batches/run",
                        "{\"fiscalPeriodId\":12,\"valuationType\":\"FX_RATE\",\"runBy\":\"forged\"}")),
                Arguments.of("run provision", new MutationRequest("POST", "/api/closing/provision-batches/run",
                        "{\"fiscalPeriodId\":12,\"provisionType\":\"ECL\",\"runBy\":\"forged\"}")),
                Arguments.of("create adjustment", new MutationRequest("POST", "/api/closing/adjustments",
                        "{\"fiscalPeriodId\":12,\"journalEntryId\":14,\"adjustmentType\":\"ACCRUAL\",\"approvedBy\":\"forged\"}")),
                Arguments.of("determine status", new MutationRequest("POST", "/api/closing/calendars/determine-status",
                        "{\"calendarId\":9,\"user\":\"forged\"}")),
                Arguments.of("annual close", new MutationRequest("POST",
                        "/api/closing/annual/perform-income-statement-closing", "{\"year\":2026}")));
    }

    private ClosingTask task(Long id, ClosingCalendar calendar, String name,
                             ClosingTask.ClosingTaskCategory category, ClosingTask.ClosingTaskStatus status,
                             int order, boolean mandatory) {
        ClosingTask task = new ClosingTask();
        task.setId(id);
        task.setClosingCalendar(calendar);
        task.setName(name);
        task.setDescription(name + " description");
        task.setCategory(category);
        task.setDueDate(LocalDateTime.of(2026, 1, order, 12, 0));
        task.setAssignedTo("closer-" + order);
        task.setStatus(status);
        task.setCompletionConditionJson("{\"required\":true}");
        task.setMandatory(mandatory);
        task.setTaskOrder(order);
        task.setAuditUser("audit-" + order);
        return task;
    }

    private record MutationRequest(String method, String path, String body) {
        MockHttpServletRequestBuilder request() {
            MockHttpServletRequestBuilder request = switch (method) {
                case "POST" -> post(path);
                case "PUT" -> put(path);
                case "DELETE" -> delete(path);
                default -> throw new AssertionError("Unsupported method: " + method);
            };
            if (body != null) {
                request.contentType(MediaType.APPLICATION_JSON).content(body);
            }
            return request;
        }
    }

    @Test
    void performIncomeStatementClosingRejectsUnknownFields() throws Exception {
        mockMvc.perform(post("/api/closing/annual/perform-income-statement-closing")
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026, \"retainedEarningsAccountCode\":\"31000\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(annualClosingUseCase);
    }

    @Test
    void performIncomeStatementClosingRejectsInvalidYear() throws Exception {
        mockMvc.perform(post("/api/closing/annual/perform-income-statement-closing")
                        .header("X-Auth-User", TRUSTED_ACTOR)
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":1899}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(annualClosingUseCase);
    }
}
