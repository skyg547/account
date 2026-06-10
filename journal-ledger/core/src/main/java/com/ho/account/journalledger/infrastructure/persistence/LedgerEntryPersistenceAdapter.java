package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.domain.ledger.repository.GlEntryRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 전기 결과 GL/SL 엔트리를 Spring Data JPA로 저장하는 출력 어댑터입니다.
 */
@Component
@ConditionalOnProperty(name = "journal-ledger.ledger.persistence-mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class LedgerEntryPersistenceAdapter implements LedgerEntryPersistencePort {

    private final GlEntryRepository glEntryRepository;
    private final SlEntryRepository slEntryRepository;

    @Override
    public void saveGlEntries(List<GlEntry> entries) {
        glEntryRepository.saveAll(entries);
    }

    @Override
    public void saveSlEntries(List<SlEntry> entries) {
        slEntryRepository.saveAll(entries);
    }
}
