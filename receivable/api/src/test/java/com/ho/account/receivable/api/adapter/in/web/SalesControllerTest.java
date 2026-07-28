package com.ho.account.receivable.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.receivable.api.dto.SalesInvoiceRequest;
import com.ho.account.receivable.api.dto.SalesInvoiceResponse;
import com.ho.account.receivable.application.port.in.SalesInvoiceCommand;
import com.ho.account.receivable.application.port.in.SalesUseCase;
import com.ho.account.receivable.domain.SalesInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class SalesControllerTest {

    @Test
    void createSalesInvoice_acceptsRequestDtoAndReturnsResponseDto() {
        CapturingSalesUseCase useCase = new CapturingSalesUseCase();
        SalesController controller = new SalesController(useCase);
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

        ResponseEntity<SalesInvoiceResponse> response = controller.createSalesInvoice(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getInvoiceNo()).isEqualTo("INV-001");
        assertThat(response.getBody().getCustomerCode()).isEqualTo("CUST-001");
        assertThat(response.getBody().getDescription()).isEqualTo("May invoice");
        assertThat(useCase.capturedCommand.createdBy()).isEqualTo("sales-user");
        assertThat(useCase.capturedCommand.totalAmount()).isEqualByComparingTo("1100.00");
    }

    private static class CapturingSalesUseCase implements SalesUseCase {

        private SalesInvoiceCommand capturedCommand;

        @Override
        public SalesInvoice createSalesInvoice(SalesInvoiceCommand command) {
            this.capturedCommand = command;
            return SalesInvoice.create(
                    command.invoiceNo(),
                    command.customerCode(),
                    command.issueDate(),
                    command.dueDate(),
                    command.netAmount(),
                    command.taxAmount(),
                    command.createdBy(),
                    command.description());
        }

        @Override
        public void updateReceivableStatus(LocalDate asOfDate) {
        }
    }
}