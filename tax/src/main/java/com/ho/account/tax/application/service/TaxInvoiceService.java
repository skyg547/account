package com.ho.account.tax.application.service;

import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.tax.application.port.in.TaxInvoiceUseCase;
import com.ho.account.tax.application.port.out.TaxInvoicePersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.dto.TaxInvoiceRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaxInvoiceService implements TaxInvoiceUseCase {

    private final TaxInvoicePersistencePort taxInvoicePersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public TaxInvoiceService(TaxInvoicePersistencePort taxInvoicePersistencePort,
                             BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.taxInvoicePersistencePort = taxInvoicePersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    @Override
    public TaxInvoice createAPInvoice(TaxInvoiceRequestDto requestDto) {
        if (!"PURCHASE".equals(requestDto.getType())) {
            throw new IllegalArgumentException("AP Invoice는 PURCHASE 타입만 생성할 수 있습니다.");
        }

        BusinessPartner businessPartner = businessPartnerPersistencePort.findByBusinessPartnerCode(
                        requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다: " + requestDto.getBusinessPartnerCode()));

        TaxInvoice taxInvoice = new TaxInvoice();
        taxInvoice.setIssueId(requestDto.getIssueId());
        taxInvoice.setType(requestDto.getType());
        taxInvoice.setIssueDate(requestDto.getIssueDate());
        taxInvoice.setBusinessPartner(businessPartner);
        taxInvoice.setSupplyAmount(requestDto.getSupplyAmount());
        taxInvoice.setTaxAmount(requestDto.getTaxAmount());
        taxInvoice.setTotalAmount(requestDto.getTotalAmount());

        taxInvoice.validateAmounts();
        return taxInvoicePersistencePort.save(taxInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxInvoice> getAPInvoiceById(Long id) {
        return taxInvoicePersistencePort.findById(id)
                .filter(TaxInvoice::isPurchaseType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxInvoice> getAPInvoiceByIssueId(String issueId) {
        return taxInvoicePersistencePort.findByIssueId(issueId)
                .filter(TaxInvoice::isPurchaseType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxInvoice> getAPInvoicesBetweenDates(LocalDate startDate, LocalDate endDate) {
        return taxInvoicePersistencePort.findByIssueDateBetween(startDate, endDate).stream()
                .filter(TaxInvoice::isPurchaseType)
                .toList();
    }

    @Override
    public TaxInvoice updateAPInvoice(Long id, TaxInvoiceRequestDto requestDto) {
        TaxInvoice existingInvoice = taxInvoicePersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!existingInvoice.isPurchaseType() || !"PURCHASE".equals(requestDto.getType())) {
            throw new IllegalArgumentException("AP Invoice는 PURCHASE 타입만 수정할 수 있습니다.");
        }

        BusinessPartner businessPartner = businessPartnerPersistencePort.findByBusinessPartnerCode(
                        requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다: " + requestDto.getBusinessPartnerCode()));

        existingInvoice.updateInfo(
                requestDto.getIssueId(),
                requestDto.getIssueDate(),
                businessPartner,
                requestDto.getSupplyAmount(),
                requestDto.getTaxAmount(),
                requestDto.getTotalAmount()
        );

        return taxInvoicePersistencePort.save(existingInvoice);
    }

    @Override
    public void deleteAPInvoice(Long id) {
        TaxInvoice existingInvoice = taxInvoicePersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!existingInvoice.isPurchaseType()) {
            throw new IllegalArgumentException("삭제 대상 세금계산서가 PURCHASE 타입이 아닙니다.");
        }

        taxInvoicePersistencePort.delete(existingInvoice);
    }
}
