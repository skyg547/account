package com.ho.account.receivable.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.receivable.application.port.in.SalesUseCase;
import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.dto.SalesInvoiceRequest;
import com.ho.account.receivable.dto.SalesInvoiceResponse;
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
        assertThat(useCase.capturedInvoice.getCreatedBy()).isEqualTo("sales-user");
    }

    private static class CapturingSalesUseCase implements SalesUseCase {

        private SalesInvoice capturedInvoice;

        @Override
        public SalesInvoice createSalesInvoice(SalesInvoice invoice) {
            this.capturedInvoice = invoice;
            return invoice;
        }

        @Override
        public void updateReceivableStatus(LocalDate asOfDate) {
        }
    }
}
