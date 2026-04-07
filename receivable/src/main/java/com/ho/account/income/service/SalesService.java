package com.ho.account.income.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
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
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SalesService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final ReceivableRepository receivableRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final JournalService journalService;

    public SalesService(SalesInvoiceRepository salesInvoiceRepository,
                        ReceivableRepository receivableRepository,
                        BusinessPartnerRepository businessPartnerRepository,
                        AccountSubjectRepository accountSubjectRepository,
                        DepartmentRepository departmentRepository,
                        JournalService journalService) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.receivableRepository = receivableRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.journalService = journalService;
    }

    public SalesInvoice createSalesInvoice(SalesInvoice invoice) {
        BusinessPartner customer = businessPartnerRepository.findByBusinessPartnerCode(
                        invoice.getCustomer().getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "고객 정보를 찾을 수 없습니다: " + invoice.getCustomer().getBusinessPartnerCode()));
        invoice.setCustomer(customer);

        if (invoice.getStatus() == null) {
            invoice.setStatus(SalesInvoiceStatus.ISSUED);
        }
        if (invoice.getCreatedBy() == null) {
            invoice.setCreatedBy("SYSTEM");
        }

        SalesInvoice savedInvoice = salesInvoiceRepository.save(invoice);

        Receivable receivable = new Receivable();
        receivable.setSalesInvoice(savedInvoice);
        receivable.setCustomer(customer);
        receivable.setOriginalAmount(savedInvoice.getTotalAmount());
        receivable.setOutstandingAmount(savedInvoice.getTotalAmount());
        receivable.setDueDate(savedInvoice.getDueDate());
        receivable.setStatus(ReceivableStatus.OPEN);
        receivableRepository.save(receivable);

        AccountSubject arAccount = accountSubjectRepository.findById("11100")
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Accounts Receivable not found"));
        AccountSubject salesRevenueAccount = accountSubjectRepository.findById("40100")
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Sales Revenue not found"));
        AccountSubject vatPayableAccount = accountSubjectRepository.findById("22100")
                .orElseThrow(() -> new IllegalStateException("AccountSubject for VAT Payable not found"));
        Department defaultDepartment = getOrCreateDefaultDepartment();

        JournalEntry salesEntry = new JournalEntry();
        salesEntry.setSlipDate(invoice.getIssueDate());
        salesEntry.setAccountingDate(invoice.getIssueDate());
        salesEntry.setDescription("매출 인식: " + invoice.getInvoiceNo());
        salesEntry.setEntryType("SALES_RECOGNITION");
        salesEntry.setLineageSourceType("SALES_INVOICE");
        salesEntry.setLineageSourceId(savedInvoice.getId().toString());
        salesEntry.setCreatedBy(invoice.getCreatedBy());
        salesEntry.setStatus(JournalEntryStatus.DRAFT);

        JournalDetail debitAr = new JournalDetail();
        debitAr.setDrcrType("DEBIT");
        debitAr.setAccountSubject(arAccount);
        debitAr.setAmount(invoice.getTotalAmount());
        debitAr.setDepartment(defaultDepartment);
        debitAr.setDetailDescription("매출채권 발생");
        salesEntry.addDetail(debitAr);

        JournalDetail creditSales = new JournalDetail();
        creditSales.setDrcrType("CREDIT");
        creditSales.setAccountSubject(salesRevenueAccount);
        creditSales.setAmount(invoice.getNetAmount());
        creditSales.setDetailDescription("상품 매출");
        salesEntry.addDetail(creditSales);

        JournalDetail creditVat = new JournalDetail();
        creditVat.setDrcrType("CREDIT");
        creditVat.setAccountSubject(vatPayableAccount);
        creditVat.setAmount(invoice.getTaxAmount());
        creditVat.setDetailDescription("부가가치세예수금");
        salesEntry.addDetail(creditVat);

        JournalEntry createdJournal = journalService.createJournalEntry(salesEntry);
        savedInvoice.setJournalEntry(createdJournal);
        salesInvoiceRepository.save(savedInvoice);

        return savedInvoice;
    }

    public void updateReceivableStatus(LocalDate asOfDate) {
        List<Receivable> overdueReceivables =
                receivableRepository.findByDueDateBeforeAndStatusNot(asOfDate, ReceivableStatus.PAID);
        for (Receivable receivable : overdueReceivables) {
            if (receivable.getDueDate().isBefore(asOfDate)) {
                receivable.setStatus(ReceivableStatus.OVERDUE);
                receivableRepository.save(receivable);

                if (receivable.getSalesInvoice() != null
                        && receivable.getSalesInvoice().getStatus() != SalesInvoiceStatus.PARTIAL_PAID) {
                    receivable.getSalesInvoice().setStatus(SalesInvoiceStatus.OVERDUE);
                    salesInvoiceRepository.save(receivable.getSalesInvoice());
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
