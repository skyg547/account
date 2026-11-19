package com.ho.account.receivable.api.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.receivable.application.port.in.CollectionUseCase;
import com.ho.account.receivable.application.port.in.ManualMatchingCommand;
import com.ho.account.receivable.application.service.ReceivableOperationConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReceivableOperationConflictHandlerTest {

    @Test
    void mapsChangedOperationKeyToHttpConflict() throws Exception {
        CollectionUseCase useCase = org.mockito.Mockito.mock(CollectionUseCase.class);
        doThrow(new ReceivableOperationConflictException("Journal operation key conflicts with different financial content"))
                .when(useCase).manualMatchCollection(any(ManualMatchingCommand.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CollectionController(useCase))
                .setControllerAdvice(new ReceivableOperationConflictHandler())
                .build();

        mockMvc.perform(post("/api/collections/manual-match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"collectionId":20,"receivableId":30,"matchingAmount":100.00,"operationId":"same-key"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Journal operation key conflicts with different financial content"));
    }

    @Test
    void mapsReusedManualOperationWithDifferentReceivableToHttpConflict() throws Exception {
        CollectionUseCase useCase = org.mockito.Mockito.mock(CollectionUseCase.class);
        doThrow(new ReceivableOperationConflictException("Matching operation ID conflicts with a prior allocation: same-key"))
                .when(useCase).manualMatchCollection(any(ManualMatchingCommand.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CollectionController(useCase))
                .setControllerAdvice(new ReceivableOperationConflictHandler())
                .build();

        mockMvc.perform(post("/api/collections/manual-match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"collectionId":20,"receivableId":31,"matchingAmount":100.00,"operationId":"same-key"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Matching operation ID conflicts with a prior allocation: same-key"));
    }
}
