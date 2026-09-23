package com.ho.account.closing.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.closing.application.port.in.AnnualClosingUseCase;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingTask;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ClosingControllerTest {

    private ClosingUseCase closingUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        closingUseCase = mock(ClosingUseCase.class);
        AnnualClosingUseCase annualClosingUseCase = mock(AnnualClosingUseCase.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ClosingController(closingUseCase, annualClosingUseCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modules(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    @Test
    void getClosingTasksByCalendarIdReturnsMappedTaskDtosInUseCaseOrder() throws Exception {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(42L);

        ClosingTask reconciliation = task(
                101L,
                calendar,
                "Bank reconciliation",
                ClosingTask.ClosingTaskCategory.PRE_CLOSING,
                ClosingTask.ClosingTaskStatus.IN_PROGRESS,
                1,
                true);
        reconciliation.setDescription("Reconcile operating accounts");
        reconciliation.setDueDate(LocalDateTime.of(2026, 9, 30, 18, 0));
        reconciliation.setAssignedTo("closing-team");
        reconciliation.setCompletionConditionJson("{\"status\":\"SUCCESS\"}");
        reconciliation.setCreatedAt(LocalDateTime.of(2026, 9, 1, 9, 0));
        reconciliation.setUpdatedAt(LocalDateTime.of(2026, 9, 24, 10, 30));
        reconciliation.setAuditUser("closer");

        ClosingTask reporting = task(
                102L,
                calendar,
                "Publish reports",
                ClosingTask.ClosingTaskCategory.REPORTING,
                ClosingTask.ClosingTaskStatus.PENDING,
                2,
                false);
        when(closingUseCase.findClosingTasksByCalendarId(42L))
                .thenReturn(List.of(reconciliation, reporting));

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
        when(closingUseCase.findClosingTasksByCalendarId(42L)).thenReturn(List.of());

        mockMvc.perform(get("/api/closing/calendars/42/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(closingUseCase).findClosingTasksByCalendarId(42L);
    }

    private ClosingTask task(
            Long id,
            ClosingCalendar calendar,
            String name,
            ClosingTask.ClosingTaskCategory category,
            ClosingTask.ClosingTaskStatus status,
            int taskOrder,
            boolean mandatory) {
        ClosingTask task = new ClosingTask();
        task.setId(id);
        task.setClosingCalendar(calendar);
        task.setName(name);
        task.setCategory(category);
        task.setStatus(status);
        task.setTaskOrder(taskOrder);
        task.setMandatory(mandatory);
        return task;
    }
}
