package com.ho.account.expenditure.adapter.out.source;

import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayableSourceDocumentProviderTest {

    @Mock
    private PurchaseInvoicePersistencePort purchaseInvoicePersistencePort;

    @Test
    void supportsPurchaseInvoiceAndLegacyP2pTypes() {
        PayableSourceDocumentProvider provider =
                new PayableSourceDocumentProvider(purchaseInvoicePersistencePort);

        assertTrue(provider.supports("PURCHASE_INVOICE"));
        assertTrue(provider.supports("P2P_AP"));
        assertFalse(provider.supports("SALES_INVOICE"));
    }

    @Test
    void returnsPurchaseInvoiceSourceDocument() {
        PayableSourceDocumentProvider provider =
                new PayableSourceDocumentProvider(purchaseInvoicePersistencePort);
        PurchaseInvoice invoice = createInvoice();
        when(purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode("PI_2026_001", "V001"))
                .thenReturn(Optional.of(invoice));

        Optional<Map<String, Object>> document =
                provider.getSourceDocument("PURCHASE_INVOICE", "PI_2026_001_V001");

        assertTrue(document.isPresent());
        assertEquals("PURCHASE_INVOICE", document.get().get("type"));
        assertEquals("PI_2026_001", document.get().get("invoiceNo"));
        assertEquals("V001", document.get().get("vendorCode"));
        assertEquals(new BigDecimal("1100.00"), document.get().get("totalAmount"));
    }

    @Test
    void returnsEmptyForInvalidSourceId() {
        PayableSourceDocumentProvider provider =
                new PayableSourceDocumentProvider(purchaseInvoicePersistencePort);

        assertTrue(provider.getSourceDocument("PURCHASE_INVOICE", "invalid").isEmpty());
    }

    private PurchaseInvoice createInvoice() {
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setId(1L);
        invoice.setInvoiceNo("PI_2026_001");
        invoice.setVendorCode("V001");
        invoice.setIssueDate(LocalDate.of(2026, 5, 8));
        invoice.setDueDate(LocalDate.of(2026, 6, 7));
        invoice.setNetAmount(new BigDecimal("1000.00"));
        invoice.setTaxAmount(new BigDecimal("100.00"));
        invoice.setTotalAmount(new BigDecimal("1100.00"));
        invoice.setStatus(PurchaseInvoiceStatus.RECEIVED);
        invoice.setDescription("source document test");
        return invoice;
    }
}
