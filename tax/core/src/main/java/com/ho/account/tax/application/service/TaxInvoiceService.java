package com.ho.account.tax.application.service;

import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.tax.application.port.in.TaxInvoiceCommand;
import com.ho.account.tax.application.port.in.TaxInvoiceUseCase;
import com.ho.account.tax.application.port.out.TaxInvoicePersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 세금계산서 비즈니스 로직을 담당하는 핵심 서비스입니다.
 *
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 시스템의 '세무 담당자'입니다.
 * API Controller가 받은 JSON 모양은 여기까지 직접 들어오지 않고, `TaxInvoiceCommand`라는 업무 명령으로 변환됩니다.
 * 그래서 core는 HTTP, Bean Validation, 화면 DTO를 몰라도 거래처 검증, 금액 정합성, 취소 정책 같은 세무 규칙에 집중할 수 있습니다.
 * 타 모듈(Master Data)과는 ID(Code) 기반 Port로 통신하여 모듈 간 결합도를 최소화합니다.
 */
@Service
@Transactional
public class TaxInvoiceService implements TaxInvoiceUseCase {

    private static final String PURCHASE_TYPE = "PURCHASE";

    private final TaxInvoicePersistencePort taxInvoicePersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;

    public TaxInvoiceService(TaxInvoicePersistencePort taxInvoicePersistencePort,
                             MasterDataQueryPort masterDataQueryPort) {
        this.taxInvoicePersistencePort = taxInvoicePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
    }

    @Override
    public TaxInvoice createAPInvoice(TaxInvoiceCommand command) {
        requirePurchaseType(command.type(), "AP Invoice는 PURCHASE 타입만 생성할 수 있습니다.");
        verifyBusinessPartner(command.businessPartnerCode());

        TaxInvoice taxInvoice = TaxInvoice.create(
                command.issueId(),
                command.type(),
                command.issueDate(),
                command.businessPartnerCode(),
                command.supplyAmount(),
                command.taxAmount(),
                command.totalAmount()
        );

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
    public TaxInvoice updateAPInvoice(Long id, TaxInvoiceCommand command) {
        TaxInvoice existingInvoice = taxInvoicePersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!existingInvoice.isPurchaseType()) {
            throw new IllegalArgumentException("수정 대상 세금계산서가 PURCHASE 타입이 아닙니다.");
        }
        requirePurchaseType(command.type(), "AP Invoice는 PURCHASE 타입만 수정할 수 있습니다.");
        verifyBusinessPartner(command.businessPartnerCode());

        existingInvoice.updateInfo(
                command.issueId(),
                command.issueDate(),
                command.businessPartnerCode(),
                command.supplyAmount(),
                command.taxAmount(),
                command.totalAmount()
        );

        return taxInvoicePersistencePort.save(existingInvoice);
    }

    @Override
    public void cancelAPInvoice(Long id, String actor, String reason) {
        TaxInvoice existingInvoice = taxInvoicePersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!existingInvoice.isPurchaseType()) {
            throw new IllegalArgumentException("취소 대상 세금계산서가 PURCHASE 타입이 아닙니다.");
        }

        existingInvoice.cancel(actor, reason);
        taxInvoicePersistencePort.save(existingInvoice);
    }

    private void requirePurchaseType(String type, String message) {
        if (!PURCHASE_TYPE.equals(type)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void verifyBusinessPartner(String businessPartnerCode) {
        masterDataQueryPort.findBusinessPartner(businessPartnerCode)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다: " + businessPartnerCode));
    }
}