package com.ho.account.journalledger.infrastructure.persistence.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 원본 전표와 역분개 전표 사이의 영속 관계를 저장하는 JPA 저장소입니다.
 *
 * <p>원본 전표 ID가 기본 키이므로 원본 하나에 활성 역분개 operation은 최대 한 건이며,
 * 역분개 전표 ID의 UNIQUE 제약으로 한 역분개가 여러 원본에 연결되는 것도 차단됩니다.</p>
 */
@Repository
public interface JournalReversalOperationRepository
        extends JpaRepository<JournalReversalOperation, Long> {

    Optional<JournalReversalOperation> findByReversalJournalEntryId(Long reversalJournalEntryId);
}
