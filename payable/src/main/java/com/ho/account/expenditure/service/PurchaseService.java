package com.ho.account.expenditure.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.expenditure.domain.*;
import com.ho.account.expenditure.repository.PayableRepository;
import com.ho.account.expenditure.repository.PurchaseInvoiceRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.service.JournalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PurchaseService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final PayableRepository payableRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final JournalService journalService;

    public PurchaseService(PurchaseInvoiceRepository purchaseInvoiceRepository,
                           PayableRepository payableRepository,
                           BusinessPartnerRepository businessPartnerRepository,
                           AccountSubjectRepository accountSubjectRepository,
                           DepartmentRepository departmentRepository,
                           JournalService journalService) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.payableRepository = payableRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.journalService = journalService;
    }

    /**
     * 매입 인보이스를 생성하고 해당 매입채무를 인식하며, 매입 인식 전표를 생성합니다.
     * 중복 인보이스 (거래처, 인보이스번호)를 방지합니다.
     * @param invoice 매입 인보이스 정보
     * @return 생성된 매입 인보이스
     */
    public PurchaseInvoice createPurchaseInvoice(PurchaseInvoice invoice) {
        // 공급업체 존재 여부 확인
        BusinessPartner vendor = businessPartnerRepository.findByBusinessPartnerCode(invoice.getVendor().getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("공급업체 정보를 찾을 수 없습니다: " + invoice.getVendor().getBusinessPartnerCode()));
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
        AccountSubject apAccount = accountSubjectRepository.findById("21100") // 매입채무 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Accounts Payable not found"));
        AccountSubject expenseAccount = accountSubjectRepository.findById("50100") // 상품매입 또는 비용 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Expense not found"));
        AccountSubject vatReceivableAccount = accountSubjectRepository.findById("13500") // 부가세대급금 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for VAT Receivable not found"));
        Department defaultDepartment = getOrCreateDefaultDepartment();

        JournalEntry purchaseEntry = new JournalEntry();
        purchaseEntry.setSlipDate(invoice.getIssueDate());
        purchaseEntry.setAccountingDate(invoice.getIssueDate());
        purchaseEntry.setDescription("매입 인식: " + invoice.getInvoiceNo() + " - " + invoice.getVendor().getBusinessPartnerName());
        purchaseEntry.setEntryType("PURCHASE_RECOGNITION");
        purchaseEntry.setLineageSourceType("PURCHASE_INVOICE");
        purchaseEntry.setLineageSourceId(invoice.getInvoiceNo() + "_" + invoice.getVendor().getBusinessPartnerCode()); // 복합키 사용
        purchaseEntry.setCreatedBy(invoice.getCreatedBy());
        purchaseEntry.setStatus(JournalEntryStatus.DRAFT);

        // 차변: 비용 (공급가액)
        JournalDetail debitExpense = new JournalDetail();
        debitExpense.setDrcrType("DEBIT");
        debitExpense.setAccountSubject(expenseAccount);
        debitExpense.setAmount(invoice.getNetAmount());
        debitExpense.setDepartment(defaultDepartment);
        debitExpense.setDetailDescription("상품 매입");
        purchaseEntry.addDetail(debitExpense);

        // 차변: 부가세대급금 (세액)
        JournalDetail debitVat = new JournalDetail();
        debitVat.setDrcrType("DEBIT");
        debitVat.setAccountSubject(vatReceivableAccount);
        debitVat.setAmount(invoice.getTaxAmount());
        debitVat.setDepartment(defaultDepartment);
        debitVat.setDetailDescription("부가세대급금");
        purchaseEntry.addDetail(debitVat);

        // 대변: 매입채무 (총액)
        JournalDetail creditAp = new JournalDetail();
        creditAp.setDrcrType("CREDIT");
        creditAp.setAccountSubject(apAccount);
        creditAp.setAmount(invoice.getTotalAmount());
        creditAp.setDetailDescription("매입채무 발생");
        purchaseEntry.addDetail(creditAp);

        JournalEntry createdJournal = journalService.createJournalEntry(purchaseEntry);
        savedInvoice.setJournalEntry(createdJournal); // 인보이스에 전표 연결
        purchaseInvoiceRepository.save(savedInvoice);

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

    private Department getOrCreateDefaultDepartment() {
        return departmentRepository.findByCode("DEFAULT")
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setCode("DEFAULT");
                    department.setName("Default Department");
                    return departmentRepository.save(department);
                });
    }
}
