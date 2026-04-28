package com.ho.account.expenditure.web;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
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

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * ?덈줈??留ㅼ엯 ?몃낫?댁뒪瑜??앹꽦?섍퀬 留ㅼ엯梨꾨Т瑜??몄떇?섎ŉ, 留ㅼ엯 ?몄떇 ?꾪몴瑜??앹꽦?⑸땲??
     * @param request 留ㅼ엯 ?몃낫?댁뒪 ?깅줉 ?붿껌 DTO
     * @return ?앹꽦??留ㅼ엯 ?몃낫?댁뒪 ?뺣낫
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

        BusinessPartner vendor = new BusinessPartner();
        vendor.setBusinessPartnerCode(request.getVendorCode());
        purchaseInvoice.setVendor(vendor);
        purchaseInvoice.setCreatedBy("SYSTEM");

        PurchaseInvoice createdInvoice = purchaseService.createPurchaseInvoice(purchaseInvoice);
        return new ResponseEntity<>(createdInvoice, HttpStatus.CREATED);
    }

    /**
     * ?뱀젙 ?좎쭨瑜?湲곗??쇰줈 留ㅼ엯梨꾨Т???곹깭瑜??낅뜲?댄듃?⑸땲??(?? ?곗껜 泥섎━).
     * @param asOfDate 泥섎━ 湲곗??쇱옄
     * @return 泥섎━ 寃곌낵 硫붿떆吏
     */
    @PostMapping("/payables/update-status/{asOfDate}")
    public ResponseEntity<String> updatePayableStatus(@PathVariable LocalDate asOfDate) {
        purchaseService.updatePayableStatus(asOfDate);
        return ResponseEntity.ok("Payable statuses updated as of " + asOfDate);
    }
}
