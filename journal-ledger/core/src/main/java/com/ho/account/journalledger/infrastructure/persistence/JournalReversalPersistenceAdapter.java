package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalReversalOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 원본별 역분개 operation을 영속화하는 출력 어댑터입니다.
 *
 * <p>동일 원본에 대한 최종 exactly-once 경계는
 * {@code journal_reversal_operations.original_journal_entry_id} 기본 키가 담당합니다.
 * 호출 서비스의 트랜잭션 안에서 전표 생성과 operation 저장이 함께 commit/rollback됩니다.</p>
 */
@Component
@RequiredArgsConstructor
public class JournalReversalPersistenceAdapter implements JournalReversalPersistencePort {

    private final JournalReversalOperationRepository repository;

    @Override
    public JournalReversalOperation save(JournalReversalOperation operation) {
        return repository.save(operation);
    }

    @Override
    public Optional<JournalReversalOperation> findByOriginalJournalEntryId(Long originalJournalEntryId) {
        return repository.findById(originalJournalEntryId);
    }

    @Override
    public Optional<JournalReversalOperation> findByReversalJournalEntryId(Long reversalJournalEntryId) {
        return repository.findByReversalJournalEntryId(reversalJournalEntryId);
    }
}
