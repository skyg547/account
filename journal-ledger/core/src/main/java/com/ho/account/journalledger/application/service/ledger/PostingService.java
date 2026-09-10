package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
@RequiredArgsConstructor
public class PostingService {

    private final JournalPersistencePort journalPersistencePort;
    private final LedgerEntryPersistencePort ledgerEntryPersistencePort;
    private final LedgerService ledgerService;
    private final ClosingLockValidationFilter closingLockValidationFilter;

    @Transactional
    public void postJournalEntry(Long journalEntryId) {
        postJournalEntry(journalEntryId, "SYSTEM");
    }

    @Transactional
    public void postJournalEntry(Long journalEntryId, String poster) {
        JournalEntry journalEntry = journalPersistencePort.findByIdWithDetails(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("JournalEntry not found: " + journalEntryId));

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
    }
}
