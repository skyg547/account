package com.ho.account.expenditure.service;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
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
     * 매입 인보이스를 생성하고 해당 매입채무를 인식하며, 매입 인식 전표를 생성합니다.
     * 중복 인보이스 (거래처, 인보이스번호)를 방지합니다.
     * @param invoice 매입 인보이스 정보
     * @return 생성된 매입 인보이스
     */
    public PurchaseInvoice createPurchaseInvoice(PurchaseInvoice invoice) {
        String vendorCode = invoice.getVendor().getBusinessPartnerCode();
        masterDataQueryPort.findBusinessPartner(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("공급업체 정보를 찾을 수 없습니다: " + vendorCode));
        BusinessPartner vendor = businessPartnerRepository.findByBusinessPartnerCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("공급업체 정보를 찾을 수 없습니다: " + vendorCode));
        invoice.setVendor(vendor);

        // 중복 인보이스 확인 (거래처 + 인보이스 번호)
        if (purchaseInvoiceRepository.findByInvoiceNoAndVendorBusinessPartnerCode(
                invoice.getInvoiceNo(), vendor.getBusinessPartnerCode()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 인보이스 번호입니다 (공급업체: " + vendor.getBusinessPartnerName() + ", 인보이스 번호: " + invoice.getInvoiceNo() + ")");
        }


        // 기본 상태 설정
        if (invoice.getStatus() == null) {
            invoice.setStatus(PurchaseInvoiceStatus.RECEIVED);
        }
        if (invoice.getCreatedBy() == null) {
            invoice.setCreatedBy("SYSTEM"); // 또는 현재 로그인 사용자 정보
        }

        PurchaseInvoice savedInvoice = purchaseInvoiceRepository.save(invoice);

        // 1. 매입채무 생성 (Open Item)
        Payable payable = new Payable();
        payable.setPurchaseInvoiceNo(savedInvoice.getInvoiceNo());
        payable.setPurchaseInvoiceVendorCode(savedInvoice.getVendor().getBusinessPartnerCode());

        payable.setVendor(vendor);
        payable.setOriginalAmount(savedInvoice.getTotalAmount());
        payable.setOutstandingAmount(savedInvoice.getTotalAmount()); // 초기에는 미지급금 전액
        payable.setDueDate(savedInvoice.getDueDate());
        payable.setStatus(PayableStatus.OPEN);
        payableRepository.save(payable);

        // 2. 매입 인식 전표 생성
        requireAccount("21100", "AccountSubject for Accounts Payable not found");
        requireAccount("50100", "AccountSubject for Expense not found");
        requireAccount("13500", "AccountSubject for VAT Receivable not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "매입 인식: " + invoice.getInvoiceNo() + " - " + invoice.getVendor().getBusinessPartnerName(),
                "PURCHASE_RECOGNITION",
                null,
                null,
                invoice.getCreatedBy(),
                invoice.getCreatedBy(),
                "PURCHASE_INVOICE",
                invoice.getInvoiceNo() + "_" + invoice.getVendor().getBusinessPartnerCode(),
                List.of(
                        new JournalLineCommand("DEBIT", "50100", invoice.getNetAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "상품 매입"),
                        new JournalLineCommand("DEBIT", "13500", invoice.getTaxAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "부가세대급금"),
                        new JournalLineCommand("CREDIT", "21100", invoice.getTotalAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "매입채무 발생"))));

        return savedInvoice;
    }

    /**
     * 매입채무 상태를 업데이트합니다. (예: 연체 처리)
     * @param asOfDate 기준일자
     */
    public void updatePayableStatus(LocalDate asOfDate) {
        // 만기일이 지난 OPEN 또는 PARTIAL_PAID 상태의 채무를 OVERDUE로 변경
        List<Payable> overduePayables = payableRepository.findByDueDateBeforeAndStatusNot(asOfDate, PayableStatus.PAID);
        for (Payable payable : overduePayables) {
            if (payable.getDueDate().isBefore(asOfDate)) { // 확실히 만기일이 지난 경우
                payable.setStatus(PayableStatus.OVERDUE);
                payableRepository.save(payable);

                // 관련 PurchaseInvoice도 OVERDUE로 업데이트 (부분 지급된 경우 제외 등 로직 추가 가능)
                if (payable.getPurchaseInvoice() != null &&
                    payable.getPurchaseInvoice().getStatus() != PurchaseInvoiceStatus.PARTIAL_PAID) { // 부분 지급된 건은 따로 관리
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
