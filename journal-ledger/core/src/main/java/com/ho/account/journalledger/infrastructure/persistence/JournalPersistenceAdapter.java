package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 전표 영속성 어댑터 (Journal Persistence Adapter) — Outbound Adapter (아웃바운드 어댑터).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전표(JournalEntry)를 데이터베이스에 저장하고 조회하는 역할을 합니다.
 * 애플리케이션 서비스(JournalEntryService)가 요청한 "저장", "조회" 등의 작업을
 * 실제 JPA 기술로 수행합니다.
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * 헥사고날 아키텍처에서 Outbound Adapter(아웃바운드 어댑터)의 역할을 합니다.
 *
 * 위치: infrastructure/persistence/ (기술 구현 계층)
 *
 * 역할:
 *   - JournalPersistencePort(Outbound Port) 인터페이스의 구현체
 *   - JournalEntryRepository(JPA)를 감싸 포트 계약을 이행합니다.
 *   - 애플리케이션 서비스는 이 어댑터의 존재를 모릅니다.
 *     오직 JournalPersistencePort 인터페이스만 알고 있습니다.
 *
 * 의존 방향:
 *   JournalEntryService → JournalPersistencePort (인터페이스)
 *                                ↑ 구현
 *   JournalPersistenceAdapter → JournalEntryRepository (JPA)
 *
 * 왜 이 계층을 나누는가?
 *   - JPA를 쓰다가 다른 기술(MyBatis, MongoDB 등)로 교체하더라도
 *     서비스 코드를 건드리지 않고 이 어댑터만 교체하면 됩니다.
 *   - 단위 테스트 시 이 어댑터를 Mock으로 대체하면 DB 없이 서비스 로직만 테스트 가능합니다.
 *
 * 단순 위임(Delegation) 패턴:
 *   일반 조회/저장은 Repository에 위임합니다. 전기 조회는 같은 트랜잭션에서 헤더를
 *   잠그고 최신 상태를 다시 읽어 중복 금융 효과를 방지합니다.
 * ─────────────────────────────────────────────────
 */
@Component
@RequiredArgsConstructor
public class JournalPersistenceAdapter implements JournalPersistencePort {

    /**
     * 전표 JPA 저장소.
     * Spring Data JPA 인터페이스로, journal_entries 테이블에 접근합니다.
     * JournalPersistenceAdapter만 이 Repository를 직접 사용합니다.
     * (서비스는 Repository를 알면 안 됩니다 — 포트로만 접근)
     */
    private final JournalEntryRepository journalEntryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 전표를 저장하거나 수정합니다.
     *
     * [개발 설명]
     * JPA의 save():
     *   - id가 null이면 INSERT (신규 생성)
     *   - id가 있으면 UPDATE (기존 수정)
     * JournalDetail은 cascade = ALL로 설정되어 있어 함께 저장됩니다.
     *
     * @param journalEntry 저장할 전표 도메인 객체
     * @return 저장된 전표 (id, slipNo 등 DB 생성 필드 포함)
     */
    @Override
    public JournalEntry save(JournalEntry journalEntry) {
        return journalEntryRepository.save(journalEntry);
    }

    /**
     * 내부 PK로 전표 헤더를 조회합니다.
     *
     * [개발 설명]
     * JournalDetail은 LAZY 로딩이므로 이 조회 결과에서 details에 접근하면
     * LazyInitializationException이 발생합니다.
     * 상세 라인이 필요하면 findByIdWithDetails()를 사용하세요.
     *
     * @param id 전표 내부 PK
     * @return 전표 헤더 Optional
     */
    @Override
    public Optional<JournalEntry> findById(Long id) {
        return journalEntryRepository.findById(id);
    }

    /**
     * 쓰기 트랜잭션에서는 전기 소유권을 확보하고 최신 헤더/상세를 반환합니다.
     * 일반 상세 조회도 자체 트랜잭션 동안 짧게 잠그며, 읽기 전용 트랜잭션은 잠금 없이 조회합니다.
     */
    @Override
    @Transactional
    public Optional<JournalEntry> findByIdWithDetails(Long id) {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return journalEntryRepository.findByIdWithDetailsWithoutLock(id);
        }
        // 자동 전표는 생성/승인/전기를 한 트랜잭션에서 수행합니다. refresh가 그 변경을
        // 버리지 않도록 먼저 반영하며, 잠금은 이후 GL/SL·잔액 저장이 끝날 때까지 유지합니다.
        entityManager.flush();
        if (journalEntryRepository.lockByIdForPosting(id).isEmpty()) {
            return Optional.empty();
        }
        JournalEntry entry = journalEntryRepository.findById(id).orElseThrow();
        // 잠금만으로는 이미 관리 중인 APPROVED 객체가 갱신되지 않습니다. 경쟁 요청이
        // 커밋한 POSTED 상태를 반드시 다시 읽어야 중복 요청을 도메인 충돌로 거부합니다.
        entityManager.refresh(entry);
        entry.getDetails().size();
        return Optional.of(entry);
    }

    /**
     * 현재 트랜잭션의 read-only 여부와 관계없이 행 잠금 없이 상세까지 조회합니다.
     * 이미 영속 관계로 확정된 역분개를 중복 요청에 반환하는 경로에서 사용합니다.
     */
    @Override
    public Optional<JournalEntry> findByIdWithDetailsWithoutLock(Long id) {
        return journalEntryRepository.findByIdWithDetailsWithoutLock(id);
    }

    /**
     * 전표번호(slipNo)로 전표를 조회합니다.
     *
     * [개발 설명]
     * slipNo는 UNIQUE 제약이 있어 항상 0개 또는 1개가 반환됩니다.
     *
     * @param slipNo 전표번호 (예: "JE-20260115-001")
     * @return 해당 전표 Optional (없으면 empty)
     */
    @Override
    public Optional<JournalEntry> findBySlipNo(String slipNo) {
        return journalEntryRepository.findBySlipNo(slipNo);
    }

    /**
     * 회계 반영일 기준으로 기간 내 전표 목록을 조회합니다.
     *
     * [개발 설명]
     * BETWEEN 조건 (startDate <= accountingDate <= endDate)으로 조회합니다.
     * 양 끝 날짜를 포함합니다.
     *
     * @param startDate 조회 시작일 (포함)
     * @param endDate   조회 종료일 (포함)
     * @return 해당 기간 전표 목록 (없으면 빈 리스트)
     */
    @Override
    public List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate) {
        return journalEntryRepository.findByAccountingDateBetween(startDate, endDate);
    }

    @Override
    public List<JournalEntry> findBySource(String sourceType, String sourceId) {
        return journalEntryRepository.findByLineageSourceTypeAndLineageSourceId(sourceType, sourceId);
    }
}
