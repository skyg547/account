package com.ho.account.expenditure.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.domain.*;
import com.ho.account.expenditure.repository.PayableRepository;
import com.ho.account.expenditure.repository.PurchaseInvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PurchaseService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final PayableRepository payableRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public PurchaseService(PurchaseInvoiceRepository purchaseInvoiceRepository,
                           PayableRepository payableRepository,
                           BusinessPartnerRepository businessPartnerRepository,
                           MasterDataQueryPort masterDataQueryPort,
                           JournalPostingPort journalPostingPort) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.payableRepository = payableRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    /**
     * ë§¤ì… ?¸ë³´?´ìŠ¤ë¥??ì„±?˜ê³  ?´ë‹¹ ë§¤ì…ì±„ë¬´ë¥??¸ì‹?˜ë©°, ë§¤ì… ?¸ì‹ ?„í‘œë¥??ì„±?©ë‹ˆ??
     * ì¤‘ë³µ ?¸ë³´?´ìŠ¤ (ê±°ë˜ì²? ?¸ë³´?´ìŠ¤ë²ˆí˜¸)ë¥?ë°©ì??©ë‹ˆ??
     * @param invoice ë§¤ì… ?¸ë³´?´ìŠ¤ ?•ë³´
     * @return ?ì„±??ë§¤ì… ?¸ë³´?´ìŠ¤
     */
    public PurchaseInvoice createPurchaseInvoice(PurchaseInvoice invoice) {
        String vendorCode = invoice.getVendor().getBusinessPartnerCode();
        masterDataQueryPort.findBusinessPartner(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("ê³µê¸‰?…ì²´ ?•ë³´ë¥?ì°¾ì„ ???†ìŠµ?ˆë‹¤: " + vendorCode));
        BusinessPartner vendor = businessPartnerRepository.findByBusinessPartnerCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("ê³µê¸‰?…ì²´ ?•ë³´ë¥?ì°¾ì„ ???†ìŠµ?ˆë‹¤: " + vendorCode));
        invoice.setVendor(vendor);

        // ì¤‘ë³µ ?¸ë³´?´ìŠ¤ ?•ì¸ (ê±°ë˜ì²?+ ?¸ë³´?´ìŠ¤ ë²ˆí˜¸)
        if (purchaseInvoiceRepository.findByInvoiceNoAndVendorBusinessPartnerCode(
                invoice.getInvoiceNo(), vendor.getBusinessPartnerCode()).isPresent()) {
            throw new IllegalArgumentException("?´ë? ì¡´ì¬?˜ëŠ” ?¸ë³´?´ìŠ¤ ë²ˆí˜¸?…ë‹ˆ??(ê³µê¸‰?…ì²´: " + vendor.getBusinessPartnerName() + ", ?¸ë³´?´ìŠ¤ ë²ˆí˜¸: " + invoice.getInvoiceNo() + ")");
        }


        // ê¸°ë³¸ ?íƒœ ?¤ì •
        if (invoice.getStatus() == null) {
            invoice.setStatus(PurchaseInvoiceStatus.RECEIVED);
        }
        if (invoice.getCreatedBy() == null) {
            invoice.setCreatedBy("SYSTEM"); // ?ëŠ” ?„ì¬ ë¡œê·¸???¬ìš©???•ë³´
        }

        PurchaseInvoice savedInvoice = purchaseInvoiceRepository.save(invoice);

        // 1. ë§¤ì…ì±„ë¬´ ?ì„± (Open Item)
        Payable payable = new Payable();
        payable.setPurchaseInvoiceNo(savedInvoice.getInvoiceNo());
        payable.setPurchaseInvoiceVendorCode(savedInvoice.getVendor().getBusinessPartnerCode());

        payable.setVendor(vendor);
        payable.setOriginalAmount(savedInvoice.getTotalAmount());
        payable.setOutstandingAmount(savedInvoice.getTotalAmount()); // ì´ˆê¸°?ëŠ” ë¯¸ì?ê¸‰ê¸ˆ ?„ì•¡
        payable.setDueDate(savedInvoice.getDueDate());
        payable.setStatus(PayableStatus.OPEN);
        payableRepository.save(payable);

        // 2. ë§¤ì… ?¸ì‹ ?„í‘œ ?ì„±
        requireAccount("21100", "AccountSubject for Accounts Payable not found");
        requireAccount("50100", "AccountSubject for Expense not found");
        requireAccount("13500", "AccountSubject for VAT Receivable not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "ë§¤ì… ?¸ì‹: " + invoice.getInvoiceNo() + " - " + invoice.getVendor().getBusinessPartnerName(),
                "PURCHASE_RECOGNITION",
                null,
                null,
                invoice.getCreatedBy(),
                invoice.getCreatedBy(),
                "PURCHASE_INVOICE",
                invoice.getInvoiceNo() + "_" + invoice.getVendor().getBusinessPartnerCode(),
                List.of(
                        new JournalLineCommand("DEBIT", "50100", invoice.getNetAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "?í’ˆ ë§¤ì…"),
                        new JournalLineCommand("DEBIT", "13500", invoice.getTaxAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "ë¶€ê°€?¸ë?ê¸‰ê¸ˆ"),
                        new JournalLineCommand("CREDIT", "21100", invoice.getTotalAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "ë§¤ì…ì±„ë¬´ ë°œìƒ"))));

        return savedInvoice;
    }

    /**
     * ë§¤ì…ì±„ë¬´ ?íƒœë¥??…ë°?´íŠ¸?©ë‹ˆ?? (?? ?°ì²´ ì²˜ë¦¬)
     * @param asOfDate ê¸°ì??¼ì
     */
    public void updatePayableStatus(LocalDate asOfDate) {
        // ë§Œê¸°?¼ì´ ì§€??OPEN ?ëŠ” PARTIAL_PAID ?íƒœ??ì±„ë¬´ë¥?OVERDUEë¡?ë³€ê²?
        List<Payable> overduePayables = payableRepository.findByDueDateBeforeAndStatusNot(asOfDate, PayableStatus.PAID);
        for (Payable payable : overduePayables) {
            if (payable.getDueDate().isBefore(asOfDate)) { // ?•ì‹¤??ë§Œê¸°?¼ì´ ì§€??ê²½ìš°
                payable.setStatus(PayableStatus.OVERDUE);
                payableRepository.save(payable);

                // ê´€??PurchaseInvoice??OVERDUEë¡??…ë°?´íŠ¸ (ë¶€ë¶?ì§€ê¸‰ëœ ê²½ìš° ?œì™¸ ??ë¡œì§ ì¶”ê? ê°€??
                if (payable.getPurchaseInvoice() != null &&
                    payable.getPurchaseInvoice().getStatus() != PurchaseInvoiceStatus.PARTIAL_PAID) { // ë¶€ë¶?ì§€ê¸‰ëœ ê±´ì? ?°ë¡œ ê´€ë¦?
                    // purchaseInvoiceRepository.findById(new PurchaseInvoiceId(payable.getPurchaseInvoiceNo(), payable.getPurchaseInvoiceVendorCode()))
                    //         .ifPresent(pi -> {
                    //             pi.setStatus(PurchaseInvoiceStatus.OVERDUE);
                    //             purchaseInvoiceRepository.save(pi);
                    //         });
                    // Lazy loading issue workaround if purchaseInvoice is not eagerly loaded
                    PurchaseInvoice pi = payable.getPurchaseInvoice();
                    if (pi != null) {
                        pi.setStatus(PurchaseInvoiceStatus.OVERDUE);
                        purchaseInvoiceRepository.save(pi);
                    }
                }
            }
        }
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
