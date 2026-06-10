package com.ho.account.receivable.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.in.CollectionUseCase;
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
import java.util.List;

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

    public CollectionService(CollectionPersistencePort collectionPersistencePort,
                             ReceivablePersistencePort receivablePersistencePort,
                             SalesInvoicePersistencePort salesInvoicePersistencePort,
                             MasterDataQueryPort masterDataQueryPort,
                             JournalPostingPort journalPostingPort,
                             ReceivableAccountMappingPort receivableAccountMappingPort,
                             CollectionMatchingPolicyPort collectionMatchingPolicyPort,
                             CollectionAllocationPersistencePort collectionAllocationPersistencePort) {
        this.collectionPersistencePort = collectionPersistencePort;
        this.receivablePersistencePort = receivablePersistencePort;
        this.salesInvoicePersistencePort = salesInvoicePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.receivableAccountMappingPort = receivableAccountMappingPort;
        this.collectionMatchingPolicyPort = collectionMatchingPolicyPort;
        this.collectionAllocationPersistencePort = collectionAllocationPersistencePort;
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
    public void manualMatchCollection(Long collectionId, Long receivableId, BigDecimal amount) {
        Collection collection = collectionPersistencePort.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found: " + collectionId));
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

    private BusinessPartnerRef validateCustomer(String customerCode) {
        return masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer info missing: " + customerCode));
    }

    private void postCollectionRecognitionJournal(Collection collection, BusinessPartnerRef customer) {
        ReceivableAccountMappingPort.CollectionRecognitionAccounts accounts =
                receivableAccountMappingPort.resolveCollectionRecognitionAccounts(collection);
        requireAccounts(accounts.requiredAccountCodes());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
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
                                customer.code(), "AR Clearing recognized"))));
    }

    private void postMatchJournal(Collection collection, Receivable receivable, BigDecimal amount, BusinessPartnerRef customer) {
        ReceivableAccountMappingPort.CollectionMatchAccounts accounts =
                receivableAccountMappingPort.resolveCollectionMatchAccounts(collection, receivable);
        requireAccounts(accounts.requiredAccountCodes());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
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
                                customer.code(), "Accounts Receivable decrease"))));
    }

    private void requireAccounts(List<String> accountCodes) {
        for (String accountCode : accountCodes) {
            masterDataQueryPort.findAccountSubject(accountCode)
                    .orElseThrow(() -> new IllegalStateException("Account missing: " + accountCode));
        }
    }
}
