package com.ho.account.journalledger.adapter.in.web.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JournalCommandAuthorizationMvcTest {

    private JournalUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(JournalUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new JournalController(useCase)).build();
    }

    @Test
    void viewerCannotSubmitApproveOrPost() throws Exception {
        for (String path : new String[]{"request-approval", "approve", "post"}) {
            mvc.perform(post("/api/journals/10/" + path)
                            .header("X-Auth-User", "viewer-1")
                            .header("X-Auth-Roles", "ROLE_JOURNAL_VIEWER"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(useCase);
    }

    @Test
    void makerCanSubmitButCannotApprove() throws Exception {
        mvc.perform(post("/api/journals/10/request-approval")
                        .header("X-Auth-User", " Maker-1 ")
                        .header("X-Auth-Roles", "journal_maker"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/journals/10/approve")
                        .header("X-Auth-User", "maker-1")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER"))
                .andExpect(status().isForbidden());

        verify(useCase).requestJournalEntryApproval(10L, "maker-1");
    }

    @Test
    void approverCanApproveButCannotPost() throws Exception {
        mvc.perform(post("/api/journals/10/approve")
                        .header("X-Auth-User", "Checker-1")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_APPROVER"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/journals/10/post")
                        .header("X-Auth-User", "checker-1")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_APPROVER"))
                .andExpect(status().isForbidden());

        verify(useCase).approveJournalEntry(10L, "checker-1");
    }

    @Test
    void posterCanPostButCannotApprove() throws Exception {
        mvc.perform(post("/api/journals/10/post")
                        .header("X-Auth-User", "Poster-1")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_POSTER"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/journals/10/approve")
                        .header("X-Auth-User", "poster-1")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_POSTER"))
                .andExpect(status().isForbidden());

        verify(useCase).postJournalEntry(10L, "poster-1");
    }

    @Test
    void missingIdentityOrRolesFailsClosed() throws Exception {
        mvc.perform(post("/api/journals/10/approve")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_APPROVER"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/journals/10/approve")
                        .header("X-Auth-User", "checker-1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    void authorizedAccountingAdminReceivesDraftForExplicitAdjustmentDate() throws Exception {
        LocalDate chosenDate = LocalDate.of(2026, 10, 1);
        JournalEntry draft = new JournalEntry();
        draft.setId(884L);
        draft.setSlipDate(chosenDate);
        draft.setAccountingDate(chosenDate);
        draft.setCreatedBy("accounting-admin");
        draft.initializeDraft();
        org.mockito.Mockito.when(useCase.createJournalEntryFromEvent(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(draft));

        mvc.perform(post("/api/journals/from-event")
                        .queryParam("accountingDate", chosenDate.toString())
                        .header("X-Auth-User", "accounting-admin")
                        .header("X-Auth-Roles", "ROLE_ACCOUNTING_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventData\":{\"ruleCode\":\"ADJUSTMENT\",\"sourceEventDate\":\"2026-08-31\",\"createdBy\":\"untrusted\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(884))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> event = ArgumentCaptor.forClass(Map.class);
        verify(useCase).createJournalEntryFromEvent(event.capture(), org.mockito.ArgumentMatchers.eq(chosenDate));
        assertThat(event.getValue()).containsEntry("createdBy", "accounting-admin")
                .containsEntry("auditUser", "accounting-admin")
                .containsEntry("sourceEventDate", "2026-08-31");
    }

    @Test
    void unauthorizedUserCannotChooseAdjustmentDate() throws Exception {
        mvc.perform(post("/api/journals/from-event")
                        .queryParam("accountingDate", "2026-10-01")
                        .header("X-Auth-User", "viewer")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_VIEWER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventData\":{\"ruleCode\":\"ADJUSTMENT\"}}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }
}
