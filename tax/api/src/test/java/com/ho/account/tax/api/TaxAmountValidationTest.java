package com.ho.account.tax.api;

import com.ho.account.tax.api.adapter.in.web.APInvoiceController;
import com.ho.account.tax.application.port.in.TaxInvoiceCommand;
import com.ho.account.tax.application.port.in.TaxInvoiceUseCase;
import com.ho.account.tax.domain.TaxInvoice;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaxAmountValidationTest {

    private TaxInvoiceUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(TaxInvoiceUseCase.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new APInvoiceController(useCase))
                .setValidator(validator)
                .build();
    }

    @Test
    void postRejectsRoundingAndPrecisionOverflowBeforeUseCase() throws Exception {
        mvc.perform(post("/api/ap/invoices").contentType(MediaType.APPLICATION_JSON)
                        .content(json("1.005", "0", "1.005")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/ap/invoices").contentType(MediaType.APPLICATION_JSON)
                        .content(json("100000000000000000", "0", "100000000000000000")))
                .andExpect(status().isBadRequest());
        verify(useCase, never()).createAPInvoice(any());
    }

    @Test
    void putRejectsIndependentRoundingBeforeUseCase() throws Exception {
        mvc.perform(put("/api/ap/invoices/7").contentType(MediaType.APPLICATION_JSON)
                        .content(json("1.005", "0.005", "1.010")))
                .andExpect(status().isBadRequest());
        verify(useCase, never()).updateAPInvoice(any(), any());
    }

    @Test
    void validAmountsReachUseCaseForCreateAndUpdate() throws Exception {
        when(useCase.createAPInvoice(any())).thenAnswer(invocation -> invoice(invocation.getArgument(0)));
        when(useCase.updateAPInvoice(any(), any())).thenAnswer(invocation -> invoice(invocation.getArgument(1)));

        mvc.perform(post("/api/ap/invoices").contentType(MediaType.APPLICATION_JSON)
                        .content(json("1.00", "0.10", "1.10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1.10));
        mvc.perform(put("/api/ap/invoices/7").contentType(MediaType.APPLICATION_JSON)
                        .content(json("1.000", "0.100", "1.100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1.100));
    }

    private TaxInvoice invoice(TaxInvoiceCommand command) {
        return TaxInvoice.create(command.issueId(), command.type(), command.issueDate(),
                command.businessPartnerCode(), command.supplyAmount(), command.taxAmount(), command.totalAmount());
    }

    private String json(String supply, String tax, String total) {
        return """
                {"issueId":"TX-VALID","type":"PURCHASE","issueDate":"2026-05-08",
                 "businessPartnerCode":"BP001","supplyAmount":%s,"taxAmount":%s,"totalAmount":%s}
                """.formatted(supply, tax, total);
    }
}
