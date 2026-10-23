package com.ho.account.expenditure.payable.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.ho.account.expenditure.application.port.in.PaymentUseCase;
import com.ho.account.expenditure.application.service.PayableOperationConflictException;
import com.ho.account.expenditure.payable.api.dto.OffsetPayableRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class PaymentControllerOffsetIdentityTest {

    @Test
    void offsetRequestWithoutOperationIdIsRejectedBeforeApplyingBalances() throws Exception {
        PaymentUseCase useCase = mock(PaymentUseCase.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            MockMvcBuilders.standaloneSetup(new PaymentController(useCase))
                    .setValidator(validator)
                    .build()
                    .perform(post("/api/payments/offset-payable")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"payableId\":100,\"advancePaymentId\":200,\"offsetAmount\":100.00,\"accountingDate\":\"2026-05-29\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(useCase);
        } finally {
            validator.close();
        }
    }

    @Test
    void offsetOperationIdIsPreservedInCoreCommand() {
        OffsetPayableRequest request = new OffsetPayableRequest();
        request.setPayableId(100L);
        request.setAdvancePaymentId(200L);
        request.setOffsetAmount(new BigDecimal("100.00"));
        request.setOperationId("  offset-17  ");
        request.setAccountingDate(LocalDate.of(2026, 5, 29));

        assertThat(request.toCommand().operationId()).isEqualTo("offset-17");
        assertThat(request.toCommand().accountingDate()).isEqualTo(LocalDate.of(2026, 5, 29));
    }

    @Test
    void journalOrOffsetOperationConflictReturnsHttp409() throws Exception {
        PaymentUseCase useCase = mock(PaymentUseCase.class);
        when(useCase.offsetPayableWithAdvancePayment(any()))
                .thenThrow(new PayableOperationConflictException("conflicting financial content"));
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            MockMvcBuilders.standaloneSetup(new PaymentController(useCase))
                    .setControllerAdvice(new PaymentConflictExceptionHandler())
                    .setValidator(validator)
                    .build()
                    .perform(post("/api/payments/offset-payable")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"payableId\":100,\"advancePaymentId\":200,\"offsetAmount\":100.00,\"operationId\":\"offset-17\",\"accountingDate\":\"2026-05-29\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("OPERATION_CONFLICT"));
        } finally {
            validator.close();
        }
    }
}
