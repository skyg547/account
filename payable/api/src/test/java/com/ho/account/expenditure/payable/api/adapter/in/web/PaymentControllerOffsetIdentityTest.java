package com.ho.account.expenditure.payable.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.expenditure.application.port.in.PaymentUseCase;
import com.ho.account.expenditure.payable.api.dto.OffsetPayableRequest;
import java.math.BigDecimal;
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
                            .content("{\"payableId\":100,\"advancePaymentId\":200,\"offsetAmount\":100.00}"))
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

        assertThat(request.toCommand().operationId()).isEqualTo("offset-17");
    }
}
