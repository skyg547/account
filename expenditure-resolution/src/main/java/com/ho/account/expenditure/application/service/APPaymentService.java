package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.in.APPaymentUseCase;
import com.ho.account.expenditure.application.port.out.APPaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.APPaymentStatus;
import com.ho.account.expenditure.dto.APPaymentRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class APPaymentService implements APPaymentUseCase {

    private final APPaymentPersistencePort apPaymentPersistencePort;
    private final ExpenditureResolutionPersistencePort resolutionPersistencePort;
    private final TaxInvoiceQueryPort taxInvoiceQueryPort;

    public APPaymentService(
            APPaymentPersistencePort apPaymentPersistencePort,
            ExpenditureResolutionPersistencePort resolutionPersistencePort,
            TaxInvoiceQueryPort taxInvoiceQueryPort) {
        this.apPaymentPersistencePort = apPaymentPersistencePort;
        this.resolutionPersistencePort = resolutionPersistencePort;
        this.taxInvoiceQueryPort = taxInvoiceQueryPort;
    }

    @Override
    public APPayment createAPPayment(APPaymentRequestDto requestDto) {
        var expenditureResolution = resolutionPersistencePort
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
        apPayment.setStatus(APPaymentStatus.PENDING);

        return apPaymentPersistencePort.save(apPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<APPayment> getAPPaymentById(Long id) {
        return apPaymentPersistencePort.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<APPayment> getAPPaymentsByExpenditureResolution(Long expenditureResolutionId) {
        return apPaymentPersistencePort.findByExpenditureResolutionId(expenditureResolutionId);
    }

    @Override
    public APPayment updateAPPayment(Long id, APPaymentRequestDto requestDto) {
        APPayment existing = apPaymentPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));

        var expenditureResolution = resolutionPersistencePort
                .findById(requestDto.getExpenditureResolutionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "지출결의서를 찾을 수 없습니다. ID: " + requestDto.getExpenditureResolutionId()));

        validatePurchaseTaxInvoice(requestDto.getTaxInvoiceId());

        existing.setExpenditureResolution(expenditureResolution);
        existing.setTaxInvoiceId(requestDto.getTaxInvoiceId());
        existing.setPaymentDate(requestDto.getPaymentDate());
        existing.setAmount(requestDto.getAmount());
        existing.setUnappliedAmount(requestDto.getAmount());
        existing.setPaymentMethod(requestDto.getPaymentMethod());

        return apPaymentPersistencePort.save(existing);
    }

    @Override
    public APPayment updateAPPaymentStatus(Long id, APPaymentStatus status) {
        APPayment existing = apPaymentPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));
        existing.setStatus(status);
        return apPaymentPersistencePort.save(existing);
    }

    @Override
    public void deleteAPPayment(Long id) {
        APPayment existing = apPaymentPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));
        apPaymentPersistencePort.delete(existing);
    }

    private void validatePurchaseTaxInvoice(Long taxInvoiceId) {
        if (taxInvoiceId == null) return;
        TaxInvoiceRef taxInvoice = taxInvoiceQueryPort.findById(taxInvoiceId)
                .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다. ID: " + taxInvoiceId));
        if (!"PURCHASE".equals(taxInvoice.type())) {
            throw new IllegalArgumentException("AP 지급에 연결하는 세금계산서는 PURCHASE 타입이어야 합니다.");
        }
    }
}
