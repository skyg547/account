package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.out.APPaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.APPaymentStatus;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.dto.APPaymentRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class APPaymentServiceTest {

    @Mock
    private APPaymentPersistencePort apPaymentPersistencePort;
    @Mock
    private ExpenditureResolutionPersistencePort resolutionPersistencePort;
    @Mock
    private TaxInvoiceQueryPort taxInvoiceQueryPort;

    @Test
    void createAPPaymentWithPurchaseTaxInvoiceCreatesPendingPayment() {
        APPaymentService service = new APPaymentService(
                apPaymentPersistencePort,
                resolutionPersistencePort,
                taxInvoiceQueryPort);

        APPaymentRequestDto request = new APPaymentRequestDto();
        request.setExpenditureResolutionId(1L);
        request.setTaxInvoiceId(10L);
        request.setPaymentDate(LocalDateTime.of(2026, 4, 29, 9, 0));
        request.setAmount(new BigDecimal("5000.00"));
        request.setPaymentMethod("TRANSFER");

        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(new ExpenditureResolution()));
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "PURCHASE")));
        when(apPaymentPersistencePort.save(any(APPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        APPayment payment = service.createAPPayment(request);

        assertEquals(APPaymentStatus.PENDING, payment.getStatus());
        assertEquals(0, payment.getUnappliedAmount().compareTo(new BigDecimal("5000.00")));
        assertEquals(10L, payment.getTaxInvoiceId());
    }

    @Test
    void createAPPaymentWithSalesTaxInvoiceThrows() {
        APPaymentService service = new APPaymentService(
                apPaymentPersistencePort,
                resolutionPersistencePort,
                taxInvoiceQueryPort);

        APPaymentRequestDto request = new APPaymentRequestDto();
        request.setExpenditureResolutionId(1L);
        request.setTaxInvoiceId(10L);
        request.setPaymentDate(LocalDateTime.of(2026, 4, 29, 9, 0));
        request.setAmount(new BigDecimal("5000.00"));
        request.setPaymentMethod("TRANSFER");

        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(new ExpenditureResolution()));
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "SALES")));

        assertThrows(IllegalArgumentException.class, () -> service.createAPPayment(request));
    }
}
