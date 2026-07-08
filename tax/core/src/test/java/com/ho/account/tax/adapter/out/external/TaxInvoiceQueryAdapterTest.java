package com.ho.account.tax.adapter.out.external;

import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.repository.TaxInvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxInvoiceQueryAdapterTest {

    @Mock
    private TaxInvoiceRepository taxInvoiceRepository;

    @Test
    void findByIdReturnsUsablePurchaseReferenceForActivePurchaseInvoice() {
        TaxInvoice invoice = purchaseInvoice("TX-ACTIVE");
        ReflectionTestUtils.setField(invoice, "id", 10L);
        when(taxInvoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));

        TaxInvoiceRef ref = new TaxInvoiceQueryAdapter(taxInvoiceRepository)
                .findById(10L)
                .orElseThrow();

        assertTrue(ref.purchase());
        assertTrue(ref.active());
        assertTrue(ref.usableForPurchaseSettlement());
    }

    @Test
    void findByIdReturnsCancelledStatusSoConsumersCanRejectSettlement() {
        TaxInvoice invoice = purchaseInvoice("TX-CANCELLED");
        ReflectionTestUtils.setField(invoice, "id", 11L);
        invoice.cancel("auditor", "wrong supplier");
        when(taxInvoiceRepository.findById(11L)).thenReturn(Optional.of(invoice));

        TaxInvoiceRef ref = new TaxInvoiceQueryAdapter(taxInvoiceRepository)
                .findById(11L)
                .orElseThrow();

        assertTrue(ref.purchase());
        assertFalse(ref.active());
        assertFalse(ref.usableForPurchaseSettlement());
    }

    private TaxInvoice purchaseInvoice(String issueId) {
        return TaxInvoice.create(
                issueId,
                "PURCHASE",
                LocalDate.of(2026, 7, 8),
                "BP001",
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1100.00"));
    }
}