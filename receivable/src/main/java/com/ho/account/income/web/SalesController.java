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
     * ?덈줈??留ㅼ텧 ?몃낫?댁뒪瑜??앹꽦?섍퀬 留ㅼ텧梨꾧텒???몄떇?섎ŉ, 留ㅼ텧 ?몄떇 ?꾪몴瑜??앹꽦?⑸땲??
     * @param request 留ㅼ텧 ?몃낫?댁뒪 ?깅줉 ?붿껌 DTO
     * @return ?앹꽦??留ㅼ텧 ?몃낫?댁뒪 ?뺣낫
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
     * ?뱀젙 ?좎쭨瑜?湲곗??쇰줈 留ㅼ텧梨꾧텒???곹깭瑜??낅뜲?댄듃?⑸땲??(?? ?곗껜 泥섎━).
     * @param asOfDate 泥섎━ 湲곗??쇱옄
     * @return 泥섎━ 寃곌낵 硫붿떆吏
     */
    @PostMapping("/receivables/update-status/{asOfDate}")
    public ResponseEntity<String> updateReceivableStatus(@PathVariable LocalDate asOfDate) {
        salesService.updateReceivableStatus(asOfDate);
        return ResponseEntity.ok("Receivable statuses updated as of " + asOfDate);
    }
}
