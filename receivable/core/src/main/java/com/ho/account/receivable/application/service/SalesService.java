package com.ho.account.receivable.application.service;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.in.SalesInvoiceCommand;
import com.ho.account.receivable.application.port.in.SalesUseCase;
import com.ho.account.receivable.application.port.out.ReceivableAccountMappingPort;
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
    private final ReceivableAccountMappingPort receivableAccountMappingPort;
    private final AccountingPeriodStatusPort accountingPeriodStatusPort;

    public SalesService(SalesInvoicePersistencePort salesInvoicePersistencePort,
                        ReceivablePersistencePort receivablePersistencePort,
                        MasterDataQueryPort masterDataQueryPort,
                        JournalPostingPort journalPostingPort,
                        ReceivableAccountMappingPort receivableAccountMappingPort,
                        AccountingPeriodStatusPort accountingPeriodStatusPort) {
        this.salesInvoicePersistencePort = salesInvoicePersistencePort;
        this.receivablePersistencePort = receivablePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.receivableAccountMappingPort = receivableAccountMappingPort;
        this.accountingPeriodStatusPort = accountingPeriodStatusPort;
    }

    @Override
    public SalesInvoice createSalesInvoice(SalesInvoiceCommand command) {
        validateAccountingPeriodOpen(command.issueDate());

        SalesInvoice invoice = toInvoice(command);
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

    private SalesInvoice toInvoice(SalesInvoiceCommand command) {
        return SalesInvoice.create(
                command.invoiceNo(),
                command.customerCode(),
                command.issueDate(),
                command.dueDate(),
                command.netAmount(),
                command.taxAmount(),
                command.createdBy(),
                command.description());
    }

    private BusinessPartnerRef validateCustomer(String customerCode) {
        return masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer info missing in master data: " + customerCode));
    }

    private void postSalesJournal(SalesInvoice invoice, BusinessPartnerRef customer) {
        ReceivableAccountMappingPort.SalesRecognitionAccounts accounts =
                receivableAccountMappingPort.resolveSalesRecognitionAccounts(invoice);
        requireAccounts(accounts.requiredAccountCodes());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "Sales: " + invoice.getInvoiceNo() + " - " + customer.name(),
                "SALES_RECOGNITION",
                null, null, invoice.getCreatedBy(), invoice.getCreatedBy(),
                "SALES_INVOICE", invoice.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", accounts.accountsReceivableAccountCode(), invoice.getTotalAmount(), null, null,
                                customer.code(), "Accounts Receivable"),
                        new JournalLineCommand("CREDIT", accounts.revenueAccountCode(), invoice.getNetAmount(), null, null,
                                customer.code(), "Sales Revenue"),
                        new JournalLineCommand("CREDIT", accounts.outputVatAccountCode(), invoice.getTaxAmount(), null, null,
                                customer.code(), "Output VAT"))));
    }

    private void requireAccounts(List<String> accountCodes) {
        for (String accountCode : accountCodes) {
            masterDataQueryPort.findAccountSubject(accountCode)
                    .orElseThrow(() -> new IllegalStateException("Account missing: " + accountCode));
        }
    }

    /**
     * 회계기간 마감 여부를 사전에 검증합니다.
     *
     * 🎓 [금융 회계 내부 통제 및 마감 정합성 - Accounting Period Controls]
     * 회계 시스템에서 마감(CLOSED) 처리된 회계기간에 매출 인보이스 및 매출 인식 전표가 작성되는 것을 사전에 차단합니다.
     *
     * 1. 소급 마감 차단 (Anti-Backdating):
     *    마감된 과거 회계기간으로 매출 인보이스를 소급 발행하면 확정된 매출 수익과 매출채권 금액이 변동되어
     *    재무제표의 신뢰성 훼손 및 공시/세무 신고의 오류를 유발합니다.
     * 2. 회계 내부 통제 이점 (Internal Control Benefits):
     *    매출 인식 및 채권 생성 전 회계기간 마감 여부를 사전 검증(Fail-Closed)하여
     *    회계 장부의 마감 정합성을 유지하고 매출 조작 및 무단 전표 생성을 예방합니다.
     *
     * @param date 검증할 매출 인보이스 발행일자(Issue Date)
     * @throws IllegalStateException 해당 회계기간이 이미 마감(CLOSED)된 경우
     */
    private void validateAccountingPeriodOpen(LocalDate date) {
        if (accountingPeriodStatusPort.isClosed(date)) {
            throw new IllegalStateException("해당 회계 반영일(" + date + ")은 이미 마감된 기간입니다.");
        }
    }
}
