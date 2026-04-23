package com.ho.account.journalledger.core.domain.ledger.service;

import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.core.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.core.domain.ledger.repository.SlBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장부 기장 서비스 (Posting Service)
 */
@Service
@RequiredArgsConstructor
public class PostingService {

    private final JournalEntryRepository journalEntryRepository;
    private final GlBalanceRepository glBalanceRepository;
    private final SlBalanceRepository slBalanceRepository;

    @Transactional
    public void postToLedger(Long journalEntryId) {
        // 전표 데이터를 기반으로 총계정원장(G/L) 및 보조부원장(S/L)에 반영하는 로직
    }
}
