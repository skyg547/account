package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.expenditure.application.port.in.APPaymentCommand;
import com.ho.account.expenditure.application.port.in.APPaymentUseCase;
import com.ho.account.expenditure.application.port.out.APPaymentPersistencePort;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.APPaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public APPayment createAPPayment(APPaymentCommand command) {
        var expenditureResolution = resolutionPersistencePort
                .findById(command.expenditureResolutionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "지출결의서를 찾을 수 없습니다. ID: " + command.expenditureResolutionId()));

        validatePurchaseTaxInvoice(command.taxInvoiceId());

        APPayment apPayment = new APPayment();
        apPayment.setExpenditureResolution(expenditureResolution);
        apPayment.setTaxInvoiceId(command.taxInvoiceId());
        apPayment.setPaymentDate(command.paymentDate());
        apPayment.setAmount(command.amount());
        apPayment.setUnappliedAmount(command.amount());
        apPayment.setPaymentMethod(command.paymentMethod());
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
    public APPayment updateAPPayment(Long id, APPaymentCommand command) {
        APPayment existing = apPaymentPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));

        var expenditureResolution = resolutionPersistencePort
                .findById(command.expenditureResolutionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "지출결의서를 찾을 수 없습니다. ID: " + command.expenditureResolutionId()));

        validatePurchaseTaxInvoice(command.taxInvoiceId());

        existing.setExpenditureResolution(expenditureResolution);
        existing.setTaxInvoiceId(command.taxInvoiceId());
        existing.setPaymentDate(command.paymentDate());
        existing.setAmount(command.amount());
        existing.setUnappliedAmount(command.amount());
        existing.setPaymentMethod(command.paymentMethod());

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
        if (!taxInvoice.purchase()) {
            throw new IllegalArgumentException("AP 지급에 연결하는 세금계산서는 PURCHASE 타입이어야 합니다.");
        }
        if (!taxInvoice.active()) {
            throw new IllegalArgumentException("AP 지급에는 취소된 세금계산서를 연결할 수 없습니다.");
        }
    }
}