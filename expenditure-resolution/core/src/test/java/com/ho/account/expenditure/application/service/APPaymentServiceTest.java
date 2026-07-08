package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.in.APPaymentCommand;
import com.ho.account.expenditure.application.port.out.APPaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.APPaymentStatus;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        APPaymentCommand command = command();

        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(createResolution()));
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "PURCHASE", "ACTIVE")));
        when(apPaymentPersistencePort.save(any(APPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        APPayment payment = service.createAPPayment(command);

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

        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(createResolution()));
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "SALES", "ACTIVE")));

        assertThrows(IllegalArgumentException.class, () -> service.createAPPayment(command()));
    }

    @Test
    void createAPPaymentWithCancelledTaxInvoiceThrows() {
        APPaymentService service = new APPaymentService(
                apPaymentPersistencePort,
                resolutionPersistencePort,
                taxInvoiceQueryPort);

        when(resolutionPersistencePort.findById(1L)).thenReturn(Optional.of(createResolution()));
        when(taxInvoiceQueryPort.findById(10L)).thenReturn(Optional.of(new TaxInvoiceRef(10L, "TX-10", "PURCHASE", "CANCELLED")));

        assertThrows(IllegalArgumentException.class, () -> service.createAPPayment(command()));
    }

    private APPaymentCommand command() {
        return new APPaymentCommand(
                1L,
                10L,
                LocalDateTime.of(2026, 4, 29, 9, 0),
                new BigDecimal("5000.00"),
                "TRANSFER");
    }

    private ExpenditureResolution createResolution() {
        return ExpenditureResolution.create(
                "REQ-20260429-001",
                "테스트 지출결의",
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 4, 30),
                "D001",
                "PAY001",
                "TEST");
    }
}