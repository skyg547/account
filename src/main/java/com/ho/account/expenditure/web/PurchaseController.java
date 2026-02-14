package com.ho.account.expenditure.web;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.dto.PurchaseInvoiceRequest;
import com.ho.account.expenditure.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final BusinessPartnerRepository businessPartnerRepository; // For mapping request DTO to domain object

    public PurchaseController(PurchaseService purchaseService, BusinessPartnerRepository businessPartnerRepository) {
        this.purchaseService = purchaseService;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    /**
     * 새로운 매입 인보이스를 생성하고 매입채무를 인식하며, 매입 인식 전표를 생성합니다.
     * @param request 매입 인보이스 등록 요청 DTO
     * @return 생성된 매입 인보이스 정보
     */
    @PostMapping("/invoices")
    public ResponseEntity<PurchaseInvoice> createPurchaseInvoice(@Valid @RequestBody PurchaseInvoiceRequest request) {
        PurchaseInvoice purchaseInvoice = new PurchaseInvoice();
        purchaseInvoice.setInvoiceNo(request.getInvoiceNo());
        purchaseInvoice.setIssueDate(request.getIssueDate());
        purchaseInvoice.setDueDate(request.getDueDate());
        purchaseInvoice.setTotalAmount(request.getTotalAmount());
        purchaseInvoice.setTaxAmount(request.getTaxAmount());
        purchaseInvoice.setNetAmount(request.getNetAmount());
        purchaseInvoice.setDescription(request.getDescription());

        BusinessPartner vendor = businessPartnerRepository.findByBusinessPartnerCode(request.getVendorCode())
                .orElseThrow(() -> new IllegalArgumentException("공급업체 정보를 찾을 수 없습니다: " + request.getVendorCode()));
        purchaseInvoice.setVendor(vendor);
        purchaseInvoice.setCreatedBy("SYSTEM"); // TODO: 실제 로그인 사용자 정보로 대체

        PurchaseInvoice createdInvoice = purchaseService.createPurchaseInvoice(purchaseInvoice);
        return new ResponseEntity<>(createdInvoice, HttpStatus.CREATED);
    }

    /**
     * 특정 날짜를 기준으로 매입채무의 상태를 업데이트합니다 (예: 연체 처리).
     * @param asOfDate 처리 기준일자
     * @return 처리 결과 메시지
     */
    @PostMapping("/payables/update-status/{asOfDate}")
    public ResponseEntity<String> updatePayableStatus(@PathVariable LocalDate asOfDate) {
        purchaseService.updatePayableStatus(asOfDate);
        return ResponseEntity.ok("Payable statuses updated as of " + asOfDate);
    }
}
