package com.accounting.system.tax.service;

import com.accounting.system.tax.domain.TaxInvoice;
import com.accounting.system.tax.repository.TaxInvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TaxService {

    private final TaxInvoiceRepository taxInvoiceRepository;

    @Autowired
    public TaxService(TaxInvoiceRepository taxInvoiceRepository) {
        this.taxInvoiceRepository = taxInvoiceRepository;
    }

    // 세금계산서 발행/수취 등록
    public TaxInvoice createTaxInvoice(TaxInvoice taxInvoice) {
        // 합계금액 검증
        if (taxInvoice.getSupplyAmount().add(taxInvoice.getTaxAmount()).compareTo(taxInvoice.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("공급가액과 세액의 합이 합계금액과 일치하지 않습니다.");
        }
        return taxInvoiceRepository.save(taxInvoice);
    }

    // 기간별 조회 (부가세 신고용)
    @Transactional(readOnly = true)
    public List<TaxInvoice> getTaxInvoices(LocalDate startDate, LocalDate endDate) {
        return taxInvoiceRepository.findByIssueDateBetween(startDate, endDate);
    }
}
