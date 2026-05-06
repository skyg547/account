package com.ho.account.income.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.income.application.port.in.SalesUseCase;
import com.ho.account.income.application.port.out.ReceivablePersistencePort;
import com.ho.account.income.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.income.domain.*;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SalesService implements SalesUseCase {

    private final SalesInvoicePersistencePort salesInvoicePersistencePort;
    private final ReceivablePersistencePort receivablePersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public SalesService(SalesInvoicePersistencePort salesInvoicePersistencePort,
                        ReceivablePersistencePort receivablePersistencePort,
                        BusinessPartnerPersistencePort businessPartnerPersistencePort,
                        MasterDataQueryPort masterDataQueryPort,
                        JournalPostingPort journalPostingPort) {
        this.salesInvoicePersistencePort = salesInvoicePersistencePort;
        this.receivablePersistencePort = receivablePersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    @Override
    public SalesInvoice createSalesInvoice(SalesInvoice invoice) {
        String customerCode = invoice.getCustomerCode();
        validateCustomer(customerCode);

        // 1. SalesInvoice 저장
        SalesInvoice savedInvoice = salesInvoicePersistencePort.save(invoice);

        // 2. Receivable 생성
        Receivable receivable = new Receivable();
        receivable.setSalesInvoice(savedInvoice);
        // Receivable 엔티티도 ID 기반 참조로 변경하는 것이 좋으나, 현재는 엔티티 정의를 확인 후 필요시 수정
        // 일단은 기존 구조 유지하되 invoice에서 정보를 가져옴
        BusinessPartner customer = businessPartnerPersistencePort.findByBusinessPartnerCode(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerCode));
        receivable.setCustomer(customer);
        
        receivable.setOriginalAmount(savedInvoice.getTotalAmount());
        receivable.setOutstandingAmount(savedInvoice.getTotalAmount());
        receivable.setDueDate(savedInvoice.getDueDate());
        receivable.setStatus(ReceivableStatus.OPEN);
        receivablePersistencePort.save(receivable);

        // 3. 매출 인식 전표 발행
        postSalesJournal(savedInvoice, customer);

        return savedInvoice;
    }

    @Override
    public void updateReceivableStatus(LocalDate asOfDate) {
        List<Receivable> overdueReceivables = receivablePersistencePort.findByDueDateBeforeAndStatusNot(asOfDate, ReceivableStatus.PAID);
        for (Receivable receivable : overdueReceivables) {
            receivable.markAsOverdue();
            receivablePersistencePort.save(receivable);

            if (receivable.getSalesInvoice() != null) {
                SalesInvoice si = receivable.getSalesInvoice();
                if (si.getStatus() != SalesInvoiceStatus.PARTIAL_PAID) {
                    si.markAsOverdue();
                    salesInvoicePersistencePort.save(si);
                }
            }
        }
    }

    private void validateCustomer(String customerCode) {
        masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer info missing in master data: " + customerCode));
    }

    private void postSalesJournal(SalesInvoice invoice, BusinessPartner customer) {
        requireAccount("11100", "Accounts Receivable account missing");
        requireAccount("40100", "Sales Revenue account missing");
        requireAccount("22100", "VAT Payable account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "Sales: " + invoice.getInvoiceNo() + " - " + customer.getBusinessPartnerName(),
                "SALES_RECOGNITION",
                null, null, "SYSTEM", "SYSTEM",
                "SALES_INVOICE", invoice.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "11100", invoice.getTotalAmount(), null, null,
                                customer.getBusinessPartnerCode(), "Accounts Receivable"),
                        new JournalLineCommand("CREDIT", "40100", invoice.getNetAmount(), null, null,
                                customer.getBusinessPartnerCode(), "Sales Revenue"),
                        new JournalLineCommand("CREDIT", "22100", invoice.getTaxAmount(), null, null,
                                customer.getBusinessPartnerCode(), "Output VAT"))));
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
