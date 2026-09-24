package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 전기 서비스 (Posting Service) — 전표를 원장에 반영합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 전기(Posting)란 승인된 '전표(영수증)'의 내용을 바탕으로 실제 '총계정원장(GL)'과 '보조원장(SL)'이라는 큰 장부에 기록을 옮겨 적는 행위입니다.
 * 이 클래스는 전표 승인 이후, 상세 라인을 불변 {@code GeneralLedger} Aggregate로 승격하고
 * 원장 저장과 잔액 갱신을 조정하는 중추적인 역할을 합니다.
 *
 * GL/SL 영속성 엔티티 조립은 출력 adapter에 위임하며, 서비스는 도메인 Aggregate 생성과
 * 저장·잔액 갱신 순서를 조정하는 업무 흐름에 집중합니다.
 */
@Service
public class PostingService {

    private static final JournalReversalPersistencePort UNAVAILABLE_REVERSAL_PERSISTENCE =
            new JournalReversalPersistencePort() {
                @Override
                public JournalReversalOperation save(JournalReversalOperation operation) {
                    throw new IllegalStateException("역분개 작업 영속성이 구성되지 않았습니다.");
                }

                @Override
                public Optional<JournalReversalOperation> findByOriginalJournalEntryId(Long originalJournalEntryId) {
                    return Optional.empty();
                }

                @Override
                public Optional<JournalReversalOperation> findByReversalJournalEntryId(Long reversalJournalEntryId) {
                    return Optional.empty();
                }
            };

    private final JournalPersistencePort journalPersistencePort;
    private final JournalReversalPersistencePort journalReversalPersistencePort;
    private final LedgerEntryPersistencePort ledgerEntryPersistencePort;
    private final LedgerService ledgerService;
    private final ClosingLockValidationFilter closingLockValidationFilter;

    /** Production wiring uses the durable reversal operation port. */
    @Autowired
    public PostingService(
            JournalPersistencePort journalPersistencePort,
            JournalReversalPersistencePort journalReversalPersistencePort,
            LedgerEntryPersistencePort ledgerEntryPersistencePort,
            LedgerService ledgerService,
            ClosingLockValidationFilter closingLockValidationFilter) {
        this.journalPersistencePort = journalPersistencePort;
        this.journalReversalPersistencePort = journalReversalPersistencePort;
        this.ledgerEntryPersistencePort = ledgerEntryPersistencePort;
        this.ledgerService = ledgerService;
        this.closingLockValidationFilter = closingLockValidationFilter;
    }

    /**
     * 기존 직접 생성 소비자의 source compatibility를 유지합니다.
     * 일반 전표는 기존대로 처리하고, 미연결 REVERSAL은 본문의 fail-closed 검사에서 거부합니다.
     */
    public PostingService(
            JournalPersistencePort journalPersistencePort,
            LedgerEntryPersistencePort ledgerEntryPersistencePort,
            LedgerService ledgerService,
            ClosingLockValidationFilter closingLockValidationFilter) {
        this(journalPersistencePort, UNAVAILABLE_REVERSAL_PERSISTENCE,
                ledgerEntryPersistencePort, ledgerService, closingLockValidationFilter);
    }

    @Transactional
    public void postJournalEntry(Long journalEntryId) {
        postJournalEntry(journalEntryId, "SYSTEM");
    }

    @Transactional
    public void postJournalEntry(Long journalEntryId, String poster) {
        // 같은 전표의 경쟁 요청은 이 트랜잭션 종료까지 대기한 뒤 최신 상태를 검증합니다.
        JournalEntry journalEntry = journalPersistencePort.findByIdWithDetails(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("JournalEntry not found: " + journalEntryId));

        // 역분개 작업도 반드시 전표 행을 잠근 뒤 읽습니다. 취소 경로와 같은 잠금 순서를
        // 사용하므로 둘이 경쟁해도 한쪽의 최신 전표 상태를 확인한 뒤에만 작업을 갱신합니다.
        Optional<JournalReversalOperation> reversalOperation =
                journalReversalPersistencePort.findByReversalJournalEntryId(journalEntryId);
        if (journalEntry.isReversal() && reversalOperation.isEmpty()) {
            // 레거시/직접 저장된 unclaimed REVERSAL을 전기하면 operation의 exactly-once 상태와
            // 원장 효과가 분리됩니다. 일반 전표만 operation 없이 기존 경로를 계속 사용합니다.
            throw new IllegalStateException("역분개 작업이 연결되지 않은 역분개 전표는 전기할 수 없습니다.");
        }

        // 승인된 전표의 값을 먼저 불변 스냅샷으로 고정합니다. 이후 adapter 두 개가 같은
        // Aggregate를 소비하므로 GL과 SL 중 한쪽만 다른 값으로 조립될 여지가 없습니다.
        GeneralLedger generalLedger = GeneralLedger.fromApproved(journalEntry);

        // 작성·승인 이후 기간이 닫힐 수 있으므로 전기 직전에 다시 확인합니다.
        // 조회 실패도 상태·감사 사용자 변경보다 먼저 전파하여 쓰기를 시작하지 않습니다.
        closingLockValidationFilter.validate(journalEntry);
        journalEntry.post(poster);
        journalPersistencePort.save(journalEntry);

        List<JournalDetail> details = journalEntry.getDetails();
        ledgerEntryPersistencePort.save(generalLedger);
        
        ledgerService.updateLedgerBalancesBulk(details);

        // 일반 전표는 작업이 없어 그대로 끝납니다. 역분개는 원장 반영과 같은 트랜잭션에서만
        // POSTED가 되어 journal 상태와 exactly-once 작업 상태가 따로 커밋되지 않습니다.
        reversalOperation.ifPresent(operation -> {
            operation.markPosted(journalEntryId);
            journalReversalPersistencePort.save(operation);
        });
    }
}
