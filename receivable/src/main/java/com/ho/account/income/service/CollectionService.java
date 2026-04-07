package com.ho.account.income.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
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
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.service.JournalService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final JournalService journalService;
    private final MatchingRuleRepository matchingRuleRepository;
    private final UnmatchedCollectionRepository unmatchedCollectionRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;

    public CollectionService(CollectionRepository collectionRepository,
                             ReceivableRepository receivableRepository,
                             BusinessPartnerRepository businessPartnerRepository,
                             AccountSubjectRepository accountSubjectRepository,
                             DepartmentRepository departmentRepository,
                             JournalService journalService,
                             MatchingRuleRepository matchingRuleRepository,
                             UnmatchedCollectionRepository unmatchedCollectionRepository,
                             SalesInvoiceRepository salesInvoiceRepository) {
        this.collectionRepository = collectionRepository;
        this.receivableRepository = receivableRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.journalService = journalService;
        this.matchingRuleRepository = matchingRuleRepository;
        this.unmatchedCollectionRepository = unmatchedCollectionRepository;
        this.salesInvoiceRepository = salesInvoiceRepository;
    }

    public Collection receivePayment(Collection collection) {
        BusinessPartner customer = businessPartnerRepository.findByBusinessPartnerCode(
                        collection.getCustomer().getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "고객 정보를 찾을 수 없습니다: " + collection.getCustomer().getBusinessPartnerCode()));
        collection.setCustomer(customer);

        if (collection.getStatus() == null) {
            collection.setStatus(CollectionStatus.RECEIVED);
        }

        Collection savedCollection = collectionRepository.save(collection);

        AccountSubject cashAccount = accountSubjectRepository.findById("10100")
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Cash/Bank not found"));
        AccountSubject arClearingAccount = accountSubjectRepository.findById("21100")
                .orElseThrow(() -> new IllegalStateException("AccountSubject for AR Clearing not found"));
        Department defaultDepartment = getOrCreateDefaultDepartment();

        JournalEntry collectionEntry = new JournalEntry();
        collectionEntry.setSlipDate(collection.getCollectionDate());
        collectionEntry.setAccountingDate(collection.getCollectionDate());
        collectionEntry.setDescription("수금 인식: "
                + collection.getCustomer().getBusinessPartnerName() + " - " + collection.getAmount());
        collectionEntry.setEntryType("COLLECTION_RECOGNITION");
        collectionEntry.setLineageSourceType("COLLECTION");
        collectionEntry.setLineageSourceId(savedCollection.getId().toString());
        collectionEntry.setCreatedBy("SYSTEM");
        collectionEntry.setStatus(JournalEntryStatus.DRAFT);

        JournalDetail debitCash = new JournalDetail();
        debitCash.setDrcrType("DEBIT");
        debitCash.setAccountSubject(cashAccount);
        debitCash.setAmount(collection.getAmount());
        debitCash.setDepartment(defaultDepartment);
        debitCash.setDetailDescription("현금/예금 증가");
        collectionEntry.addDetail(debitCash);

        JournalDetail creditArClearing = new JournalDetail();
        creditArClearing.setDrcrType("CREDIT");
        creditArClearing.setAccountSubject(arClearingAccount);
        creditArClearing.setAmount(collection.getAmount());
        creditArClearing.setDetailDescription("AR Clearing 계정 증가 (미매칭 수금)");
        collectionEntry.addDetail(creditArClearing);

        JournalEntry createdJournal = journalService.createJournalEntry(collectionEntry);
        savedCollection.setJournalEntry(createdJournal);
        collectionRepository.save(savedCollection);

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

        AccountSubject arAccount = accountSubjectRepository.findById("11100")
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Accounts Receivable not found"));
        AccountSubject arClearingAccount = accountSubjectRepository.findById("21100")
                .orElseThrow(() -> new IllegalStateException("AccountSubject for AR Clearing not found"));
        Department defaultDepartment = getOrCreateDefaultDepartment();

        JournalEntry matchingEntry = new JournalEntry();
        matchingEntry.setSlipDate(collection.getCollectionDate());
        matchingEntry.setAccountingDate(collection.getCollectionDate());
        matchingEntry.setDescription("수금 매칭: "
                + collection.getCustomer().getBusinessPartnerName() + " - " + collection.getAmount());
        matchingEntry.setEntryType("COLLECTION_MATCHING");
        matchingEntry.setLineageSourceType("COLLECTION_MATCH");
        matchingEntry.setLineageSourceId(collection.getId().toString());
        matchingEntry.setCreatedBy("SYSTEM_AUTO_MATCH");
        matchingEntry.setStatus(JournalEntryStatus.DRAFT);

        JournalDetail debitArClearing = new JournalDetail();
        debitArClearing.setDrcrType("DEBIT");
        debitArClearing.setAccountSubject(arClearingAccount);
        debitArClearing.setAmount(collection.getAmount());
        debitArClearing.setDepartment(defaultDepartment);
        debitArClearing.setDetailDescription("AR Clearing 계정 감소");
        matchingEntry.addDetail(debitArClearing);

        JournalDetail creditAr = new JournalDetail();
        creditAr.setDrcrType("CREDIT");
        creditAr.setAccountSubject(arAccount);
        creditAr.setAmount(collection.getAmount());
        creditAr.setDetailDescription("매출채권 감소");
        matchingEntry.addDetail(creditAr);

        journalService.createJournalEntry(matchingEntry);
    }

    public List<UnmatchedCollection> getUnmatchedCollections() {
        return unmatchedCollectionRepository.findByStatus(UnmatchedCollectionStatus.PENDING);
    }

    public Collection manualMatchCollection(Long collectionId, Long receivableId, BigDecimal matchingAmount) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("수금 정보를 찾을 수 없습니다: " + collectionId));
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new IllegalArgumentException("매출채권을 찾을 수 없습니다: " + receivableId));

        if (matchingAmount.compareTo(BigDecimal.ZERO) <= 0
                || matchingAmount.compareTo(collection.getAmount()) > 0
                || matchingAmount.compareTo(receivable.getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("유효하지 않은 매칭 금액입니다.");
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
            newUnmatched.setReason("부분 매칭 후 잔액");
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
