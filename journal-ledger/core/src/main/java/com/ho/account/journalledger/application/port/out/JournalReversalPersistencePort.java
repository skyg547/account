package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import java.util.Optional;

/**
 * 원본별 단일 역분개 작업을 저장하고 조회하는 출력 포트입니다.
 *
 * <p>원본 전표 ID의 영속 고유성이 최종 동시성 방어선이며, 애플리케이션 서비스는 전표 행을
 * 먼저 잠가 같은 원본의 생성 순서를 직렬화합니다.</p>
 */
public interface JournalReversalPersistencePort {

    JournalReversalOperation save(JournalReversalOperation operation);

    Optional<JournalReversalOperation> findByOriginalJournalEntryId(Long originalJournalEntryId);

    Optional<JournalReversalOperation> findByReversalJournalEntryId(Long reversalJournalEntryId);
}
