package com.ho.account.expenditure.service;

import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.dto.APPaymentRequestDto;
import com.ho.account.expenditure.repository.APPaymentRepository;
import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class APPaymentService {

    private final APPaymentRepository apPaymentRepository;
    private final ExpenditureResolutionRepository expenditureResolutionRepository;
    private final TaxInvoiceQueryPort taxInvoiceQueryPort;

    public APPaymentService(APPaymentRepository apPaymentRepository,
            ExpenditureResolutionRepository expenditureResolutionRepository,
            TaxInvoiceQueryPort taxInvoiceQueryPort) {
        this.apPaymentRepository = apPaymentRepository;
        this.expenditureResolutionRepository = expenditureResolutionRepository;
        this.taxInvoiceQueryPort = taxInvoiceQueryPort;
    }

    public APPayment createAPPayment(APPaymentRequestDto requestDto) {
        ExpenditureResolution expenditureResolution = expenditureResolutionRepository
                .findById(requestDto.getExpenditureResolutionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "지출결의서를 찾을 수 없습니다. ID: " + requestDto.getExpenditureResolutionId()));

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());

        APPayment apPayment = new APPayment();
        apPayment.setExpenditureResolution(expenditureResolution);
        apPayment.setTaxInvoiceId(requestDto.getTaxInvoiceId());
        apPayment.setPaymentDate(requestDto.getPaymentDate());
        apPayment.setAmount(requestDto.getAmount());
        apPayment.setUnappliedAmount(requestDto.getAmount());
        apPayment.setPaymentMethod(requestDto.getPaymentMethod());
        apPayment.setStatus("PENDING");

        return apPaymentRepository.save(apPayment);
    }

    @Transactional(readOnly = true)
    public Optional<APPayment> getAPPaymentById(Long id) {
        return apPaymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<APPayment> getAPPaymentsByExpenditureResolution(Long expenditureResolutionId) {
        return apPaymentRepository.findAll().stream()
                .filter(payment -> payment.getExpenditureResolution().getId().equals(expenditureResolutionId))
                .toList();
    }

    public APPayment updateAPPayment(Long id, APPaymentRequestDto requestDto) {
        APPayment existingPayment = apPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));

        ExpenditureResolution expenditureResolution = expenditureResolutionRepository
                .findById(requestDto.getExpenditureResolutionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "지출결의서를 찾을 수 없습니다. ID: " + requestDto.getExpenditureResolutionId()));

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());

        existingPayment.setExpenditureResolution(expenditureResolution);
        existingPayment.setTaxInvoiceId(requestDto.getTaxInvoiceId());
        existingPayment.setPaymentDate(requestDto.getPaymentDate());
        existingPayment.setAmount(requestDto.getAmount());
        existingPayment.setUnappliedAmount(requestDto.getAmount());
        existingPayment.setPaymentMethod(requestDto.getPaymentMethod());

        return apPaymentRepository.save(existingPayment);
    }

    public APPayment updateAPPaymentStatus(Long id, String status) {
        APPayment existingPayment = apPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));
        existingPayment.setStatus(status);
        return apPaymentRepository.save(existingPayment);
    }

    public void deleteAPPayment(Long id) {
        APPayment existingPayment = apPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));
        apPaymentRepository.delete(existingPayment);
    }

    private void validatePurchaseTaxInvoice(Long taxInvoiceId) {
        if (taxInvoiceId == null) {
            return;
        }

        TaxInvoiceRef taxInvoice = taxInvoiceQueryPort.findById(taxInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다. ID: " + taxInvoiceId));
        if (!"PURCHASE".equals(taxInvoice.type())) {
            throw new IllegalArgumentException("AP 지급에 연결하는 세금계산서는 PURCHASE 타입이어야 합니다.");
        }
    }
}
