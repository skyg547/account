package com.ho.account.tax.application.service;

import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.tax.application.port.in.TaxInvoiceCommand;
import com.ho.account.tax.application.port.out.TaxInvoicePersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxInvoiceServiceTest {

    @Mock
    private TaxInvoicePersistencePort taxInvoicePersistencePort;

    @Mock
    private MasterDataQueryPort masterDataQueryPort;

    @Test
    void createAPInvoiceUsesCommandAndVerifiesBusinessPartner() {
        TaxInvoiceService service = new TaxInvoiceService(taxInvoicePersistencePort, masterDataQueryPort);
        TaxInvoiceCommand command = purchaseCommand("TX-100");

        when(masterDataQueryPort.findBusinessPartner("BP001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("BP001", "Local partner", "VENDOR", true)));
        when(taxInvoicePersistencePort.save(any(TaxInvoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaxInvoice saved = service.createAPInvoice(command);

        assertEquals("TX-100", saved.getIssueId());
        assertEquals("PURCHASE", saved.getType());
        verify(masterDataQueryPort).findBusinessPartner("BP001");
    }

    @Test
    void createAPInvoiceRejectsSalesTypeBeforePersistence() {
        TaxInvoiceService service = new TaxInvoiceService(taxInvoicePersistencePort, masterDataQueryPort);
        TaxInvoiceCommand command = new TaxInvoiceCommand(
                "TX-SALES",
                "SALES",
                LocalDate.of(2026, 7, 8),
                "BP001",
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1100.00"));

        assertThrows(IllegalArgumentException.class, () -> service.createAPInvoice(command));

        verify(masterDataQueryPort, never()).findBusinessPartner(any());
        verify(taxInvoicePersistencePort, never()).save(any());
    }

    @Test
    void cancelAPInvoicePersistsAuditFields() {
        TaxInvoiceService service = new TaxInvoiceService(taxInvoicePersistencePort, masterDataQueryPort);
        TaxInvoice invoice = TaxInvoice.create(
                "TX-CANCEL",
                "PURCHASE",
                LocalDate.of(2026, 7, 8),
                "BP001",
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1100.00"));
        when(taxInvoicePersistencePort.findById(10L)).thenReturn(Optional.of(invoice));

        service.cancelAPInvoice(10L, "auditor", "wrong supplier");

        ArgumentCaptor<TaxInvoice> captor = ArgumentCaptor.forClass(TaxInvoice.class);
        verify(taxInvoicePersistencePort).save(captor.capture());
        assertEquals(TaxInvoice.TaxInvoiceStatus.CANCELLED, captor.getValue().getStatus());
        assertEquals("auditor", captor.getValue().getCancelledBy());
        assertEquals("wrong supplier", captor.getValue().getCancellationReason());
    }

    private TaxInvoiceCommand purchaseCommand(String issueId) {
        return new TaxInvoiceCommand(
                issueId,
                "PURCHASE",
                LocalDate.of(2026, 7, 8),
                "BP001",
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1100.00"));
    }
}