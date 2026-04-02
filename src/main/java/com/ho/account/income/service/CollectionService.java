package com.ho.account.income.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.income.domain.*;
import com.ho.account.income.repository.CollectionRepository;
import com.ho.account.income.repository.MatchingRuleRepository;
import com.ho.account.income.repository.ReceivableRepository;
import com.ho.account.income.repository.UnmatchedCollectionRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.service.JournalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime; // 누락된 import 추가
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ReceivableRepository receivableRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final JournalService journalService;
    private final MatchingRuleRepository matchingRuleRepository;
    private final UnmatchedCollectionRepository unmatchedCollectionRepository;

    public CollectionService(CollectionRepository collectionRepository,
                             ReceivableRepository receivableRepository,
                             BusinessPartnerRepository businessPartnerRepository,
                             AccountSubjectRepository accountSubjectRepository,
                             JournalService journalService,
                             MatchingRuleRepository matchingRuleRepository,
                             UnmatchedCollectionRepository unmatchedCollectionRepository) {
        this.collectionRepository = collectionRepository;
        this.receivableRepository = receivableRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.journalService = journalService;
        this.matchingRuleRepository = matchingRuleRepository;
        this.unmatchedCollectionRepository = unmatchedCollectionRepository;
    }

    /**
     * 고객으로부터 수금을 수신하고, 수금 내역을 기록하며 초기 전표를 생성합니다.
     * 초기에는 UnmatchedCollection으로 처리될 수 있습니다.
     * @param collection 수금 정보
     * @return 생성된 수금 내역
     */
    public Collection receivePayment(Collection collection) {
        BusinessPartner customer = businessPartnerRepository.findByBusinessPartnerCode(collection.getCustomer().getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("고객 정보를 찾을 수 없습니다: " + collection.getCustomer().getBusinessPartnerCode()));
        collection.setCustomer(customer);

        // 초기 상태 설정
        if (collection.getStatus() == null) {
            collection.setStatus(CollectionStatus.RECEIVED);
        }

        Collection savedCollection = collectionRepository.save(collection);

        // 1. 수금 인식 전표 생성 (차변: 현금/예금, 대변: 미매칭수금 또는 AR Clearing)
        AccountSubject cashAccount = accountSubjectRepository.findById("10100") // 현금 또는 예금 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Cash/Bank not found"));
        AccountSubject arClearingAccount = accountSubjectRepository.findById("21100") // AR Clearing 계정 (예시) - 임시 계정으로 사용
                .orElseThrow(() -> new IllegalStateException("AccountSubject for AR Clearing not found"));

        JournalEntry collectionEntry = new JournalEntry();
        collectionEntry.setSlipDate(collection.getCollectionDate());
        collectionEntry.setAccountingDate(collection.getCollectionDate());
        collectionEntry.setDescription("수금 인식: " + collection.getCustomer().getBusinessPartnerName() + " - " + collection.getAmount());
        collectionEntry.setEntryType("COLLECTION_RECOGNITION");
        collectionEntry.setLineageSourceType("COLLECTION");
        collectionEntry.setLineageSourceId(savedCollection.getId().toString());
        collectionEntry.setCreatedBy("SYSTEM");
        collectionEntry.setStatus(JournalEntryStatus.DRAFT);

        // 차변: 현금/예금
        JournalDetail debitCash = new JournalDetail();
        debitCash.setDrcrType("DEBIT");
        debitCash.setAccountSubject(cashAccount);
        debitCash.setAmount(collection.getAmount());
        debitCash.setDetailDescription("현금/예금 증가");
        collectionEntry.addDetail(debitCash);

        // 대변: AR Clearing
        JournalDetail creditArClearing = new JournalDetail();
        creditArClearing.setDrcrType("CREDIT");
        creditArClearing.setAccountSubject(arClearingAccount);
        creditArClearing.setAmount(collection.getAmount());
        creditArClearing.setDetailDescription("AR Clearing 계정 증가 (미매칭 수금)");
        collectionEntry.addDetail(creditArClearing);

        JournalEntry createdJournal = journalService.createJournalEntry(collectionEntry);
        savedCollection.setJournalEntry(createdJournal);
        collectionRepository.save(savedCollection);

        // 수신된 수금은 일단 자동 매칭 프로세스를 시도합니다.
        attemptAutoMatching(savedCollection);

        return savedCollection;
    }

    /**
     * 자동 매칭 규칙에 따라 수금 내역과 매출채권을 매칭합니다.
     * @param collection 자동 매칭을 시도할 수금 내역
     */
    public void attemptAutoMatching(Collection collection) {
        if (collection.getStatus() != CollectionStatus.RECEIVED && collection.getStatus() != CollectionStatus.UNMATCHED) {
            return; // 이미 매칭되었거나 처리된 수금은 다시 시도하지 않음
        }

        List<MatchingRule> activeRules = matchingRuleRepository.findByIsActiveOrderByPriorityAsc(true);
        boolean matched = false;
        String matchReason = "No matching rule applied.";

        for (MatchingRule rule : activeRules) {
            // 고객 코드가 일치하는 OPEN 상태의 매출채권을 찾습니다.
            List<Receivable> openReceivables = receivableRepository.findByCustomerBusinessPartnerCodeAndStatus(
                    collection.getCustomer().getBusinessPartnerCode(), ReceivableStatus.OPEN);
            // Partial Paid 도 고려할 수 있으나, 우선 OPEN만 대상으로 함

            for (Receivable receivable : openReceivables) {
                if (applyMatchingRule(rule, collection, receivable)) {
                    // 매칭 성공: 수금 및 채권 업데이트, 전표 생성/조정
                    processSuccessfulMatch(collection, receivable);
                    matched = true;
                    matchReason = "Matched by rule: " + rule.getRuleName();
                    break; // 하나의 수금은 하나의 채권에 매칭 (복수 채권 매칭 로직은 추후 확장)
                }
            }
            if (matched) break;
        }

        if (matched) {
            // UnmatchedCollection이 있다면 해결 처리
            unmatchedCollectionRepository.findByCollection(collection).ifPresent(uc -> {
                uc.setStatus(UnmatchedCollectionStatus.RESOLVED);
                uc.setResolvedBy("SYSTEM_AUTO_MATCH");
                uc.setResolvedAt(LocalDateTime.now());
                unmatchedCollectionRepository.save(uc);
            });
        } else {
            // 매칭 실패: UnmatchedCollection으로 기록 (미매칭 큐)
            UnmatchedCollection unmatched = unmatchedCollectionRepository.findByCollection(collection)
                    .orElse(new UnmatchedCollection());
            unmatched.setCollection(collection);
            unmatched.setReason(matchReason);
            unmatched.setStatus(UnmatchedCollectionStatus.PENDING);
            unmatchedCollectionRepository.save(unmatched);

            collection.setStatus(CollectionStatus.UNMATCHED);
            collectionRepository.save(collection);
        }
    }

    /**
     * 특정 매칭 규칙을 수금과 매출채권에 적용합니다.
     * @param rule 적용할 매칭 규칙
     * @param collection 수금 정보
     * @param receivable 매출채권 정보
     * @return 매칭 성공 여부
     */
    private boolean applyMatchingRule(MatchingRule rule, Collection collection, Receivable receivable) {
        switch (rule.getMatchCriteria()) {
            case REFERENCE_NO_EXACT:
                // 수금의 참조 번호와 인보이스 번호가 일치하는 경우
                return collection.getReferenceNo() != null &&
                       receivable.getSalesInvoice() != null &&
                       collection.getReferenceNo().equals(receivable.getSalesInvoice().getInvoiceNo());
            case CUSTOMER_CODE:
                // 고객 코드는 이미 필터링 되었으므로, 항상 true (단독 규칙으로 의미 없음)
                return true;
            case AMOUNT_EXACT:
                // 수금 금액과 채권 미수금이 정확히 일치하는 경우
                return collection.getAmount().compareTo(receivable.getOutstandingAmount()) == 0;
            case AMOUNT_FUZZY:
                // 수금 금액과 채권 미수금이 허용 오차 범위 내에서 일치하는 경우
                BigDecimal tolerance = rule.getToleranceAmount() != null ? rule.getToleranceAmount() : BigDecimal.ZERO;
                BigDecimal diff = collection.getAmount().subtract(receivable.getOutstandingAmount()).abs();
                return diff.compareTo(tolerance) <= 0;
            case VIRTUAL_ACCOUNT:
                // 가상 계좌가 일치하는 경우 (여기서는 SalesInvoice와 직접 연결이 어려울 수 있으므로 확장 필요)
                // 현재는 Collection에만 virtualAccount가 있으므로, Receivable에서는 매칭 불가.
                // 향후 SalesInvoice 또는 Customer에 가상계좌 정보가 있다면 사용 가능.
                return false; // 임시로 false
            default:
                return false;
        }
    }

    /**
     * 매칭 성공 시 수금 및 채권을 업데이트하고 전표를 조정합니다.
     * @param collection 매칭된 수금
     * @param receivable 매칭된 매출채권
     */
    private void processSuccessfulMatch(Collection collection, Receivable receivable) {
        // 채권 미수금 감소
        receivable.setOutstandingAmount(receivable.getOutstandingAmount().subtract(collection.getAmount()));
        if (receivable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            receivable.setStatus(ReceivableStatus.PAID);
            receivable.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            receivable.setStatus(ReceivableStatus.PARTIAL_PAID);
        }
        receivableRepository.save(receivable);

        // 수금 상태 업데이트
        collection.setStatus(CollectionStatus.MATCHED);
        collectionRepository.save(collection);

        // 관련 SalesInvoice 상태 업데이트 (부분 수금 또는 전액 수금)
        if (receivable.getSalesInvoice() != null) {
            SalesInvoice invoice = receivable.getSalesInvoice();
            if (receivable.getStatus() == ReceivableStatus.PAID) {
                invoice.setStatus(SalesInvoiceStatus.PAID);
            } else if (receivable.getStatus() == ReceivableStatus.PARTIAL_PAID) {
                invoice.setStatus(SalesInvoiceStatus.PARTIAL_PAID);
            }
            // else if (invoice.getStatus() == SalesInvoiceStatus.OVERDUE) { // 연체 상태 유지하거나 해제 로직 추가
            // }
            salesInvoiceRepository.save(invoice);
        }


        // 전표 조정 (AR Clearing 차변, 매출채권 대변)
        AccountSubject arAccount = accountSubjectRepository.findById("11100") // 매출채권 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Accounts Receivable not found"));
        AccountSubject arClearingAccount = accountSubjectRepository.findById("21100") // AR Clearing 계정 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for AR Clearing not found"));

        JournalEntry matchingEntry = new JournalEntry();
        matchingEntry.setSlipDate(collection.getCollectionDate());
        matchingEntry.setAccountingDate(collection.getCollectionDate());
        matchingEntry.setDescription("수금 매칭: " + collection.getCustomer().getBusinessPartnerName() + " - " + collection.getAmount());
        matchingEntry.setEntryType("COLLECTION_MATCHING");
        matchingEntry.setLineageSourceType("COLLECTION_MATCH");
        matchingEntry.setLineageSourceId(collection.getId().toString());
        matchingEntry.setCreatedBy("SYSTEM_AUTO_MATCH");
        matchingEntry.setStatus(JournalEntryStatus.DRAFT);

        // 차변: AR Clearing 계정 감소
        JournalDetail debitArClearing = new JournalDetail();
        debitArClearing.setDrcrType("DEBIT");
        debitArClearing.setAccountSubject(arClearingAccount);
        debitArClearing.setAmount(collection.getAmount());
        debitArClearing.setDetailDescription("AR Clearing 계정 감소");
        matchingEntry.addDetail(debitArClearing);

        // 대변: 매출채권 감소
        JournalDetail creditAr = new JournalDetail();
        creditAr.setDrcrType("CREDIT");
        creditAr.setAccountSubject(arAccount);
        creditAr.setAmount(collection.getAmount());
        creditAr.setDetailDescription("매출채권 감소");
        matchingEntry.addDetail(creditAr);

        journalService.createJournalEntry(matchingEntry);
    }

    /**
     * 미매칭 수금 목록을 조회합니다. (미매칭 큐 운영 화면용)
     * @return 미매칭 상태의 수금 목록
     */
    public List<UnmatchedCollection> getUnmatchedCollections() {
        return unmatchedCollectionRepository.findByStatus(UnmatchedCollectionStatus.PENDING);
    }

    /**
     * 미매칭 수금을 수동으로 매출채권에 매칭 처리합니다.
     * (더 복잡한 로직이 필요하며, 현재는 1:1 매칭으로 가정)
     * @param collectionId 수금 ID
     * @param receivableId 매출채권 ID
     * @param matchingAmount 매칭 금액
     * @return 매칭된 수금
     */
    public Collection manualMatchCollection(Long collectionId, Long receivableId, BigDecimal matchingAmount) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("수금을 찾을 수 없습니다: " + collectionId));
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new IllegalArgumentException("매출채권을 찾을 수 없습니다: " + receivableId));

        if (matchingAmount.compareTo(BigDecimal.ZERO) <= 0 ||
            matchingAmount.compareTo(collection.getAmount()) > 0 ||
            matchingAmount.compareTo(receivable.getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("유효하지 않은 매칭 금액입니다.");
        }

        // 매칭 처리
        processSuccessfulMatch(collection, receivable); // 로직 재사용

        // UnmatchedCollection이 있다면 해결 처리
        unmatchedCollectionRepository.findByCollection(collection).ifPresent(uc -> {
            uc.setStatus(UnmatchedCollectionStatus.RESOLVED);
            uc.setResolvedBy("MANUAL_MATCH");
            uc.setResolvedAt(LocalDateTime.now());
            unmatchedCollectionRepository.save(uc);
        });

        // 잔액 처리 (부분 매칭)
        if (matchingAmount.compareTo(collection.getAmount()) < 0) {
            // 수금이 부분 매칭된 경우, 잔액은 다시 UnmatchedCollection으로
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


    /**
     * 매칭 규칙을 생성/업데이트 합니다.
     * @param rule 매칭 규칙 정보
     * @return 저장된 매칭 규칙
     */
    public MatchingRule saveMatchingRule(MatchingRule rule) {
        return matchingRuleRepository.save(rule);
    }

    /**
     * 모든 활성 매칭 규칙을 조회합니다.
     * @return 활성 매칭 규칙 목록
     */
    public List<MatchingRule> getAllActiveMatchingRules() {
        return matchingRuleRepository.findByIsActiveOrderByPriorityAsc(true);
    }

    /**
     * 특정 ID로 수금 내역을 조회합니다.
     * @param id 조회할 수금 ID
     * @return 수금 엔티티
     */
    @Transactional(readOnly = true)
    public Optional<Collection> findById(Long id) {
        return collectionRepository.findById(id);
    }

    // TODO: 연체/대손/손상 연계 로직 (정책 범위) - SalesService 또는 별도 서비스에서 관리
    // TODO: 에이징 리포트 표준 - ReportingService 또는 ReceivableService에서 쿼리 로직 구현
}
