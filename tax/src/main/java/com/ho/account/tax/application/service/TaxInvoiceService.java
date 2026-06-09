package com.ho.account.tax.application.service;

import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.tax.application.port.in.TaxInvoiceUseCase;
import com.ho.account.tax.application.port.out.TaxInvoicePersistencePort;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.dto.TaxInvoiceRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 세금계산서 비즈니스 로직을 담당하는 핵심 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 시스템의 '세무 담당자'입니다. 
 * 매입 세금계산서를 받으면 거래처가 실제 등록된 곳인지 확인하고, 
 * 우리 장부에 기록할 수 있도록 도메인 객체(`TaxInvoice`)를 생성하여 저장소(Port)에 보관하는 일을 합니다.
 * 타 모듈(Master Data)과는 ID(Code) 기반으로 통신하여 모듈 간 결합도를 최소화합니다.
 */
@Service
@Transactional
public class TaxInvoiceService implements TaxInvoiceUseCase {

    // @todo [DDD/Bounded Context] The service stores only a partner code, but validation still imports a
    // master-data internal port. Query partner validity through a contracts-level port so the tax use case
    // remains independent of master-data persistence and domain implementation details.
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

        // 거래처 존재 여부 검증 (모듈 간 정합성 체크)
        businessPartnerPersistencePort.findByBusinessPartnerCode(requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다: " + requestDto.getBusinessPartnerCode()));

        TaxInvoice taxInvoice = TaxInvoice.create(
                requestDto.getIssueId(),
                requestDto.getType(),
                requestDto.getIssueDate(),
                requestDto.getBusinessPartnerCode(), // 엔티티 직접 참조 대신 Code 저장
                requestDto.getSupplyAmount(),
                requestDto.getTaxAmount(),
                requestDto.getTotalAmount()
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
    public TaxInvoice updateAPInvoice(Long id, TaxInvoiceRequestDto requestDto) {
        TaxInvoice existingInvoice = taxInvoicePersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!existingInvoice.isPurchaseType() || !"PURCHASE".equals(requestDto.getType())) {
            throw new IllegalArgumentException("AP Invoice는 PURCHASE 타입만 수정할 수 있습니다.");
        }

        businessPartnerPersistencePort.findByBusinessPartnerCode(requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다: " + requestDto.getBusinessPartnerCode()));

        existingInvoice.updateInfo(
                requestDto.getIssueId(),
                requestDto.getIssueDate(),
                requestDto.getBusinessPartnerCode(), // 엔티티 직접 참조 대신 Code 저장
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

        // @todo [업무 프로세스] (see docs/todo_remediation_plan.md) 세금계산서와 같은 증빙 문서는 물리적 삭제(delete)보다 수정 세금계산서 발행 또는 취소(Cancel) 상태로의 논리적 삭제 및 이력 보존이 회계 원칙에 부합합니다.
        taxInvoicePersistencePort.delete(existingInvoice);
    }
}
