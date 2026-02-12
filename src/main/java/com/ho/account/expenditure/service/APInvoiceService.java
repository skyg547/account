package com.ho.account.expenditure.service;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.dto.TaxInvoiceRequestDto;
import com.ho.account.tax.repository.TaxInvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class APInvoiceService {

    private final TaxInvoiceRepository taxInvoiceRepository;
    private final BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    public APInvoiceService(TaxInvoiceRepository taxInvoiceRepository, BusinessPartnerRepository businessPartnerRepository) {
        this.taxInvoiceRepository = taxInvoiceRepository;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    /**
     * 매입 세금계산서 (AP Invoice)를 생성합니다.
     *
     * @param requestDto 생성할 매입 세금계산서 정보가 담긴 DTO
     * @return 생성된 TaxInvoice 엔티티
     */
    public TaxInvoice createAPInvoice(TaxInvoiceRequestDto requestDto) {
        if (!"PURCHASE".equals(requestDto.getType())) {
            throw new IllegalArgumentException("AP Invoice는 'PURCHASE' 타입만 생성할 수 있습니다.");
        }

        BusinessPartner businessPartner = businessPartnerRepository.findByBusinessPartnerCode(requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. 코드: " + requestDto.getBusinessPartnerCode()));

        TaxInvoice taxInvoice = new TaxInvoice();
        taxInvoice.setIssueId(requestDto.getIssueId());
        taxInvoice.setType(requestDto.getType());
        taxInvoice.setIssueDate(requestDto.getIssueDate());
        taxInvoice.setBusinessPartner(businessPartner);
        taxInvoice.setSupplyAmount(requestDto.getSupplyAmount());
        taxInvoice.setTaxAmount(requestDto.getTaxAmount());
        taxInvoice.setTotalAmount(requestDto.getTotalAmount());

        // 합계금액 검증 (서비스 계층에서 다시 검증)
        if (taxInvoice.getSupplyAmount().add(taxInvoice.getTaxAmount()).compareTo(taxInvoice.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("공급가액과 세액의 합이 합계금액과 일치하지 않습니다.");
        }

        return taxInvoiceRepository.save(taxInvoice);
    }

    /**
     * ID로 매입 세금계산서 (AP Invoice)를 조회합니다.
     *
     * @param id 조회할 AP Invoice ID
     * @return Optional<TaxInvoice>
     */
    @Transactional(readOnly = true)
    public Optional<TaxInvoice> getAPInvoiceById(Long id) {
        return taxInvoiceRepository.findById(id)
                .filter(ti -> "PURCHASE".equals(ti.getType()));
    }

    /**
     * 발행번호로 매입 세금계산서 (AP Invoice)를 조회합니다.
     *
     * @param issueId 조회할 발행번호
     * @return Optional<TaxInvoice>
     */
    @Transactional(readOnly = true)
    public Optional<TaxInvoice> getAPInvoiceByIssueId(String issueId) {
        // Since issueId is unique across all types, we need to filter by type after fetching
        return taxInvoiceRepository.findByIssueId(issueId) // Use the new findByIssueId method
                .filter(ti -> "PURCHASE".equals(ti.getType()));
    }

    /**
     * 특정 기간 동안의 모든 매입 세금계산서 (AP Invoice)를 조회합니다.
     *
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return 매입 TaxInvoice 리스트
     */
    @Transactional(readOnly = true)
    public List<TaxInvoice> getAPInvoicesBetweenDates(LocalDate startDate, LocalDate endDate) {
        return taxInvoiceRepository.findByIssueDateBetween(startDate, endDate).stream()
                .filter(ti -> "PURCHASE".equals(ti.getType()))
                .toList();
    }

    /**
     * 매입 세금계산서 (AP Invoice) 정보를 수정합니다.
     *
     * @param id         수정할 AP Invoice ID
     * @param requestDto 수정할 내용이 담긴 DTO
     * @return 수정된 TaxInvoice 엔티티
     */
    public TaxInvoice updateAPInvoice(Long id, TaxInvoiceRequestDto requestDto) {
        TaxInvoice existingInvoice = taxInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!"PURCHASE".equals(existingInvoice.getType())) {
            throw new IllegalArgumentException("수정하려는 세금계산서가 'PURCHASE' 타입이 아닙니다.");
        }
        if (!"PURCHASE".equals(requestDto.getType())) {
            throw new IllegalArgumentException("AP Invoice는 'PURCHASE' 타입으로만 수정할 수 있습니다.");
        }

        BusinessPartner businessPartner = businessPartnerRepository.findByBusinessPartnerCode(requestDto.getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. 코드: " + requestDto.getBusinessPartnerCode()));

        existingInvoice.setIssueId(requestDto.getIssueId());
        existingInvoice.setIssueDate(requestDto.getIssueDate());
        existingInvoice.setBusinessPartner(businessPartner);
        existingInvoice.setSupplyAmount(requestDto.getSupplyAmount());
        existingInvoice.setTaxAmount(requestDto.getTaxAmount());
        existingInvoice.setTotalAmount(requestDto.getTotalAmount());

        // 합계금액 검증
        if (existingInvoice.getSupplyAmount().add(existingInvoice.getTaxAmount()).compareTo(existingInvoice.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("공급가액과 세액의 합이 합계금액과 일치하지 않습니다.");
        }

        return taxInvoiceRepository.save(existingInvoice);
    }

    /**
     * 매입 세금계산서를 삭제합니다 (논리적 삭제 또는 상태 변경).
     *
     * @param id 삭제할 AP Invoice ID
     */
    public void deleteAPInvoice(Long id) {
        TaxInvoice existingInvoice = taxInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("매입 세금계산서를 찾을 수 없습니다. ID: " + id));

        if (!"PURCHASE".equals(existingInvoice.getType())) {
            throw new IllegalArgumentException("삭제하려는 세금계산서가 'PURCHASE' 타입이 아닙니다.");
        }

        // 실제 삭제 대신 상태 변경 등의 논리적 삭제를 고려할 수 있습니다.
        // 현재 TaxInvoice 엔티티에 상태 필드가 없으므로 실제 삭제를 진행하거나,
        // 필요에 따라 TaxInvoice 엔티티에 'status' 필드를 추가하여 논리적 삭제를 구현할 수 있습니다.
        taxInvoiceRepository.delete(existingInvoice);
    }
}
