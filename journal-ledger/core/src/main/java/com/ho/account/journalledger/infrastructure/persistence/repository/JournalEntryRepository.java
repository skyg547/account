package com.ho.account.journalledger.infrastructure.persistence.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 전표 저장소 (Journal Entry Repository)
 */
@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /*
     * SimpleJpaRepository의 bulk delete는 엔티티를 로드하지 않아 @PreRemove를 실행하지 않습니다.
     * 상태를 선별하는 것만으로는 같은 우회 API가 다시 사용될 수 있으므로 저장소 경계에서 전부 막습니다.
     */
    @Override
    default void deleteAllInBatch() {
        throw bulkDeleteDisabled();
    }

    @Override
    default void deleteAllInBatch(Iterable<JournalEntry> entities) {
        throw bulkDeleteDisabled();
    }

    @Override
    default void deleteAllByIdInBatch(Iterable<Long> ids) {
        throw bulkDeleteDisabled();
    }

    @Override
    @Deprecated
    default void deleteInBatch(Iterable<JournalEntry> entities) {
        throw bulkDeleteDisabled();
    }

    private static IllegalStateException bulkDeleteDisabled() {
        return new IllegalStateException(
                "POSTED 전표 이력 보호를 우회하는 JPA bulk delete는 사용할 수 없습니다.");
    }
    
    Optional<JournalEntry> findBySlipNo(String slipNo);
    
    List<JournalEntry> findBySlipDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<JournalEntry> findByAccountingDate(LocalDate accountingDate);

    /**
     * 원천 시스템 유형과 ID로 전표 목록을 조회합니다. (역추적용)
     */
    List<JournalEntry> findByLineageSourceTypeAndLineageSourceId(String lineageSourceType, String lineageSourceId);

    /**
     * 회계일자와 원천 ID로 전표 목록을 조회합니다.
     */
    List<JournalEntry> findByAccountingDateAndLineageSourceId(LocalDate accountingDate, String lineageSourceId);

    // PostgreSQL은 LEFT JOIN의 nullable 쪽에 FOR UPDATE를 적용할 수 없으므로 헤더만 잠급니다.
    @Query(value = "SELECT id FROM journal_entries WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockByIdForPosting(@Param("id") Long id);

    @Query("SELECT je FROM JournalEntry je LEFT JOIN FETCH je.details WHERE je.id = :id")
    Optional<JournalEntry> findByIdWithDetails(@Param("id") Long id);

    /**
     * 배타 잠금 없이 전표와 상세를 한 번에 조회합니다.
     *
     * <p>역분개 재시도에서 이미 연결된 역분개를 반환하는 읽기 경로용입니다. 상세 컬렉션을
     * 같은 쿼리로 가져와 트랜잭션 밖 lazy loading과 라인별 추가 조회를 피합니다.</p>
     */
    @Query("SELECT je FROM JournalEntry je LEFT JOIN FETCH je.details WHERE je.id = :id")
    Optional<JournalEntry> findByIdWithDetailsWithoutLock(@Param("id") Long id);
}
