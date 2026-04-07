package com.ho.account.income.web;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.income.domain.SalesInvoice;
import com.ho.account.income.dto.SalesInvoiceRequest;
import com.ho.account.income.service.SalesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesService salesService;
    private final BusinessPartnerRepository businessPartnerRepository; // For mapping request DTO to domain object

    public SalesController(SalesService salesService, BusinessPartnerRepository businessPartnerRepository) {
        this.salesService = salesService;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    /**
     * 새로운 매출 인보이스를 생성하고 매출채권을 인식하며, 매출 인식 전표를 생성합니다.
     * @param request 매출 인보이스 등록 요청 DTO
     * @return 생성된 매출 인보이스 정보
     */
    @PostMapping("/invoices")
    public ResponseEntity<SalesInvoice> createSalesInvoice(@Valid @RequestBody SalesInvoiceRequest request) {
        SalesInvoice salesInvoice = new SalesInvoice();
        salesInvoice.setInvoiceNo(request.getInvoiceNo());
        salesInvoice.setIssueDate(request.getIssueDate());
        salesInvoice.setDueDate(request.getDueDate());
        salesInvoice.setTotalAmount(request.getTotalAmount());
        salesInvoice.setTaxAmount(request.getTaxAmount());
        salesInvoice.setNetAmount(request.getNetAmount());
        salesInvoice.setDescription(request.getDescription());

        BusinessPartner customer = businessPartnerRepository.findByBusinessPartnerCode(request.getCustomerCode())
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다: " + request.getCustomerCode()));
        salesInvoice.setCustomer(customer);
        salesInvoice.setCreatedBy("SYSTEM"); // TODO: 실제 로그인 사용자 정보로 대체

        SalesInvoice createdInvoice = salesService.createSalesInvoice(salesInvoice);
        return new ResponseEntity<>(createdInvoice, HttpStatus.CREATED);
    }

    /**
     * 특정 날짜를 기준으로 매출채권의 상태를 업데이트합니다 (예: 연체 처리).
     * @param asOfDate 처리 기준일자
     * @return 처리 결과 메시지
     */
    @PostMapping("/receivables/update-status/{asOfDate}")
    public ResponseEntity<String> updateReceivableStatus(@PathVariable LocalDate asOfDate) {
        salesService.updateReceivableStatus(asOfDate);
        return ResponseEntity.ok("Receivable statuses updated as of " + asOfDate);
    }
}
