package com.ho.account.income.service;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.income.domain.Collection;
import com.ho.account.income.domain.CollectionStatus;
import com.ho.account.income.domain.MatchingCriteria;
import com.ho.account.income.domain.MatchingRule;
import com.ho.account.income.domain.Receivable;
import com.ho.account.income.domain.ReceivableStatus;
import com.ho.account.income.domain.SalesInvoice;
import com.ho.account.income.domain.SalesInvoiceStatus;
import com.ho.account.income.domain.UnmatchedCollection;
import com.ho.account.income.domain.UnmatchedCollectionStatus;
import com.ho.account.income.repository.CollectionRepository;
import com.ho.account.income.repository.MatchingRuleRepository;
import com.ho.account.income.repository.ReceivableRepository;
import com.ho.account.income.repository.SalesInvoiceRepository;
import com.ho.account.income.repository.UnmatchedCollectionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ReceivableRepository receivableRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final MatchingRuleRepository matchingRuleRepository;
    private final UnmatchedCollectionRepository unmatchedCollectionRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;

    public CollectionService(CollectionRepository collectionRepository,
                             ReceivableRepository receivableRepository,
                             BusinessPartnerRepository businessPartnerRepository,
                             MasterDataQueryPort masterDataQueryPort,
                             JournalPostingPort journalPostingPort,
                             MatchingRuleRepository matchingRuleRepository,
                             UnmatchedCollectionRepository unmatchedCollectionRepository,
                             SalesInvoiceRepository salesInvoiceRepository) {
        this.collectionRepository = collectionRepository;
        this.receivableRepository = receivableRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.matchingRuleRepository = matchingRuleRepository;
        this.unmatchedCollectionRepository = unmatchedCollectionRepository;
        this.salesInvoiceRepository = salesInvoiceRepository;
    }

    public Collection receivePayment(Collection collection) {
        String customerCode = collection.getCustomer().getBusinessPartnerCode();
        masterDataQueryPort.findBusinessPartner(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerCode));
        BusinessPartner customer = businessPartnerRepository.findByBusinessPartnerCode(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerCode));
        collection.setCustomer(customer);

        if (collection.getStatus() == null) {
            collection.setStatus(CollectionStatus.RECEIVED);
        }

        Collection savedCollection = collectionRepository.save(collection);

        requireAccount("10100", "AccountSubject for Cash/Bank not found");
        requireAccount("21100", "AccountSubject for AR Clearing not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                collection.getCollectionDate(),
                collection.getCollectionDate(),
                "Collection received: " + customer.getBusinessPartnerName() + " - " + collection.getAmount(),
                "COLLECTION_RECOGNITION",
                null,
                null,
                "SYSTEM",
                "SYSTEM",
                "COLLECTION",
                savedCollection.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "10100", collection.getAmount(), null, null,
                                customer.getBusinessPartnerCode(), "Cash/Bank increase"),
                        new JournalLineCommand("CREDIT", "21100", collection.getAmount(), null, null,
                                customer.getBusinessPartnerCode(), "AR clearing increase"))));

        attemptAutoMatching(savedCollection);
        return savedCollection;
    }

    public void attemptAutoMatching(Collection collection) {
        if (collection.getStatus() != CollectionStatus.RECEIVED
                && collection.getStatus() != CollectionStatus.UNMATCHED) {
            return;
        }

        List<MatchingRule> activeRules = matchingRuleRepository.findByIsActiveOrderByPriorityAsc(true);
        boolean matched = false;
        String matchReason = "No matching rule applied.";

        for (MatchingRule rule : activeRules) {
            List<Receivable> openReceivables = receivableRepository.findByCustomerBusinessPartnerCodeAndStatus(
                    collection.getCustomer().getBusinessPartnerCode(), ReceivableStatus.OPEN);

            for (Receivable receivable : openReceivables) {
                if (applyMatchingRule(rule, collection, receivable)) {
                    processSuccessfulMatch(collection, receivable);
                    matched = true;
                    matchReason = "Matched by rule: " + rule.getRuleName();
                    break;
                }
            }

            if (matched) {
                break;
            }
        }

        if (matched) {
            unmatchedCollectionRepository.findByCollection(collection).stream().findFirst().ifPresent(unmatched -> {
                unmatched.setStatus(UnmatchedCollectionStatus.RESOLVED);
                unmatched.setResolvedBy("SYSTEM_AUTO_MATCH");
                unmatched.setResolvedAt(LocalDateTime.now());
                unmatchedCollectionRepository.save(unmatched);
            });
            return;
        }

        UnmatchedCollection unmatched = unmatchedCollectionRepository.findByCollection(collection).stream()
                .findFirst()
                .orElse(new UnmatchedCollection());
        unmatched.setCollection(collection);
        unmatched.setReason(matchReason);
        unmatched.setStatus(UnmatchedCollectionStatus.PENDING);
        unmatchedCollectionRepository.save(unmatched);

        collection.setStatus(CollectionStatus.UNMATCHED);
        collectionRepository.save(collection);
    }

    private boolean applyMatchingRule(MatchingRule rule, Collection collection, Receivable receivable) {
        MatchingCriteria criteria = rule.getMatchCriteria();
        return switch (criteria) {
            case REFERENCE_NO_EXACT -> collection.getReferenceNo() != null
                    && receivable.getSalesInvoice() != null
                    && collection.getReferenceNo().equals(receivable.getSalesInvoice().getInvoiceNo());
            case CUSTOMER_CODE -> true;
            case AMOUNT_EXACT -> collection.getAmount().compareTo(receivable.getOutstandingAmount()) == 0;
            case AMOUNT_FUZZY -> {
                BigDecimal tolerance = rule.getToleranceAmount() != null
                        ? rule.getToleranceAmount()
                        : BigDecimal.ZERO;
                BigDecimal diff = collection.getAmount().subtract(receivable.getOutstandingAmount()).abs();
                yield diff.compareTo(tolerance) <= 0;
            }
            case VIRTUAL_ACCOUNT -> false;
        };
    }

    private void processSuccessfulMatch(Collection collection, Receivable receivable) {
        receivable.setOutstandingAmount(receivable.getOutstandingAmount().subtract(collection.getAmount()));
        if (receivable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            receivable.setStatus(ReceivableStatus.PAID);
            receivable.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            receivable.setStatus(ReceivableStatus.PARTIAL_PAID);
        }
        receivableRepository.save(receivable);

        collection.setStatus(CollectionStatus.MATCHED);
        collectionRepository.save(collection);

        if (receivable.getSalesInvoice() != null) {
            SalesInvoice invoice = receivable.getSalesInvoice();
            if (receivable.getStatus() == ReceivableStatus.PAID) {
                invoice.setStatus(SalesInvoiceStatus.PAID);
            } else if (receivable.getStatus() == ReceivableStatus.PARTIAL_PAID) {
                invoice.setStatus(SalesInvoiceStatus.PARTIAL_PAID);
            }
            salesInvoiceRepository.save(invoice);
        }

        requireAccount("11100", "AccountSubject for Accounts Receivable not found");
        requireAccount("21100", "AccountSubject for AR Clearing not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                collection.getCollectionDate(),
                collection.getCollectionDate(),
                "Collection matched: " + collection.getCustomer().getBusinessPartnerName() + " - "
                        + collection.getAmount(),
                "COLLECTION_MATCHING",
                null,
                null,
                "SYSTEM_AUTO_MATCH",
                "SYSTEM_AUTO_MATCH",
                "COLLECTION_MATCH",
                collection.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", collection.getAmount(), null, null,
                                collection.getCustomer().getBusinessPartnerCode(), "AR clearing decrease"),
                        new JournalLineCommand("CREDIT", "11100", collection.getAmount(), null, null,
                                collection.getCustomer().getBusinessPartnerCode(), "Accounts receivable decrease"))));
    }

    public List<UnmatchedCollection> getUnmatchedCollections() {
        return unmatchedCollectionRepository.findByStatus(UnmatchedCollectionStatus.PENDING);
    }

    public Collection manualMatchCollection(Long collectionId, Long receivableId, BigDecimal matchingAmount) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found: " + collectionId));
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new IllegalArgumentException("Receivable not found: " + receivableId));

        if (matchingAmount.compareTo(BigDecimal.ZERO) <= 0
                || matchingAmount.compareTo(collection.getAmount()) > 0
                || matchingAmount.compareTo(receivable.getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("Invalid matching amount.");
        }

        processSuccessfulMatch(collection, receivable);

        unmatchedCollectionRepository.findByCollection(collection).stream().findFirst().ifPresent(unmatched -> {
            unmatched.setStatus(UnmatchedCollectionStatus.RESOLVED);
            unmatched.setResolvedBy("MANUAL_MATCH");
            unmatched.setResolvedAt(LocalDateTime.now());
            unmatchedCollectionRepository.save(unmatched);
        });

        if (matchingAmount.compareTo(collection.getAmount()) < 0) {
            Collection remainingCollection = new Collection();
            remainingCollection.setCollectionDate(collection.getCollectionDate());
            remainingCollection.setCustomer(collection.getCustomer());
            remainingCollection.setAmount(collection.getAmount().subtract(matchingAmount));
            remainingCollection.setBankAccount(collection.getBankAccount());
            remainingCollection.setVirtualAccount(collection.getVirtualAccount());
            remainingCollection.setReferenceNo(collection.getReferenceNo());
            remainingCollection.setStatus(CollectionStatus.UNMATCHED);
            collectionRepository.save(remainingCollection);

            UnmatchedCollection newUnmatched = new UnmatchedCollection();
            newUnmatched.setCollection(remainingCollection);
            newUnmatched.setReason("Remaining amount after partial match");
            newUnmatched.setStatus(UnmatchedCollectionStatus.PENDING);
            unmatchedCollectionRepository.save(newUnmatched);

            collection.setStatus(CollectionStatus.PARTIAL_MATCHED);
            collectionRepository.save(collection);
        }

        return collection;
    }

    public MatchingRule saveMatchingRule(MatchingRule rule) {
        return matchingRuleRepository.save(rule);
    }

    public List<MatchingRule> getAllActiveMatchingRules() {
        return matchingRuleRepository.findByIsActiveOrderByPriorityAsc(true);
    }

    @Transactional(readOnly = true)
    public Optional<Collection> findById(Long id) {
        return collectionRepository.findById(id);
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
