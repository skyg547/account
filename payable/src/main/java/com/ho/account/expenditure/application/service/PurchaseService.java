package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.in.PurchaseUseCase;
import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.*;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PurchaseService implements PurchaseUseCase {

    private final PurchaseInvoicePersistencePort purchaseInvoicePersistencePort;
    private final PayablePersistencePort payablePersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public PurchaseService(PurchaseInvoicePersistencePort purchaseInvoicePersistencePort,
                           PayablePersistencePort payablePersistencePort,
                           BusinessPartnerPersistencePort businessPartnerPersistencePort,
                           MasterDataQueryPort masterDataQueryPort,
                           JournalPostingPort journalPostingPort) {
        this.purchaseInvoicePersistencePort = purchaseInvoicePersistencePort;
        this.payablePersistencePort = payablePersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    @Override
    public PurchaseInvoice createPurchaseInvoice(PurchaseInvoice invoice) {
        String vendorCode = invoice.getVendor().getBusinessPartnerCode();
        validateVendor(vendorCode);
        
        BusinessPartner vendor = businessPartnerPersistencePort.findByBusinessPartnerCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorCode));
        invoice.setVendor(vendor);

        if (purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode(
                invoice.getInvoiceNo(), vendor.getBusinessPartnerCode()).isPresent()) {
            throw new IllegalArgumentException("Duplicate invoice number for vendor: " + vendor.getBusinessPartnerName());
        }

        if (invoice.getStatus() == null) {
            invoice.setStatus(PurchaseInvoiceStatus.RECEIVED);
        }
        if (invoice.getCreatedBy() == null) {
            invoice.setCreatedBy("SYSTEM");
        }

        PurchaseInvoice savedInvoice = purchaseInvoicePersistencePort.save(invoice);

        // 1. Payable 생성
        Payable payable = new Payable();
        payable.setPurchaseInvoice(savedInvoice);
        payable.setVendor(vendor);
        payable.setOriginalAmount(savedInvoice.getTotalAmount());
        payable.setOutstandingAmount(savedInvoice.getTotalAmount());
        payable.setDueDate(savedInvoice.getDueDate());
        payable.setStatus(PayableStatus.OPEN);
        payablePersistencePort.save(payable);

        // 2. 전표 발행
        postPurchaseJournal(savedInvoice, vendor);

        return savedInvoice;
    }

    @Override
    public void updatePayableStatus(LocalDate asOfDate) {
        List<Payable> overduePayables = payablePersistencePort.findByDueDateBeforeAndStatusNot(asOfDate, PayableStatus.PAID);
        for (Payable payable : overduePayables) {
            payable.markAsOverdue();
            payablePersistencePort.save(payable);
            
            // 연관 인보이스 상태 업데이트 (Rich Domain Model 적용 포인트)
            if (payable.getPurchaseInvoice() != null) {
                PurchaseInvoice pi = payable.getPurchaseInvoice();
                if (pi.getStatus() != PurchaseInvoiceStatus.PARTIAL_PAID) {
                    pi.setStatus(PurchaseInvoiceStatus.OVERDUE);
                    purchaseInvoicePersistencePort.save(pi);
                }
            }
        }
    }

    private void validateVendor(String vendorCode) {
        masterDataQueryPort.findBusinessPartner(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor info missing in master data: " + vendorCode));
    }

    private void postPurchaseJournal(PurchaseInvoice invoice, BusinessPartner vendor) {
        requireAccount("21100", "Accounts Payable account missing");
        requireAccount("50100", "Expense account missing");
        requireAccount("13500", "VAT account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "Purchase: " + invoice.getInvoiceNo() + " - " + vendor.getBusinessPartnerName(),
                "PURCHASE_RECOGNITION",
                null,
                null,
                invoice.getCreatedBy(),
                invoice.getCreatedBy(),
                "PURCHASE_INVOICE",
                invoice.getInvoiceNo() + "_" + vendor.getBusinessPartnerCode(),
                List.of(
                        new JournalLineCommand("DEBIT", "50100", invoice.getNetAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "Purchase Expense"),
                        new JournalLineCommand("DEBIT", "13500", invoice.getTaxAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "Input VAT"),
                        new JournalLineCommand("CREDIT", "21100", invoice.getTotalAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "Accounts Payable"))));
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
