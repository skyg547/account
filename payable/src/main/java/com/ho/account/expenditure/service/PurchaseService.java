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
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public PurchaseService(PurchaseInvoiceRepository purchaseInvoiceRepository,
                           PayableRepository payableRepository,
                           BusinessPartnerPersistencePort businessPartnerPersistencePort,
                           MasterDataQueryPort masterDataQueryPort,
                           JournalPostingPort journalPostingPort) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.payableRepository = payableRepository;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    /**
     * 留ㅼ엯 ?몃낫?댁뒪瑜??앹꽦?섍퀬 ?대떦 留ㅼ엯梨꾨Т瑜??몄떇?섎ŉ, 留ㅼ엯 ?몄떇 ?꾪몴瑜??앹꽦?⑸땲??
     * 以묐났 ?몃낫?댁뒪 (嫄곕옒泥? ?몃낫?댁뒪踰덊샇)瑜?諛⑹??⑸땲??
     * @param invoice 留ㅼ엯 ?몃낫?댁뒪 ?뺣낫
     * @return ?앹꽦??留ㅼ엯 ?몃낫?댁뒪
     */
    public PurchaseInvoice createPurchaseInvoice(PurchaseInvoice invoice) {
        String vendorCode = invoice.getVendor().getBusinessPartnerCode();
        masterDataQueryPort.findBusinessPartner(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("怨듦툒?낆껜 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎: " + vendorCode));
        BusinessPartner vendor = businessPartnerPersistencePort.findByBusinessPartnerCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("怨듦툒?낆껜 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎: " + vendorCode));
        invoice.setVendor(vendor);

        // 以묐났 ?몃낫?댁뒪 ?뺤씤 (嫄곕옒泥?+ ?몃낫?댁뒪 踰덊샇)
        if (purchaseInvoiceRepository.findByInvoiceNoAndVendorBusinessPartnerCode(
                invoice.getInvoiceNo(), vendor.getBusinessPartnerCode()).isPresent()) {
            throw new IllegalArgumentException("?대? 議댁옱?섎뒗 ?몃낫?댁뒪 踰덊샇?낅땲??(怨듦툒?낆껜: " + vendor.getBusinessPartnerName() + ", ?몃낫?댁뒪 踰덊샇: " + invoice.getInvoiceNo() + ")");
        }


        // 湲곕낯 ?곹깭 ?ㅼ젙
        if (invoice.getStatus() == null) {
            invoice.setStatus(PurchaseInvoiceStatus.RECEIVED);
        }
        if (invoice.getCreatedBy() == null) {
            invoice.setCreatedBy("SYSTEM"); // ?먮뒗 ?꾩옱 濡쒓렇???ъ슜???뺣낫
        }

        PurchaseInvoice savedInvoice = purchaseInvoiceRepository.save(invoice);

        // 1. 留ㅼ엯梨꾨Т ?앹꽦 (Open Item)
        Payable payable = new Payable();
        payable.setPurchaseInvoiceNo(savedInvoice.getInvoiceNo());
        payable.setPurchaseInvoiceVendorCode(savedInvoice.getVendor().getBusinessPartnerCode());

        payable.setVendor(vendor);
        payable.setOriginalAmount(savedInvoice.getTotalAmount());
        payable.setOutstandingAmount(savedInvoice.getTotalAmount()); // 珥덇린?먮뒗 誘몄?湲됯툑 ?꾩븸
        payable.setDueDate(savedInvoice.getDueDate());
        payable.setStatus(PayableStatus.OPEN);
        payableRepository.save(payable);

        // 2. 留ㅼ엯 ?몄떇 ?꾪몴 ?앹꽦
        requireAccount("21100", "AccountSubject for Accounts Payable not found");
        requireAccount("50100", "AccountSubject for Expense not found");
        requireAccount("13500", "AccountSubject for VAT Receivable not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "留ㅼ엯 ?몄떇: " + invoice.getInvoiceNo() + " - " + invoice.getVendor().getBusinessPartnerName(),
                "PURCHASE_RECOGNITION",
                null,
                null,
                invoice.getCreatedBy(),
                invoice.getCreatedBy(),
                "PURCHASE_INVOICE",
                invoice.getInvoiceNo() + "_" + invoice.getVendor().getBusinessPartnerCode(),
                List.of(
                        new JournalLineCommand("DEBIT", "50100", invoice.getNetAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "?곹뭹 留ㅼ엯"),
                        new JournalLineCommand("DEBIT", "13500", invoice.getTaxAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "遺媛?몃?湲됯툑"),
                        new JournalLineCommand("CREDIT", "21100", invoice.getTotalAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "留ㅼ엯梨꾨Т 諛쒖깮"))));

        return savedInvoice;
    }

    /**
     * 留ㅼ엯梨꾨Т ?곹깭瑜??낅뜲?댄듃?⑸땲?? (?? ?곗껜 泥섎━)
     * @param asOfDate 湲곗??쇱옄
     */
    public void updatePayableStatus(LocalDate asOfDate) {
        // 留뚭린?쇱씠 吏??OPEN ?먮뒗 PARTIAL_PAID ?곹깭??梨꾨Т瑜?OVERDUE濡?蹂寃?
        List<Payable> overduePayables = payableRepository.findByDueDateBeforeAndStatusNot(asOfDate, PayableStatus.PAID);
        for (Payable payable : overduePayables) {
            if (payable.getDueDate().isBefore(asOfDate)) { // ?뺤떎??留뚭린?쇱씠 吏??寃쎌슦
                payable.setStatus(PayableStatus.OVERDUE);
                payableRepository.save(payable);

                // 愿??PurchaseInvoice??OVERDUE濡??낅뜲?댄듃 (遺遺?吏湲됰맂 寃쎌슦 ?쒖쇅 ??濡쒖쭅 異붽? 媛??
                if (payable.getPurchaseInvoice() != null &&
                    payable.getPurchaseInvoice().getStatus() != PurchaseInvoiceStatus.PARTIAL_PAID) { // 遺遺?吏湲됰맂 嫄댁? ?곕줈 愿由?
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
