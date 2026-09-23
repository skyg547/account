package com.ho.account.receivable.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.receivable.api.dto.SalesInvoiceRequest;
import com.ho.account.receivable.application.port.in.SalesInvoiceCommand;
import com.ho.account.receivable.application.port.in.SalesUseCase;
import com.ho.account.receivable.application.port.in.SalesUseCase.InvalidSalesInvoiceStatusException;
import com.ho.account.receivable.application.port.in.SalesUseCase.SalesInvoiceNotFoundException;
import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.domain.SalesInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SalesControllerTest {

    @Mock
    private SalesUseCase salesUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SalesController(salesUseCase))
                .setControllerAdvice(new SalesQueryExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void createSalesInvoice_acceptsRequestDtoAndReturnsResponseDto() throws Exception {
        SalesInvoiceRequest request = createRequest();
        when(salesUseCase.createSalesInvoice(org.mockito.ArgumentMatchers.any(SalesInvoiceCommand.class)))
                .thenReturn(createInvoice(10L, SalesInvoiceStatus.ISSUED));

        mockMvc.perform(post("/api/sales/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNo").value("INV-001"))
                .andExpect(jsonPath("$.customerCode").value("CUST-001"))
                .andExpect(jsonPath("$.description").value("May invoice"));

        ArgumentCaptor<SalesInvoiceCommand> command = ArgumentCaptor.forClass(SalesInvoiceCommand.class);
        verify(salesUseCase).createSalesInvoice(command.capture());
        assertThat(command.getValue().createdBy()).isEqualTo("sales-user");
        assertThat(command.getValue().totalAmount()).isEqualByComparingTo("1100.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/sales/invoices", "/api/receivable/invoices"})
    void findInvoices_isAvailableThroughBothAliases(String path) throws Exception {
        when(salesUseCase.findInvoices(null)).thenReturn(List.of(createInvoice(10L, SalesInvoiceStatus.ISSUED)));

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("ISSUED"));

        verify(salesUseCase).findInvoices(null);
    }

    @Test
    void findInvoices_forwardsOptionalStatusFilter() throws Exception {
        when(salesUseCase.findInvoices("paid")).thenReturn(List.of(createInvoice(11L, SalesInvoiceStatus.PAID)));

        mockMvc.perform(get("/api/receivable/invoices").param("status", "paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(salesUseCase).findInvoices("paid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/sales/invoices/12", "/api/receivable/invoices/12"})
    void findInvoiceById_isAvailableThroughBothAliases(String path) throws Exception {
        when(salesUseCase.findInvoiceById(12L)).thenReturn(createInvoice(12L, SalesInvoiceStatus.PARTIAL_PAID));

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.invoiceNo").value("INV-001"))
                .andExpect(jsonPath("$.status").value("PARTIAL_PAID"));

        verify(salesUseCase).findInvoiceById(12L);
    }

    @Test
    void findInvoices_returnsBadRequestForInvalidStatus() throws Exception {
        when(salesUseCase.findInvoices("unknown"))
                .thenThrow(new InvalidSalesInvoiceStatusException("unknown", new IllegalArgumentException()));

        mockMvc.perform(get("/api/receivable/invoices").param("status", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findInvoiceById_returnsNotFoundForMissingInvoice() throws Exception {
        when(salesUseCase.findInvoiceById(404L))
                .thenThrow(new SalesInvoiceNotFoundException(404L));

        mockMvc.perform(get("/api/receivable/invoices/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void queryAdviceDoesNotReclassifyUnrelatedCreateException() {
        SalesInvoiceRequest request = createRequest();
        when(salesUseCase.createSalesInvoice(org.mockito.ArgumentMatchers.any(SalesInvoiceCommand.class)))
                .thenThrow(new IllegalArgumentException("create business validation failed"));

        assertThatThrownBy(() -> mockMvc.perform(post("/api/sales/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("create business validation failed");
    }

    private SalesInvoiceRequest createRequest() {
        SalesInvoiceRequest request = new SalesInvoiceRequest();
        request.setInvoiceNo("INV-001");
        request.setCustomerCode("CUST-001");
        request.setIssueDate(LocalDate.of(2026, 5, 1));
        request.setDueDate(LocalDate.of(2026, 5, 31));
        request.setNetAmount(new BigDecimal("1000.00"));
        request.setTaxAmount(new BigDecimal("100.00"));
        request.setTotalAmount(new BigDecimal("1100.00"));
        request.setDescription("May invoice");
        request.setCreatedBy("sales-user");
        return request;
    }

    private SalesInvoice createInvoice(Long id, SalesInvoiceStatus status) {
        SalesInvoice invoice = SalesInvoice.create(
                "INV-001",
                "CUST-001",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                "sales-user",
                "May invoice");
        invoice.setId(id);
        invoice.setStatus(status);
        return invoice;
    }
}
