package com.ho.account.income.web;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
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

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    /**
     * ?àÎ°ú??Îß§Ï∂ú ?∏Î≥¥?¥Ïä§Î•??ùÏÑ±?òÍ≥† Îß§Ï∂úÏ±ÑÍ∂å???∏Ïãù?òÎ©∞, Îß§Ï∂ú ?∏Ïãù ?ÑÌëúÎ•??ùÏÑ±?©Îãà??
     * @param request Îß§Ï∂ú ?∏Î≥¥?¥Ïä§ ?±Î°ù ?îÏ≤≠ DTO
     * @return ?ùÏÑ±??Îß§Ï∂ú ?∏Î≥¥?¥Ïä§ ?ïÎ≥¥
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

        BusinessPartner customer = new BusinessPartner();
        customer.setBusinessPartnerCode(request.getCustomerCode());
        salesInvoice.setCustomer(customer);
        salesInvoice.setCreatedBy("SYSTEM");

        SalesInvoice createdInvoice = salesService.createSalesInvoice(salesInvoice);
        return new ResponseEntity<>(createdInvoice, HttpStatus.CREATED);
    }

    /**
     * ?πÏ†ï ?†ÏßúÎ•?Í∏∞Ï??ºÎ°ú Îß§Ï∂úÏ±ÑÍ∂å???ÅÌÉúÎ•??ÖÎç∞?¥Ìä∏?©Îãà??(?? ?∞Ï≤¥ Ï≤òÎ¶¨).
     * @param asOfDate Ï≤òÎ¶¨ Í∏∞Ï??ºÏûê
     * @return Ï≤òÎ¶¨ Í≤∞Í≥º Î©îÏãúÏßÄ
     */
    @PostMapping("/receivables/update-status/{asOfDate}")
    public ResponseEntity<String> updateReceivableStatus(@PathVariable LocalDate asOfDate) {
        salesService.updateReceivableStatus(asOfDate);
        return ResponseEntity.ok("Receivable statuses updated as of " + asOfDate);
    }
}
