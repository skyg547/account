package com.ho.account.journalledger.adapter.in.web.period;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.contracts.journal.*;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JournalPeriodControllerTest {
    // Synthetic test-only credential, never loaded from runtime configuration.
    private static final String TOKEN = "synthetic-journal-test-credential-762";
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private JournalPeriodFencePort port;
    private MockMvc mvc;
    private final JournalPeriodScope scope = new JournalPeriodScope(762L,
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    private final JournalPeriodFenceCommand command = new JournalPeriodFenceCommand("close-762",
            new JournalPeriodCheckpoint(scope, 7), "closer");

    @BeforeEach
    void setUp() {
        port = mock(JournalPeriodFencePort.class);
        mvc = MockMvcBuilders.standaloneSetup(new JournalPeriodController(port, TOKEN)).build();
    }

    @Test
    void authenticatedControlsReturnExactProtocolRecords() throws Exception {
        when(port.checkpoint(scope)).thenReturn(command.checkpoint());
        when(port.fence(command)).thenReturn(new JournalPeriodFenceResult(command, JournalPeriodFenceResult.State.ACTIVE));
        when(port.release(command)).thenReturn(new JournalPeriodFenceResult(command, JournalPeriodFenceResult.State.RELEASED));
        mvc.perform(authorized("checkpoint", scope)).andExpect(status().isOk())
                .andExpect(jsonPath("revision").value(7)).andExpect(jsonPath("scope.fiscalPeriodId").value(762));
        mvc.perform(authorized("fence", command)).andExpect(status().isOk())
                .andExpect(jsonPath("state").value("ACTIVE")).andExpect(jsonPath("command.operationId").value("close-762"));
        mvc.perform(authorized("release", command)).andExpect(status().isOk())
                .andExpect(jsonPath("state").value("RELEASED"));
        verify(port).checkpoint(scope);
        verify(port).fence(command);
        verify(port).release(command);
    }

    @ParameterizedTest
    @ValueSource(strings = {"checkpoint", "fence", "release"})
    void identityHeaderAloneWrongCredentialDuplicatesAndUserHeadersNeverAuthorize(String operation) throws Exception {
        Object body = operation.equals("checkpoint") ? scope : command;
        mvc.perform(request(operation, body).header("X-Service-Identity", "closing"))
                .andExpect(status().isForbidden());
        mvc.perform(request(operation, body).header("X-Service-Identity", "closing")
                        .header("Authorization", "Bearer incorrect"))
                .andExpect(status().isUnauthorized());
        mvc.perform(authorized(operation, body).header("X-Service-Identity", "closing"))
                .andExpect(status().isForbidden());
        mvc.perform(authorized(operation, body).header("Authorization", "Bearer duplicate"))
                .andExpect(status().isForbidden());
        mvc.perform(authorized(operation, body).header("X-Auth-User", "admin"))
                .andExpect(status().isForbidden());
        mvc.perform(authorized(operation, body).header("X-User-ID", "admin"))
                .andExpect(status().isForbidden());
        mvc.perform(request(operation, body).header("X-Service-Identity", "another-service")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());
        verifyNoInteractions(port);
    }

    @Test
    void missingOrShortConfigurationFailsClosed() throws Exception {
        for (String value : new String[] {"", "short"}) {
            MockMvc unavailable = MockMvcBuilders.standaloneSetup(new JournalPeriodController(port, value)).build();
            unavailable.perform(authorized("checkpoint", scope)).andExpect(status().isServiceUnavailable());
        }
        verifyNoInteractions(port);
    }

    @Test
    void malformedAndOversizeCommandIsRejectedBeforePersistence() throws Exception {
        String valid = json.writeValueAsString(command);
        for (String body : new String[] {
                valid.replace("close-762", "x".repeat(101)),
                valid.replace("closer", "x".repeat(51)),
                valid.replace("\"revision\":7", "\"revision\":-1"),
                "{\"operationId\":\"test\",\"checkpoint\":null,\"actor\":\"closer\"}"}) {
            mvc.perform(post("/api/internal/journal-periods/fence").contentType("application/json").content(body)
                            .header("Authorization", "Bearer " + TOKEN).header("X-Service-Identity", "closing"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(port);
    }

    @Test
    void conflictAndDatabaseFailureExposeConstantBoundedErrors() throws Exception {
        when(port.fence(any())).thenThrow(new IllegalStateException("synthetic internal detail"));
        mvc.perform(authorized("fence", command)).andExpect(status().isConflict())
                .andExpect(content().string("Journal period control conflict"));
        when(port.release(any())).thenThrow(new DataAccessResourceFailureException("synthetic SQL detail"));
        mvc.perform(authorized("release", command)).andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Journal period control unavailable"));
        when(port.checkpoint(any())).thenThrow(new org.springframework.transaction.CannotCreateTransactionException(
                "synthetic connection detail"));
        mvc.perform(authorized("checkpoint", scope)).andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Journal period control unavailable"));
    }

    private MockHttpServletRequestBuilder authorized(String operation, Object body) throws Exception {
        return request(operation, body).header("Authorization", "Bearer " + TOKEN).header("X-Service-Identity", "closing");
    }

    private MockHttpServletRequestBuilder request(String operation, Object body) throws Exception {
        return post("/api/internal/journal-periods/" + operation).contentType("application/json")
                .content(json.writeValueAsString(body));
    }
}
