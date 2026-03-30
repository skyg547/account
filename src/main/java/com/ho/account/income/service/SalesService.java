package com.ho.account.income.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.income.domain.Receivable;
import com.ho.account.income.domain.ReceivableStatus;
import com.ho.account.income.domain.SalesInvoice;
import com.ho.account.income.domain.SalesInvoiceStatus;
import com.ho.account.income.repository.ReceivableRepository;
import com.ho.account.income.repository.SalesInvoiceRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.service.JournalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List; // Added missing import

@Service
@Transactional
public class SalesService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final ReceivableRepository receivableRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final JournalService journalService;

    public SalesService(SalesInvoiceRepository salesInvoiceRepository,
                        ReceivableRepository receivableRepository,
                        BusinessPartnerRepository businessPartnerRepository,
                        AccountSubjectRepository accountSubjectRepository,
                        JournalService journalService) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.receivableRepository = receivableRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.journalService = journalService;
    }

    /**
     * 매출 인보이스를 생성하고 해당 매출채권을 인식하며, 매출 인식 전표를 생성합니다.
     * @param invoice 매출 인보이스 정보
     * @return 생성된 매출 인보이스
     */
    public SalesInvoice createSalesInvoice(SalesInvoice invoice) {
        // 고객 존재 여부 확인
        BusinessPartner customer = businessPartnerRepository.findByBusinessPartnerCode(invoice.getCustomer().getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다: " + invoice.getCustomer().getBusinessPartnerCode()));
        invoice.setCustomer(customer);

        // 기본 상태 설정
        if (invoice.getStatus() == null) {
            invoice.setStatus(SalesInvoiceStatus.ISSUED);
        }
        if (invoice.getCreatedBy() == null) {
            invoice.setCreatedBy("SYSTEM"); // 또는 현재 로그인 사용자 정보
        }

        SalesInvoice savedInvoice = salesInvoiceRepository.save(invoice);

        // 1. 매출채권 생성 (Open Item)
        Receivable receivable = new Receivable();
        receivable.setSalesInvoice(savedInvoice);
        receivable.setCustomer(customer);
        receivable.setOriginalAmount(savedInvoice.getTotalAmount());
        receivable.setOutstandingAmount(savedInvoice.getTotalAmount()); // 초기에는 미수금 전액
        receivable.setDueDate(savedInvoice.getDueDate());
        receivable.setStatus(ReceivableStatus.OPEN);
        receivableRepository.save(receivable);

        // 2. 매출 인식 전표 생성
        AccountSubject arAccount = accountSubjectRepository.findById("11100") // 매출채권 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Accounts Receivable not found"));
        AccountSubject salesRevenueAccount = accountSubjectRepository.findById("40100") // 매출 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Sales Revenue not found"));
        AccountSubject vatPayableAccount = accountSubjectRepository.findById("22100") // 부가세예수금 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for VAT Payable not found"));

        JournalEntry salesEntry = new JournalEntry();
        salesEntry.setSlipDate(invoice.getIssueDate());
        salesEntry.setAccountingDate(invoice.getIssueDate());
        salesEntry.setDescription("매출 인식: " + invoice.getInvoiceNo());
        salesEntry.setEntryType("SALES_RECOGNITION");
        salesEntry.setLineageSourceType("SALES_INVOICE");
        salesEntry.setLineageSourceId(savedInvoice.getId().toString());
        salesEntry.setCreatedBy(invoice.getCreatedBy());
        salesEntry.setStatus(JournalEntryStatus.DRAFT);

        // 차변: 매출채권 (총액)
        JournalDetail debitAr = new JournalDetail();
        debitAr.setDrcrType("DEBIT");
        debitAr.setAccountSubject(arAccount);
        debitAr.setAmount(invoice.getTotalAmount());
        debitAr.setDetailDescription("매출채권 발생");
        salesEntry.addDetail(debitAr);

        // 대변: 매출 (공급가액)
        JournalDetail creditSales = new JournalDetail();
        creditSales.setDrcrType("CREDIT");
        creditSales.setAccountSubject(salesRevenueAccount);
        creditSales.setAmount(invoice.getNetAmount());
        creditSales.setDetailDescription("상품 매출");
        salesEntry.addDetail(creditSales);

        // 대변: 부가세예수금 (세액)
        JournalDetail creditVat = new JournalDetail();
        creditVat.setDrcrType("CREDIT");
        creditVat.setAccountSubject(vatPayableAccount);
        creditVat.setAmount(invoice.getTaxAmount());
        creditVat.setDetailDescription("부가세예수금");
        salesEntry.addDetail(creditVat);

        JournalEntry createdJournal = journalService.createJournalEntry(salesEntry);
        savedInvoice.setJournalEntry(createdJournal); // 인보이스에 전표 연결
        salesInvoiceRepository.save(savedInvoice);

        return savedInvoice;
    }

    /**
     * 매출채권 상태를 업데이트합니다. (예: 연체 처리)
     * 실제로는 배치성으로 주기적으로 실행됩니다.
     * @param asOfDate 기준일자
     */
    public void updateReceivableStatus(LocalDate asOfDate) {
        // 만기일이 지난 OPEN 또는 PARTIAL_PAID 상태의 채권을 OVERDUE로 변경
        List<Receivable> overdueReceivables = receivableRepository.findByDueDateBeforeAndStatusNot(asOfDate, ReceivableStatus.PAID);
        for (Receivable receivable : overdueReceivables) {
            if (receivable.getDueDate().isBefore(asOfDate)) { // 확실히 만기일이 지난 경우
                receivable.setStatus(ReceivableStatus.OVERDUE);
                receivableRepository.save(receivable);

                // 관련 SalesInvoice도 OVERDUE로 업데이트 (부분 수금된 경우 제외 등 로직 추가 가능)
                if (receivable.getSalesInvoice() != null &&
                    receivable.getSalesInvoice().getStatus() != SalesInvoiceStatus.PARTIAL_PAID) { // 부분 수금된 건은 따로 관리
                    receivable.getSalesInvoice().setStatus(SalesInvoiceStatus.OVERDUE);
                    salesInvoiceRepository.save(receivable.getSalesInvoice());
                }
            }
        }
    }

    // 기타 매출 관련 비즈니스 로직 (취소, 수정 등) 추가 가능
}
