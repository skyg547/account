package com.ho.account.tax.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.dto.TaxInvoiceRequestDto;
import com.ho.account.tax.repository.TaxInvoiceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class APInvoiceService {

    private final TaxInvoiceRepository taxInvoiceRepository;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public APInvoiceService(TaxInvoiceRepository taxInvoiceRepository,
                            BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.taxInvoiceRepository = taxInvoiceRepository;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    public TaxInvoice createAPInvoice(TaxInvoiceRequestDto requestDto) {
        if (!"PURCHASE".equals(requestDto.getType())) {
            throw new IllegalArgumentException("AP Invoice는 PURCHASE 타입만 생성할 수 있습니다.");
        }

        BusinessPartner businessPartner = businessPartnerPersistencePort.findByBusinessPartnerCode(
                        requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "거래처를 찾을 수 없습니다. 코드: " + requestDto.getBusinessPartnerCode()));

        TaxInvoice taxInvoice = new TaxInvoice();
        taxInvoice.setIssueId(requestDto.getIssueId());
        taxInvoice.setType(requestDto.getType());
        taxInvoice.setIssueDate(requestDto.getIssueDate());
        taxInvoice.setBusinessPartner(businessPartner);
        taxInvoice.setSupplyAmount(requestDto.getSupplyAmount());
        taxInvoice.setTaxAmount(requestDto.getTaxAmount());
        taxInvoice.setTotalAmount(requestDto.getTotalAmount());

        validateAmount(taxInvoice);
        return taxInvoiceRepository.save(taxInvoice);
    }

    @Transactional(readOnly = true)
    public Optional<TaxInvoice> getAPInvoiceById(Long id) {
        return taxInvoiceRepository.findById(id)
                .filter(ti -> "PURCHASE".equals(ti.getType()));
    }

    @Transactional(readOnly = true)
    public Optional<TaxInvoice> getAPInvoiceByIssueId(String issueId) {
        return taxInvoiceRepository.findByIssueId(issueId)
                .filter(ti -> "PURCHASE".equals(ti.getType()));
    }

    @Transactional(readOnly = true)
    public List<TaxInvoice> getAPInvoicesBetweenDates(LocalDate startDate, LocalDate endDate) {
        return taxInvoiceRepository.findByIssueDateBetween(startDate, endDate).stream()
                .filter(ti -> "PURCHASE".equals(ti.getType()))
                .toList();
    }

    public TaxInvoice updateAPInvoice(Long id, TaxInvoiceRequestDto requestDto) {
        TaxInvoice existingInvoice = taxInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!"PURCHASE".equals(existingInvoice.getType()) || !"PURCHASE".equals(requestDto.getType())) {
            throw new IllegalArgumentException("AP Invoice는 PURCHASE 타입만 수정할 수 있습니다.");
        }

        BusinessPartner businessPartner = businessPartnerPersistencePort.findByBusinessPartnerCode(
                        requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "거래처를 찾을 수 없습니다. 코드: " + requestDto.getBusinessPartnerCode()));

        existingInvoice.setIssueId(requestDto.getIssueId());
        existingInvoice.setIssueDate(requestDto.getIssueDate());
        existingInvoice.setBusinessPartner(businessPartner);
        existingInvoice.setSupplyAmount(requestDto.getSupplyAmount());
        existingInvoice.setTaxAmount(requestDto.getTaxAmount());
        existingInvoice.setTotalAmount(requestDto.getTotalAmount());

        validateAmount(existingInvoice);
        return taxInvoiceRepository.save(existingInvoice);
    }

    public void deleteAPInvoice(Long id) {
        TaxInvoice existingInvoice = taxInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!"PURCHASE".equals(existingInvoice.getType())) {
            throw new IllegalArgumentException("삭제 대상 세금계산서가 PURCHASE 타입이 아닙니다.");
        }

        taxInvoiceRepository.delete(existingInvoice);
    }

    private void validateAmount(TaxInvoice taxInvoice) {
        if (taxInvoice.getSupplyAmount().add(taxInvoice.getTaxAmount()).compareTo(taxInvoice.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("공급가액과 세액의 합이 합계금액과 일치하지 않습니다.");
        }
    }
}
