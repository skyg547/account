package com.ho.account.receivable.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.in.SalesUseCase;
import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 매출 인식 및 채권 생성을 담당하는 핵심 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 '회사의 수입'을 장부에 적는 일을 총괄합니다.
 * 1. 매출이 발생하면 인보이스(청구서)를 발행하고, 
 * 2. 동시에 "돈을 받아야 한다"는 채권(Receivable) 장부를 새로 만듭니다. 
 * 3. 마지막으로 회계 시스템(Journal Ledger)에 "매출이 이만큼 났으니 전표 끊어줘!"라고 요청하는 조율사 역할을 수행합니다.
 */
@Service
@Transactional
public class SalesService implements SalesUseCase {

    private final SalesInvoicePersistencePort salesInvoicePersistencePort;
    private final ReceivablePersistencePort receivablePersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public SalesService(SalesInvoicePersistencePort salesInvoicePersistencePort,
                        ReceivablePersistencePort receivablePersistencePort,
                        MasterDataQueryPort masterDataQueryPort,
                        JournalPostingPort journalPostingPort) {
        this.salesInvoicePersistencePort = salesInvoicePersistencePort;
        this.receivablePersistencePort = receivablePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    @Override
    public SalesInvoice createSalesInvoice(SalesInvoice invoice) {
        String customerCode = invoice.getCustomerCode();
        BusinessPartnerRef customer = validateCustomer(customerCode);

        // 1. SalesInvoice 저장
        SalesInvoice savedInvoice = salesInvoicePersistencePort.save(invoice);

        // 2. Receivable 생성
        Receivable receivable = new Receivable();
        receivable.setSalesInvoice(savedInvoice);
        receivable.setCustomerCode(customerCode);
        
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

    private BusinessPartnerRef validateCustomer(String customerCode) {
        return masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer info missing in master data: " + customerCode));
    }

    private void postSalesJournal(SalesInvoice invoice, BusinessPartnerRef customer) {
        // @todo Accounting policy: resolve AR/revenue/VAT accounts from product/customer/tax profile or JournalRuleEngine instead of fixed codes.
        requireAccount("11100", "Accounts Receivable account missing");
        requireAccount("40100", "Sales Revenue account missing");
        requireAccount("22100", "VAT Payable account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "Sales: " + invoice.getInvoiceNo() + " - " + customer.name(),
                "SALES_RECOGNITION",
                null, null, "SYSTEM", "SYSTEM",
                "SALES_INVOICE", invoice.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "11100", invoice.getTotalAmount(), null, null,
                                customer.code(), "Accounts Receivable"),
                        new JournalLineCommand("CREDIT", "40100", invoice.getNetAmount(), null, null,
                                customer.code(), "Sales Revenue"),
                        new JournalLineCommand("CREDIT", "22100", invoice.getTaxAmount(), null, null,
                                customer.code(), "Output VAT"))));
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
