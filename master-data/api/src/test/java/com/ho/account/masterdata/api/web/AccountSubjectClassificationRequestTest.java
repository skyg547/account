package com.ho.account.masterdata.api.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;
import com.ho.account.masterdata.core.application.port.in.AccountSubjectUseCase;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AccountSubjectClassificationRequestTest {

    private AccountSubjectUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(AccountSubjectUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountSubjectController(useCase)).build();
    }

    @Test
    void directPutRejectsClassificationFieldsBeforeUseCase() throws Exception {
        // Classification changes require the approval audit; the direct request must fail during JSON binding.
        for (String field : new String[] {
                "\"accountType\":\"ASSET\"",
                "\"regulatoryMappingCode\":\"R-100\"",
                "\"clearRegulatoryMappingCode\":true" }) {
            mockMvc.perform(put("/api/basic/account-subjects/{code}", "1100")
                            .header("X-Auth-Roles", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{" + field + "}"))
                    .andExpect(status().isBadRequest());
        }
        verify(useCase, never()).updateAccountSubject(any(), any());
    }

    @Test
    void nameOnlyPutPreservesOmittedOptionalValues() throws Exception {
        when(useCase.updateAccountSubject(eq("1100"), any(AccountSubjectCommand.class)))
                .thenReturn(mock(AccountSubject.class));

        mockMvc.perform(put("/api/basic/account-subjects/{code}", "1100")
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated name\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AccountSubjectCommand> command = ArgumentCaptor.forClass(AccountSubjectCommand.class);
        verify(useCase).updateAccountSubject(eq("1100"), command.capture());
        assertEquals("Updated name", command.getValue().name());
        assertNull(command.getValue().unsettled());
        assertNull(command.getValue().fixedAsset());
        assertNull(command.getValue().accountType());
        assertNull(command.getValue().regulatoryMappingCode());
        assertFalse(command.getValue().clearRegulatoryMappingCode());
    }
}
