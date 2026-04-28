package com.ho.account.income.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.income.domain.Receivable;
import com.ho.account.income.domain.ReceivableStatus;
import com.ho.account.income.domain.SalesInvoice;
import com.ho.account.income.domain.SalesInvoiceStatus;
import com.ho.account.income.repository.ReceivableRepository;
import com.ho.account.income.repository.SalesInvoiceRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SalesService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final ReceivableRepository receivableRepository;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public SalesService(SalesInvoiceRepository salesInvoiceRepository,
                        ReceivableRepository receivableRepository,
                        BusinessPartnerPersistencePort businessPartnerPersistencePort,
                        MasterDataQueryPort masterDataQueryPort,
                        JournalPostingPort journalPostingPort) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.receivableRepository = receivableRepository;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    public SalesInvoice createSalesInvoice(SalesInvoice invoice) {
        String customerCode = invoice.getCustomer().getBusinessPartnerCode();
        
        // MasterDataQueryPort를 통해 정합성 확인
        masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다: " + customerCode));
        
        // 실제 엔티티 연결은 PersistencePort를 통해 수행
        BusinessPartner customer = businessPartnerPersistencePort.findByBusinessPartnerCode(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("고객 엔티티를 찾을 수 없습니다: " + customerCode));
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

        requireAccount("11100", "AccountSubject for Accounts Receivable not found");
        requireAccount("40100", "AccountSubject for Sales Revenue not found");
        requireAccount("22100", "AccountSubject for VAT Payable not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "매출 인식: " + invoice.getInvoiceNo(),
                "SALES_RECOGNITION",
                null,
                null,
                invoice.getCreatedBy(),
                invoice.getCreatedBy(),
                "SALES_INVOICE",
                savedInvoice.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "11100", invoice.getTotalAmount(), null, null,
                                customer.getBusinessPartnerCode(), "매출채권 발생"),
                        new JournalLineCommand("CREDIT", "40100", invoice.getNetAmount(), null, null,
                                customer.getBusinessPartnerCode(), "상품 매출"),
                        new JournalLineCommand("CREDIT", "22100", invoice.getTaxAmount(), null, null,
                                customer.getBusinessPartnerCode(), "부가가치세예수금"))));

        return savedInvoice;
    }

    public void updateReceivableStatus(LocalDate asOfDate) {
        List<Receivable> overdueReceivables =
                receivableRepository.findByDueDateBeforeAndStatusNot(asOfDate, ReceivableStatus.PAID);
        for (Receivable receivable : overdueReceivables) {
            receivable.setStatus(ReceivableStatus.OVERDUE);
            receivableRepository.save(receivable);

            if (receivable.getSalesInvoice() != null
                    && receivable.getSalesInvoice().getStatus() != SalesInvoiceStatus.PARTIAL_PAID) {
                receivable.getSalesInvoice().setStatus(SalesInvoiceStatus.OVERDUE);
                salesInvoiceRepository.save(receivable.getSalesInvoice());
            }
        }
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
