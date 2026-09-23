package com.ho.account.expenditure.payable.api.adapter.in.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.expenditure.application.port.in.PurchaseUseCase;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PurchaseControllerTest {

    private PurchaseUseCase purchaseUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        purchaseUseCase = mock(PurchaseUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PurchaseController(purchaseUseCase))
                .setControllerAdvice(new PurchaseQueryExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modules(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/purchase", "/api/payable"})
    void getPurchaseInvoicesSupportsBothRoutesAndPassesStatus(String basePath) throws Exception {
        when(purchaseUseCase.findInvoices("OVERDUE"))
                .thenReturn(List.of(invoice(71L, PurchaseInvoiceStatus.OVERDUE)));

        mockMvc.perform(get(basePath + "/invoices").param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(71))
                .andExpect(jsonPath("$[0].invoiceNo").value("PI-2026-071"))
                .andExpect(jsonPath("$[0].vendorCode").value("VENDOR-071"))
                .andExpect(jsonPath("$[0].issueDate").value("2026-09-01"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-09-30"))
                .andExpect(jsonPath("$[0].netAmount").value(1000.00))
                .andExpect(jsonPath("$[0].taxAmount").value(100.00))
                .andExpect(jsonPath("$[0].totalAmount").value(1100.00))
                .andExpect(jsonPath("$[0].status").value("OVERDUE"))
                .andExpect(jsonPath("$[0].journalEntryId").value(9071))
                .andExpect(jsonPath("$[0].description").value("Office supplies"))
                .andExpect(jsonPath("$[0].createdBy").value("buyer-user"));

        verify(purchaseUseCase).findInvoices("OVERDUE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/purchase", "/api/payable"})
    void getPurchaseInvoiceByIdSupportsBothRoutesAndReturnsDto(String basePath) throws Exception {
        when(purchaseUseCase.findInvoiceById(71L))
                .thenReturn(invoice(71L, PurchaseInvoiceStatus.RECEIVED));

        mockMvc.perform(get(basePath + "/invoices/{id}", 71L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(71))
                .andExpect(jsonPath("$.invoiceNo").value("PI-2026-071"))
                .andExpect(jsonPath("$.vendorCode").value("VENDOR-071"))
                .andExpect(jsonPath("$.issueDate").value("2026-09-01"))
                .andExpect(jsonPath("$.dueDate").value("2026-09-30"))
                .andExpect(jsonPath("$.netAmount").value(1000.00))
                .andExpect(jsonPath("$.taxAmount").value(100.00))
                .andExpect(jsonPath("$.totalAmount").value(1100.00))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.journalEntryId").value(9071))
                .andExpect(jsonPath("$.description").value("Office supplies"))
                .andExpect(jsonPath("$.createdBy").value("buyer-user"));

        verify(purchaseUseCase).findInvoiceById(71L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/purchase", "/api/payable"})
    void getPurchaseInvoicesReturnsBadRequestForInvalidStatus(String basePath) throws Exception {
        when(purchaseUseCase.findInvoices("unknown"))
                .thenThrow(new IllegalArgumentException("Invalid purchase invoice status: unknown"));

        mockMvc.perform(get(basePath + "/invoices").param("status", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid purchase invoice status: unknown"));

        verify(purchaseUseCase).findInvoices("unknown");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/purchase", "/api/payable"})
    void getPurchaseInvoiceByIdReturnsNotFoundWhenInvoiceDoesNotExist(String basePath) throws Exception {
        when(purchaseUseCase.findInvoiceById(404L))
                .thenThrow(new EntityNotFoundException("PurchaseInvoice not found with id: 404"));

        mockMvc.perform(get(basePath + "/invoices/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("PurchaseInvoice not found with id: 404"));

        verify(purchaseUseCase).findInvoiceById(404L);
    }

    private PurchaseInvoice invoice(Long id, PurchaseInvoiceStatus status) {
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setId(id);
        invoice.setInvoiceNo("PI-2026-071");
        invoice.setVendorCode("VENDOR-071");
        invoice.setIssueDate(LocalDate.of(2026, 9, 1));
        invoice.setDueDate(LocalDate.of(2026, 9, 30));
        invoice.setNetAmount(new BigDecimal("1000.00"));
        invoice.setTaxAmount(new BigDecimal("100.00"));
        invoice.setTotalAmount(new BigDecimal("1100.00"));
        invoice.setStatus(status);
        invoice.setJournalEntryId(9071L);
        invoice.setDescription("Office supplies");
        invoice.setCreatedBy("buyer-user");
        return invoice;
    }
}
