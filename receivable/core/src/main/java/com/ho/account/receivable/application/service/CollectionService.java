package com.ho.account.receivable.application.service;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.in.*;
import com.ho.account.receivable.application.port.out.CollectionPersistencePort;
import com.ho.account.receivable.application.port.out.CollectionAllocationPersistencePort;
import com.ho.account.receivable.application.port.out.CollectionMatchingPolicyPort;
import com.ho.account.receivable.application.port.out.ReceivableAccountMappingPort;
import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CollectionService implements CollectionUseCase {

    private final CollectionPersistencePort collectionPersistencePort;
    private final ReceivablePersistencePort receivablePersistencePort;
    private final SalesInvoicePersistencePort salesInvoicePersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final ReceivableAccountMappingPort receivableAccountMappingPort;
    private final CollectionMatchingPolicyPort collectionMatchingPolicyPort;
    private final CollectionAllocationPersistencePort collectionAllocationPersistencePort;
    private final AccountingPeriodStatusPort accountingPeriodStatusPort;

    public CollectionService(CollectionPersistencePort collectionPersistencePort,
                             ReceivablePersistencePort receivablePersistencePort,
                             SalesInvoicePersistencePort salesInvoicePersistencePort,
                             MasterDataQueryPort masterDataQueryPort,
                             JournalPostingPort journalPostingPort,
                             ReceivableAccountMappingPort receivableAccountMappingPort,
                             CollectionMatchingPolicyPort collectionMatchingPolicyPort,
                             CollectionAllocationPersistencePort collectionAllocationPersistencePort,
                             AccountingPeriodStatusPort accountingPeriodStatusPort) {
        this.collectionPersistencePort = collectionPersistencePort;
        this.receivablePersistencePort = receivablePersistencePort;
        this.salesInvoicePersistencePort = salesInvoicePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.receivableAccountMappingPort = receivableAccountMappingPort;
        this.collectionMatchingPolicyPort = collectionMatchingPolicyPort;
        this.collectionAllocationPersistencePort = collectionAllocationPersistencePort;
        this.accountingPeriodStatusPort = accountingPeriodStatusPort;
    }

    @Override
    public Collection receivePayment(CollectionCommand command) {
        // 멱등성 가드: 동일 referenceNo를 가진 수납 요청 재시도 시 중복 생성 및 중복 분개 방지
        if (command.referenceNo() != null && !command.referenceNo().isBlank()) {
            Optional<Collection> existing = collectionPersistencePort.findByReferenceNo(command.referenceNo());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        validateAccountingPeriodOpen(command.collectionDate());

        Collection collection = toCollection(command);
        String customerCode = collection.getCustomerCode();
        BusinessPartnerRef customer = validateCustomer(customerCode);

        if (collection.getStatus() == null) {
            collection.setStatus(CollectionStatus.RECEIVED);
        }

        Collection savedCollection = collectionPersistencePort.save(collection);

        // 1. 수납 인식 전표 발행 (Cash -> AR Clearing)
        postCollectionRecognitionJournal(savedCollection, customer);

        return savedCollection;
    }

    @Override
    public void attemptAutoMatching(Long collectionId) {
        Collection collection = collectionPersistencePort.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found: " + collectionId));

        validateAccountingPeriodOpen(collection.getCollectionDate());

        if (!collection.canMatch()) {
            return;
        }

        // 자동 매칭은 참조번호, 만기일 허용 범위, 금액을 차례로 확인하며 중복 후보는 수동 확인으로 남깁니다.
        List<Receivable> openReceivables = receivablePersistencePort.findOpenItemsByCustomerCode(
                collection.getCustomerCode());

        Receivable match = collectionMatchingPolicyPort.selectMatch(collection, openReceivables).orElse(null);

        if (match != null) {
            processMatch(collection, match, collection.getUnallocatedAmount());
        } else {
            collection.markAsUnmatched();
            collectionPersistencePort.save(collection);
        }
    }

    @Override
    public void manualMatchCollection(ManualMatchingCommand command) {
        Long collectionId = command.collectionId();
        Long receivableId = command.receivableId();
        BigDecimal amount = command.matchingAmount();
        Collection collection = collectionPersistencePort.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found: " + collectionId));

        validateAccountingPeriodOpen(collection.getCollectionDate());

        Receivable receivable = receivablePersistencePort.findById(receivableId)
                .orElseThrow(() -> new IllegalArgumentException("Receivable not found: " + receivableId));

        if (amount.compareTo(collection.getUnallocatedAmount()) > 0
                || amount.compareTo(receivable.getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("Invalid match amount.");
        }

        processMatch(collection, receivable, amount);
    }

    @Override
    public List<Collection> getUnmatchedCollections() {
        return collectionPersistencePort.findByUnmatched();
    }

    private void processMatch(Collection collection, Receivable receivable, BigDecimal amount) {
        // 자동 매칭 후보도 이 경로를 지나므로 고객 귀속을 상태 변경 전에 함께 검증합니다.
        String customerCode = collection.getCustomerCode();
        if (customerCode == null || customerCode.isBlank()
                || !customerCode.equals(receivable.getCustomerCode())) {
            throw new IllegalArgumentException("Collection and receivable customer codes must match.");
        }

        // DDD: 엔티티 내부 로직 호출
        receivable.applyCollection(amount);
        collection.applyAllocation(amount);

        receivablePersistencePort.save(receivable);
        collectionPersistencePort.save(collection);
        // 매칭 결과와 양쪽 잔액을 별도 이력으로 남겨 부분 매칭과 후속 매칭을 추적할 수 있습니다.
        collectionAllocationPersistencePort.save(CollectionAllocation.record(collection, receivable, amount));

        // 연관 인보이스 상태 업데이트
        if (receivable.getSalesInvoice() != null) {
            SalesInvoice si = receivable.getSalesInvoice();
            si.updateStatusFromReceivable(receivable.getStatus());
            salesInvoicePersistencePort.save(si);
        }

        // 매칭 전표 발행 (AR Clearing -> AR)
        BusinessPartnerRef customer = validateCustomer(collection.getCustomerCode());
        postMatchJournal(collection, receivable, amount, customer);
    }

    private Collection toCollection(CollectionCommand command) {
        Collection collection = new Collection();
        collection.setCollectionDate(command.collectionDate());
        collection.setCustomerCode(command.customerCode());
        collection.setAmount(command.amount());
        collection.setBankAccount(command.bankAccount());
        collection.setVirtualAccount(command.virtualAccount());
        collection.setReferenceNo(command.referenceNo());
        return collection;
    }

    private BusinessPartnerRef validateCustomer(String customerCode) {
        return masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer info missing: " + customerCode));
    }

    private void postCollectionRecognitionJournal(Collection collection, BusinessPartnerRef customer) {
        ReceivableAccountMappingPort.CollectionRecognitionAccounts accounts =
                receivableAccountMappingPort.resolveCollectionRecognitionAccounts(collection);
        requireAccounts(accounts.requiredAccountCodes());

        JournalEntryCommand command = new JournalEntryCommand(
                collection.getCollectionDate(),
                collection.getCollectionDate(),
                "Collection: " + customer.name() + " - " + collection.getAmount(),
                "COLLECTION_RECOGNITION",
                null, null, "SYSTEM", "SYSTEM",
                "COLLECTION", collection.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", accounts.cashAccountCode(), collection.getAmount(), null, null,
                                customer.code(), "Cash/Bank Increase"),
                        new JournalLineCommand("CREDIT", accounts.arClearingAccountCode(), collection.getAmount(), null, null,
                                customer.code(), "AR Clearing recognized")));

        validateJournalBalance(command);
        journalPostingPort.createDraftEntry(command);
    }

    private void postMatchJournal(Collection collection, Receivable receivable, BigDecimal amount, BusinessPartnerRef customer) {
        ReceivableAccountMappingPort.CollectionMatchAccounts accounts =
                receivableAccountMappingPort.resolveCollectionMatchAccounts(collection, receivable);
        requireAccounts(accounts.requiredAccountCodes());

        JournalEntryCommand command = new JournalEntryCommand(
                collection.getCollectionDate(),
                collection.getCollectionDate(),
                "Match: " + customer.name() + " - " + amount,
                "AR_CLEARING",
                null, null, "SYSTEM", "SYSTEM",
                "COLLECTION_MATCH", collection.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", accounts.arClearingAccountCode(), amount, null, null,
                                customer.code(), "AR Clearing decrease"),
                        new JournalLineCommand("CREDIT", accounts.accountsReceivableAccountCode(), amount, null, null,
                                customer.code(), "Accounts Receivable decrease")));

        validateJournalBalance(command);
        journalPostingPort.createDraftEntry(command);
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
     * 회계 시스템에서 마감(CLOSED) 처리된 회계기간에 수납(Collection) 및 수납 매칭 전표가 발행되는 것을 사전에 차단합니다.
     *
     * 1. 소급 마감 차단 (Anti-Backdating):
     *    이미 마감된 기간으로 수납 거래 및 매칭 전표를 소급 작성하는 것을 금지하여
     *    현금 흐름표, 외상매출금 잔액 및 반제 내역의 정합성을 보호합니다.
     * 2. 회계 내부 통제 이점 (Internal Control Benefits):
     *    수납 등록 및 매칭 처리 시 사전 검증(Fail-Closed)을 실시함으로써
     *    마감 기간에 대한 자금 입금/반제 무단 수정을 원천 차단하고 재무 통제의 완전성을 보장합니다.
     *
     * @param date 검증할 수납일자(Collection Date)
     * @throws IllegalStateException 해당 회계기간이 이미 마감(CLOSED)된 경우
     */
    private void validateAccountingPeriodOpen(LocalDate date) {
        if (accountingPeriodStatusPort.isClosed(date)) {
            throw new IllegalStateException("해당 회계 반영일(" + date + ")은 이미 마감된 기간입니다.");
        }
    }

    /**
     * 발행할 전표의 복식부기 대차평균(Equivalence of Debits and Credits) 균형을 사전 검증합니다.
     *
     * 🎓 [금융 회계 대차평균의 원리 및 원장 정합성 보장 - Double-Entry Bookkeeping Balance Validation]
     * 복식부기(Double-entry bookkeeping)의 핵심 원칙에 따라 모든 회계 전표는 차변(Debit) 합계와 대변(Credit) 합계가
     * 정확히 일치(Equivalence of Debits and Credits)해야 합니다.
     *
     * 1. 대차평균의 원리 (Equivalence of Debits and Credits):
     *    모든 거래는 차변과 대변에 동일한 금액으로 양방향 기록되어야 하며, 차변 합계와 대변 합계는 반드시 equal(compareTo == 0)이어야 합니다.
     * 2. 원장 정합성 보장 및 Fail-Closed 사전 차단:
     *    차대변 금액이 불일치하는 불평형 전표가 원장에 반영될 경우 총계정원장(General Ledger)의 대차 균형이 파괴되어
     *    시산표(Trial Balance) 및 재무제표(Financial Statements)의 심각한 오류와 왜곡을 유발합니다.
     *    전표 발행 직전 사전 검증을 통해 불평형 전표 발행을 원천 차단(Fail-Closed)함으로써 회계 데이터의 정합성과 내부 통제를 보장합니다.
     *
     * @param command 발행할 전표 데이터 (JournalEntryCommand)
     * @throws IllegalArgumentException 전표 라인이 없거나 차변 합계와 대변 합계가 일치하지 않을 경우
     */
    private void validateJournalBalance(JournalEntryCommand command) {
        if (command == null || command.lines() == null || command.lines().isEmpty()) {
            throw new IllegalArgumentException("전표 상세 라인이 존재하지 않습니다.");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (JournalLineCommand line : command.lines()) {
            if (line.amount() == null) {
                throw new IllegalArgumentException("전표 라인의 금액(amount)은 null일 수 없습니다.");
            }
            if ("DEBIT".equalsIgnoreCase(line.drcrType())) {
                debitTotal = debitTotal.add(line.amount());
            } else if ("CREDIT".equalsIgnoreCase(line.drcrType())) {
                creditTotal = creditTotal.add(line.amount());
            } else {
                throw new IllegalArgumentException("유효하지 않은 차대변 구분(drcrType)입니다: " + line.drcrType());
            }
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalArgumentException(
                    String.format("전표의 차변 합계(%s)와 대변 합계(%s)가 일치하지 않습니다.", debitTotal, creditTotal));
        }
    }
}
