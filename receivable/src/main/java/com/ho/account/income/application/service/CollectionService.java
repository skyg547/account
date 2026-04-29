package com.ho.account.income.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.income.application.port.in.CollectionUseCase;
import com.ho.account.income.application.port.out.CollectionPersistencePort;
import com.ho.account.income.application.port.out.ReceivablePersistencePort;
import com.ho.account.income.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.income.domain.*;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
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
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public CollectionService(CollectionPersistencePort collectionPersistencePort,
                             ReceivablePersistencePort receivablePersistencePort,
                             SalesInvoicePersistencePort salesInvoicePersistencePort,
                             BusinessPartnerPersistencePort businessPartnerPersistencePort,
                             MasterDataQueryPort masterDataQueryPort,
                             JournalPostingPort journalPostingPort) {
        this.collectionPersistencePort = collectionPersistencePort;
        this.receivablePersistencePort = receivablePersistencePort;
        this.salesInvoicePersistencePort = salesInvoicePersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    @Override
    public Collection receivePayment(Collection collection) {
        String customerCode = collection.getCustomer().getBusinessPartnerCode();
        validateCustomer(customerCode);

        BusinessPartner customer = businessPartnerPersistencePort.findByBusinessPartnerCode(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerCode));
        collection.setCustomer(customer);

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

        // 단순 자동 매칭 로직 (참조번호 기반)
        List<Receivable> openReceivables = receivablePersistencePort.findByCustomerCodeAndStatus(
                collection.getCustomer().getBusinessPartnerCode(), ReceivableStatus.OPEN);

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
            // 부분 매칭 시 잔액 처리 로직 (생략 가능 또는 별도 Collection 생성)
        }

        receivablePersistencePort.save(receivable);
        collectionPersistencePort.save(collection);

        // 연관 인보이스 상태 업데이트
        if (receivable.getSalesInvoice() != null) {
            SalesInvoice si = receivable.getSalesInvoice();
            si.setStatus(receivable.getStatus() == ReceivableStatus.PAID ? SalesInvoiceStatus.PAID : SalesInvoiceStatus.PARTIAL_PAID);
            salesInvoicePersistencePort.save(si);
        }

        // 매칭 전표 발행 (AR Clearing -> AR)
        postMatchJournal(collection, receivable, amount);
    }

    private void validateCustomer(String customerCode) {
        masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer info missing: " + customerCode));
    }

    private void postCollectionRecognitionJournal(Collection collection, BusinessPartner customer) {
        requireAccount("10100", "Cash account missing");
        requireAccount("21100", "AR Clearing account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                collection.getCollectionDate(),
                collection.getCollectionDate(),
                "Collection: " + customer.getBusinessPartnerName() + " - " + collection.getAmount(),
                "COLLECTION_RECOGNITION",
                null, null, "SYSTEM", "SYSTEM",
                "COLLECTION", collection.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "10100", collection.getAmount(), null, null,
                                customer.getBusinessPartnerCode(), "Cash/Bank Increase"),
                        new JournalLineCommand("CREDIT", "21100", collection.getAmount(), null, null,
                                customer.getBusinessPartnerCode(), "AR Clearing recognized"))));
    }

    private void postMatchJournal(Collection collection, Receivable receivable, BigDecimal amount) {
        requireAccount("21100", "AR Clearing account missing");
        requireAccount("11100", "Accounts Receivable account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                collection.getCollectionDate(),
                collection.getCollectionDate(),
                "Match: " + collection.getCustomer().getBusinessPartnerName() + " - " + amount,
                "AR_CLEARING",
                null, null, "SYSTEM", "SYSTEM",
                "COLLECTION_MATCH", collection.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", amount, null, null,
                                collection.getCustomer().getBusinessPartnerCode(), "AR Clearing decrease"),
                        new JournalLineCommand("CREDIT", "11100", amount, null, null,
                                collection.getCustomer().getBusinessPartnerCode(), "Accounts Receivable decrease"))));
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
