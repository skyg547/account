package com.ho.account.receivable.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.in.CollectionUseCase;
import com.ho.account.receivable.application.port.out.CollectionPersistencePort;
import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CollectionService implements CollectionUseCase {

    private final CollectionPersistencePort collectionPersistencePort;
    private final ReceivablePersistencePort receivablePersistencePort;
    private final SalesInvoicePersistencePort salesInvoicePersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public CollectionService(CollectionPersistencePort collectionPersistencePort,
                             ReceivablePersistencePort receivablePersistencePort,
                             SalesInvoicePersistencePort salesInvoicePersistencePort,
                             MasterDataQueryPort masterDataQueryPort,
                             JournalPostingPort journalPostingPort) {
        this.collectionPersistencePort = collectionPersistencePort;
        this.receivablePersistencePort = receivablePersistencePort;
        this.salesInvoicePersistencePort = salesInvoicePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    @Override
    public Collection receivePayment(Collection collection) {
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

        if (!collection.canMatch()) {
            return;
        }

        // @todo Matching policy: exact amount-only matching ignores reference number, virtual account, due-date tolerance, and duplicate candidates.
        // 단순 자동 매칭 로직 (참조번호 기반)
        List<Receivable> openReceivables = receivablePersistencePort.findByCustomerCodeAndStatus(
                collection.getCustomerCode(), ReceivableStatus.OPEN);

        Receivable match = openReceivables.stream()
                .filter(r -> r.getOriginalAmount().compareTo(collection.getAmount()) == 0)
                .findFirst()
                .orElse(null);

        if (match != null) {
            processMatch(collection, match, collection.getAmount());
        } else {
            collection.markAsUnmatched();
            collectionPersistencePort.save(collection);
        }
    }

    @Override
    public void manualMatchCollection(Long collectionId, Long receivableId, BigDecimal amount) {
        Collection collection = collectionPersistencePort.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found: " + collectionId));
        Receivable receivable = receivablePersistencePort.findById(receivableId)
                .orElseThrow(() -> new IllegalArgumentException("Receivable not found: " + receivableId));

        if (amount.compareTo(collection.getAmount()) > 0 || amount.compareTo(receivable.getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("Invalid match amount.");
        }

        processMatch(collection, receivable, amount);
    }

    @Override
    public List<Collection> getUnmatchedCollections() {
        return collectionPersistencePort.findByUnmatched();
    }

    private void processMatch(Collection collection, Receivable receivable, BigDecimal amount) {
        // DDD: 엔티티 내부 로직 호출
        receivable.applyCollection(amount);
        
        if (amount.compareTo(collection.getAmount()) == 0) {
            collection.markAsMatched();
        } else {
            collection.markAsPartialMatched();
            // @todo Open-item consistency: persist residual collection/open receivable split explicitly instead of leaving partial matching implied.
            // 부분 매칭 시 잔액 처리 로직 (생략 가능 또는 별도 Collection 생성)
        }

        receivablePersistencePort.save(receivable);
        collectionPersistencePort.save(collection);

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

    private BusinessPartnerRef validateCustomer(String customerCode) {
        return masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer info missing: " + customerCode));
    }

    private void postCollectionRecognitionJournal(Collection collection, BusinessPartnerRef customer) {
        // @todo Accounting policy: replace hardcoded cash/clearing accounts with bank clearing policy or JournalRuleEngine mapping.
        requireAccount("10100", "Cash account missing");
        requireAccount("21100", "AR Clearing account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                collection.getCollectionDate(),
                collection.getCollectionDate(),
                "Collection: " + customer.name() + " - " + collection.getAmount(),
                "COLLECTION_RECOGNITION",
                null, null, "SYSTEM", "SYSTEM",
                "COLLECTION", collection.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "10100", collection.getAmount(), null, null,
                                customer.code(), "Cash/Bank Increase"),
                        new JournalLineCommand("CREDIT", "21100", collection.getAmount(), null, null,
                                customer.code(), "AR Clearing recognized"))));
    }

    private void postMatchJournal(Collection collection, Receivable receivable, BigDecimal amount, BusinessPartnerRef customer) {
        // @todo Accounting policy: replace hardcoded clearing/AR accounts with configured receivable clearing policy.
        requireAccount("21100", "AR Clearing account missing");
        requireAccount("11100", "Accounts Receivable account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                collection.getCollectionDate(),
                collection.getCollectionDate(),
                "Match: " + customer.name() + " - " + amount,
                "AR_CLEARING",
                null, null, "SYSTEM", "SYSTEM",
                "COLLECTION_MATCH", collection.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", amount, null, null,
                                customer.code(), "AR Clearing decrease"),
                        new JournalLineCommand("CREDIT", "11100", amount, null, null,
                                customer.code(), "Accounts Receivable decrease"))));
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
