package com.ho.account.asset.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.asset.application.port.in.LeaseUseCase;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseRemeasurementConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LeaseRemeasurementIdentityApiTest {
    LeaseUseCase useCase;
    MockMvc mvc;

    @BeforeEach void setup() {
        useCase = mock(LeaseUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new LeaseAccountingController(useCase)).build();
    }

    @Test void missingOrMalformedOperationIdReturns400BeforeUseCase() throws Exception {
        for (String property : new String[] {"", ",\"operationId\":\"\"", ",\"operationId\":\"has space\"",
                ",\"operationId\":\"" + "x".repeat(81) + "\""}) {
            mvc.perform(post("/api/ifrs16/leases/remeasure").header("X-User-ID", "maker")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"contractId\":1,\"remeasurementDate\":\"2026-06-01\",\"newMonthlyPayment\":2400.00" + property + "}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(useCase);
    }

    @Test void forwardsStableOperationAndReturns409ForChangedReuse() throws Exception {
        when(useCase.remeasureLease(eq(1L), eq(LocalDate.of(2026, 6, 1)), any(BigDecimal.class), isNull(), isNull(), eq("maker"), eq("AMENDMENT-1")))
                .thenReturn(new LeaseContract()).thenThrow(new LeaseRemeasurementConflictException());
        String json = "{\"contractId\":1,\"remeasurementDate\":\"2026-06-01\",\"newMonthlyPayment\":2400.00,\"operationId\":\"AMENDMENT-1\"}";
        mvc.perform(post("/api/ifrs16/leases/remeasure").header("X-User-ID", "maker")
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
        mvc.perform(post("/api/ifrs16/leases/remeasure").header("X-User-ID", "maker")
                .contentType(MediaType.APPLICATION_JSON).content(json.replace("2400.00", "2500.00"))).andExpect(status().isConflict());
        verify(useCase, times(2)).remeasureLease(eq(1L), eq(LocalDate.of(2026, 6, 1)), any(BigDecimal.class), isNull(), isNull(), eq("maker"), eq("AMENDMENT-1"));
    }
}
