package com.ho.account.receivable.adapter.out.source;

import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.SalesInvoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceivableSourceDocumentProviderTest {

    @Mock
    private SalesInvoicePersistencePort salesInvoicePersistencePort;

    @Test
    void supportsSalesInvoiceAndReceivableLineageTypes() {
        ReceivableSourceDocumentProvider provider =
                new ReceivableSourceDocumentProvider(salesInvoicePersistencePort);

        assertTrue(provider.supports("SALES_INVOICE"));
        assertTrue(provider.supports("O2C_AR"));
        assertTrue(provider.supports("SALES"));
        assertFalse(provider.supports("PURCHASE_INVOICE"));
    }

    @Test
    void returnsSalesInvoiceSourceDocument() {
        ReceivableSourceDocumentProvider provider =
                new ReceivableSourceDocumentProvider(salesInvoicePersistencePort);
        SalesInvoice invoice = createInvoice();
        when(salesInvoicePersistencePort.findById(10L)).thenReturn(Optional.of(invoice));

        Optional<Map<String, Object>> document =
                provider.getSourceDocument("SALES_INVOICE", "10");

        assertTrue(document.isPresent());
        assertEquals("SALES_INVOICE", document.get().get("type"));
        assertEquals(10L, document.get().get("id"));
        assertEquals("SI-2026-001", document.get().get("invoiceNo"));
        assertEquals("C001", document.get().get("customerCode"));
        assertEquals(new BigDecimal("1100.00"), document.get().get("totalAmount"));
    }

    @Test
    void returnsEmptyForInvalidInvoiceId() {
        ReceivableSourceDocumentProvider provider =
                new ReceivableSourceDocumentProvider(salesInvoicePersistencePort);

        assertTrue(provider.getSourceDocument("SALES_INVOICE", "not-a-number").isEmpty());
    }

    private SalesInvoice createInvoice() {
        SalesInvoice invoice = SalesInvoice.create(
                "SI-2026-001",
                "C001",
                LocalDate.of(2026, 5, 8),
                LocalDate.of(2026, 6, 7),
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                "tester");
        ReflectionTestUtils.setField(invoice, "id", 10L);
        return invoice;
    }
}
