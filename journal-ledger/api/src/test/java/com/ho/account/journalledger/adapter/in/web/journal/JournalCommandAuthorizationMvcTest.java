package com.ho.account.journalledger.adapter.in.web.journal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
