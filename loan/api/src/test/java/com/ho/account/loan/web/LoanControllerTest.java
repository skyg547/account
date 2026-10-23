package com.ho.account.loan.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.loan.application.port.in.LoanUseCase;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.dto.LoanEventRequestDto;
import com.ho.account.loan.dto.LoanEventResultDto;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanControllerTest {

    @Test
    void rejectsUnsupportedPaymentFrequenciesAtHttpContractBoundary() throws Exception {
        LoanUseCase useCase = mock(LoanUseCase.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new LoanController(useCase))
                .setControllerAdvice(new LoanApiExceptionHandler())
                .build();
        for (Loan.PaymentFrequency frequency : Loan.PaymentFrequency.values()) {
            if (frequency == Loan.PaymentFrequency.MONTHLY) {
                continue;
            }
            String request = """
                    {"loanNumber":"LN-API-UNSUPPORTED","businessPartnerId":100,
                     "currencyCode":"KRW","loanType":"TERM_LOAN","principalAmount":1200.00,
                     "interestRate":0.1200,"disbursalDate":"2026-01-01",
                     "maturityDate":"2026-04-01","paymentFrequency":"%s"}
                    """.formatted(frequency);
            mvc.perform(post("/api/loan/loans").contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                            "paymentFrequency " + frequency)));
        }
        verifyNoInteractions(useCase);
    }

    @Test
    void defaultEventReturnsEventWithoutArtificialRecalculation() {
        LoanUseCase useCase = mock(LoanUseCase.class);
        Loan loan = Loan.create(
                "LN-API-1", 100L, "KRW", Loan.LoanType.TERM_LOAN,
                new BigDecimal("1000.00"), new BigDecimal("0.0450"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1),
                Loan.PaymentFrequency.MONTHLY, "risk-user");
        loan.setId(7L);
        loan.activateAfterDisbursal(
                loan.getDisbursalDate(), loan.getPrincipalAmount(), "risk-user");
        LoanEvent event = LoanEvent.record(
                loan, LoanEvent.EventType.DEFAULT, LocalDate.of(2026, 6, 1),
                "Borrower defaulted", null, "risk-user");
        when(useCase.processLoanEvent(
                eq(7L), eq(LoanEvent.EventType.DEFAULT), eq(LocalDate.of(2026, 6, 1)),
                eq("Borrower defaulted"), eq("risk-user"),
                eq(Optional.empty()), eq(Optional.empty())))
                .thenReturn(new LoanUseCase.LoanEventResult(event, Optional.empty()));

        LoanEventRequestDto request = new LoanEventRequestDto();
        request.setLoanId(7L);
        request.setEventType(LoanEvent.EventType.DEFAULT);
        request.setEventDate(LocalDate.of(2026, 6, 1));
        request.setDescription("Borrower defaulted");
        request.setUser("risk-user");

        ResponseEntity<LoanEventResultDto> response =
                new LoanController(useCase).processLoanEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().event().getEventType()).isEqualTo(LoanEvent.EventType.DEFAULT);
        assertThat(response.getBody().recalculationRun()).isNull();
        verify(useCase).processLoanEvent(
                7L, LoanEvent.EventType.DEFAULT, LocalDate.of(2026, 6, 1),
                "Borrower defaulted", "risk-user", Optional.empty(), Optional.empty());
    }

    @Test
    void exceptionHandlerMapsDomainFailuresToStableStatuses() {
        LoanApiExceptionHandler handler = new LoanApiExceptionHandler();

        assertThat(handler.handleNotFound(new EntityNotFoundException("missing")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleBadRequest(new IllegalArgumentException("bad input")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleConflict(new IllegalStateException("invalid state")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
