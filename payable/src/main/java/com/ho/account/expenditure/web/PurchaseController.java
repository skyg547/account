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
     * ?ˆë¡œ??ë§¤ì… ?¸ë³´?´ìŠ¤ë¥??ì„±?˜ê³  ë§¤ì…ì±„ë¬´ë¥??¸ì‹?˜ë©°, ë§¤ì… ?¸ì‹ ?„í‘œë¥??ì„±?©ë‹ˆ??
     * @param request ë§¤ì… ?¸ë³´?´ìŠ¤ ?±ë¡ ?”ì²­ DTO
     * @return ?ì„±??ë§¤ì… ?¸ë³´?´ìŠ¤ ?•ë³´
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
     * ?¹ì • ? ì§œë¥?ê¸°ì??¼ë¡œ ë§¤ì…ì±„ë¬´???íƒœë¥??…ë°?´íŠ¸?©ë‹ˆ??(?? ?°ì²´ ì²˜ë¦¬).
     * @param asOfDate ì²˜ë¦¬ ê¸°ì??¼ì
     * @return ì²˜ë¦¬ ê²°ê³¼ ë©”ì‹œì§€
     */
    @PostMapping("/payables/update-status/{asOfDate}")
    public ResponseEntity<String> updatePayableStatus(@PathVariable LocalDate asOfDate) {
        purchaseService.updatePayableStatus(asOfDate);
        return ResponseEntity.ok("Payable statuses updated as of " + asOfDate);
    }
}
