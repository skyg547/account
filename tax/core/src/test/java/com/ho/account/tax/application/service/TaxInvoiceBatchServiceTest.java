package com.ho.account.tax.application.service;

import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.tax.application.port.in.TaxInvoiceBatchUseCase.TaxInvoiceValidationResult;
import com.ho.account.tax.application.port.out.TaxInvoicePersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxInvoiceBatchServiceTest {

    @Mock
    private TaxInvoicePersistencePort taxInvoicePersistencePort;

    @Mock
    private MasterDataQueryPort masterDataQueryPort;

    @Test
    @DisplayName("매입 세금계산서 일괄 검증 시 벌크 쿼리(findAllByPartnerCodes) 1회만 호출하여 N+1 문제를 방지한다")
    void validatePurchaseInvoicesUsesBulkQueryToPreventNPlusOne() {
        TaxInvoiceBatchService service = new TaxInvoiceBatchService(taxInvoicePersistencePort, masterDataQueryPort);

        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);

        TaxInvoice invoice1 = createPurchaseInvoice("TX-001", "BP001", "1000.00", "100.00", "1100.00");
        TaxInvoice invoice2 = createPurchaseInvoice("TX-002", "BP002", "2000.00", "200.00", "2200.00");
        TaxInvoice invoice3 = createPurchaseInvoice("TX-003", "BP001", "3000.00", "300.00", "3300.00");
        TaxInvoice salesInvoice = createSalesInvoice("TX-004", "BP003", "5000.00", "500.00", "5500.00");

        List<TaxInvoice> invoices = List.of(invoice1, invoice2, invoice3, salesInvoice);
        Pageable pageable = PageRequest.of(0, 500);
        when(taxInvoicePersistencePort.findByIssueDateBetween(startDate, endDate, pageable))
                .thenReturn(new PageImpl<>(invoices, pageable, invoices.size()));

        Set<String> expectedPartnerCodes = Set.of("BP001", "BP002");
        when(masterDataQueryPort.findAllByPartnerCodes(expectedPartnerCodes))
                .thenReturn(Map.of(
                        "BP001", new BusinessPartnerRef("BP001", "Partner 1", "VENDOR", true),
                        "BP002", new BusinessPartnerRef("BP002", "Partner 2", "VENDOR", true)
                ));

        TaxInvoiceValidationResult result = service.validatePurchaseInvoices(startDate, endDate);

        assertEquals(4, result.scannedCount());
        assertEquals(3, result.validatedCount());

        // N+1 문제 검증: 단건 조회가 아닌 벌크 쿼리가 정확히 1회 호출되었는지 확인
        verify(masterDataQueryPort).findAllByPartnerCodes(expectedPartnerCodes);
        verify(masterDataQueryPort, never()).findBusinessPartner(any());
    }

    @Test
    @DisplayName("대량 세금계산서 데이터를 여러 페이지로 분할(Paging/Chunking)하여 OOM을 예방하며 정상 검증한다")
    void validatePurchaseInvoicesProcessesInPagesToPreventOOM() {
        TaxInvoiceBatchService service = new TaxInvoiceBatchService(taxInvoicePersistencePort, masterDataQueryPort);

        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);
        int pageSize = 3;

        TaxInvoice invoice1 = createPurchaseInvoice("TX-001", "BP001", "1000.00", "100.00", "1100.00");
        TaxInvoice invoice2 = createPurchaseInvoice("TX-002", "BP002", "2000.00", "200.00", "2200.00");
        TaxInvoice salesInvoice = createSalesInvoice("TX-003", "BP003", "5000.00", "500.00", "5500.00");

        TaxInvoice invoice3 = createPurchaseInvoice("TX-004", "BP001", "3000.00", "300.00", "3300.00");
        TaxInvoice invoice4 = createPurchaseInvoice("TX-005", "BP004", "4000.00", "400.00", "4400.00");

        Pageable pageable0 = PageRequest.of(0, pageSize);
        Pageable pageable1 = PageRequest.of(1, pageSize);

        when(taxInvoicePersistencePort.findByIssueDateBetween(startDate, endDate, pageable0))
                .thenReturn(new PageImpl<>(List.of(invoice1, invoice2, salesInvoice), pageable0, 5));
        when(taxInvoicePersistencePort.findByIssueDateBetween(startDate, endDate, pageable1))
                .thenReturn(new PageImpl<>(List.of(invoice3, invoice4), pageable1, 5));

        when(masterDataQueryPort.findAllByPartnerCodes(Set.of("BP001", "BP002")))
                .thenReturn(Map.of(
                        "BP001", new BusinessPartnerRef("BP001", "Partner 1", "VENDOR", true),
                        "BP002", new BusinessPartnerRef("BP002", "Partner 2", "VENDOR", true)
                ));
        when(masterDataQueryPort.findAllByPartnerCodes(Set.of("BP001", "BP004")))
                .thenReturn(Map.of(
                        "BP001", new BusinessPartnerRef("BP001", "Partner 1", "VENDOR", true),
                        "BP004", new BusinessPartnerRef("BP004", "Partner 4", "VENDOR", true)
                ));

        TaxInvoiceValidationResult result = service.validatePurchaseInvoices(startDate, endDate, pageSize);

        assertEquals(5, result.scannedCount());
        assertEquals(4, result.validatedCount());

        verify(taxInvoicePersistencePort).findByIssueDateBetween(startDate, endDate, pageable0);
        verify(taxInvoicePersistencePort).findByIssueDateBetween(startDate, endDate, pageable1);
        verify(masterDataQueryPort, times(2)).findAllByPartnerCodes(any());
    }

    @Test
    @DisplayName("존재하지 않는 거래처의 매입 세금계산서 포함 시 IllegalStateException 예외가 발생한다")
    void validatePurchaseInvoicesThrowsExceptionWhenPartnerMissing() {
        TaxInvoiceBatchService service = new TaxInvoiceBatchService(taxInvoicePersistencePort, masterDataQueryPort);

        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);

        TaxInvoice invoice1 = createPurchaseInvoice("TX-001", "BP001", "1000.00", "100.00", "1100.00");
        TaxInvoice invoice2 = createPurchaseInvoice("TX-002", "BP-MISSING", "2000.00", "200.00", "2200.00");

        List<TaxInvoice> invoices = List.of(invoice1, invoice2);
        Pageable pageable = PageRequest.of(0, 500);
        when(taxInvoicePersistencePort.findByIssueDateBetween(startDate, endDate, pageable))
                .thenReturn(new PageImpl<>(invoices, pageable, invoices.size()));

        when(masterDataQueryPort.findAllByPartnerCodes(Set.of("BP001", "BP-MISSING")))
                .thenReturn(Map.of(
                        "BP001", new BusinessPartnerRef("BP001", "Partner 1", "VENDOR", true)
                ));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.validatePurchaseInvoices(startDate, endDate));

        assertEquals("Business partner missing for tax invoice TX-002", ex.getMessage());
    }

    private TaxInvoice createPurchaseInvoice(String issueId, String partnerCode, String supplyAmt, String vatAmt, String totalAmt) {
        return TaxInvoice.create(
                issueId,
                "PURCHASE",
                LocalDate.of(2026, 8, 10),
                partnerCode,
                new BigDecimal(supplyAmt),
                new BigDecimal(vatAmt),
                new BigDecimal(totalAmt)
        );
    }

    private TaxInvoice createSalesInvoice(String issueId, String partnerCode, String supplyAmt, String vatAmt, String totalAmt) {
        return TaxInvoice.create(
                issueId,
                "SALES",
                LocalDate.of(2026, 8, 10),
                partnerCode,
                new BigDecimal(supplyAmt),
                new BigDecimal(vatAmt),
                new BigDecimal(totalAmt)
        );
    }
}

