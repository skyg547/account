package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 전표 영속성 포트 (Journal Persistence Port) — Outbound Port (아웃바운드 포트).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전표(JournalEntry)를 저장소(데이터베이스)에 저장하고 조회하는 기능을 정의합니다.
 * 이 포트를 통해 서비스(애플리케이션 계층)는 데이터베이스 기술에 의존하지 않고
 * "전표를 저장해줘", "전표번호로 찾아줘" 같은 의도만 표현합니다.
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * 헥사고날 아키텍처에서 Outbound Port(아웃바운드 포트)의 역할을 합니다.
 *
 * 의존 방향:
 *   JournalEntryService → JournalPersistencePort ← JournalPersistenceAdapter(JPA 구현체)
 *
 * - 이 인터페이스는 "기술 독립적" 영속성 계약입니다.
 *   JPA를 쓰든, MongoDB를 쓰든, 이 포트만 구현하면 서비스 코드 변경 없이 교체 가능합니다.
 * - 구현체: JournalPersistenceAdapter (adapter/out/persistence/)
 *   → 내부적으로 JournalRepository(JPA)를 감쌉니다.
 * - 서비스 테스트 시 이 인터페이스를 Mock으로 대체하면 DB 없이 단위 테스트 가능합니다.
 *
 * [조회 메서드 설계 원칙]
 * - findById: 헤더만 로딩 (성능 우선, 상세 라인 불필요한 경우)
 * - findByIdWithDetails: 쓰기 트랜잭션에서는 헤더 잠금 후 최신 상세 조회, 읽기 전용이면 JOIN FETCH
 * - findBySlipNo: 전표번호 기반 조회 (외부 참조, 감사 추적용)
 * - findByAccountingDateBetween: 기간별 목록 조회 (월말 마감, 원장 집계용)
 * ─────────────────────────────────────────────────
 */
public interface JournalPersistencePort {

    /**
     * Reserves one database-backed number for an automatically generated slip.
     * Sequence gaps after rollback are acceptable; reusing a number is not.
     */
    default long nextSlipNumber() {
        throw new SlipNumberAllocationException("전표번호 채번 저장소가 구성되지 않았습니다.");
    }

    /**
     * 전표를 저장하거나 수정합니다 (Upsert 방식).
     *
     * [업무 설명]
     * 신규 전표 생성 및 기존 전표 상태 변경(승인, 전기 등) 시 모두 사용됩니다.
     *
     * [개발 설명]
     * JPA의 save()와 동일한 의미입니다.
     *   - id == null → INSERT (신규 저장)
     *   - id != null → UPDATE (기존 수정)
     * JournalDetail은 cascade = ALL 설정으로 함께 저장됩니다.
     *
     * @param journalEntry 저장할 전표 도메인 객체
     * @return 저장된 전표 (id, slipNo 등 DB 생성 필드 포함)
     */
    JournalEntry save(JournalEntry journalEntry);

    /**
     * 내부 PK로 전표 헤더를 조회합니다.
     *
     * [업무 설명]
     * 전표 승인, 전기 처리 시 전표를 불러올 때 사용합니다.
     * 상세 라인(JournalDetail)이 필요하지 않은 경우 이 메서드를 사용합니다.
     *
     * [개발 설명]
     * JournalDetail은 LAZY 로딩이므로, 이 메서드로 조회 후 details에 접근하면
     * LazyInitializationException이 발생합니다. 상세 라인이 필요하면 findByIdWithDetails()를 사용하세요.
     *
     * @param id 전표 내부 PK
     * @return 전표 헤더 (없으면 Optional.empty())
     */
    Optional<JournalEntry> findById(Long id);

    /**
     * 전표를 상세 라인(JournalDetail)까지 함께 조회합니다.
     *
     * [업무 설명]
     * 전표 상세 화면이나 전기(Posting) 처리 시처럼 차변/대변 라인이
     * 모두 필요한 경우에 사용합니다.
     *
     * [개발 설명]
     * 읽기 전용 조회는 JOIN FETCH를 사용합니다. 쓰기 조회는 헤더 잠금 후
     * 기존 관리 객체까지 새로 읽으므로 잠금/새로고침/상세 로딩에 추가 쿼리가 발생합니다.
     * 쓰기 트랜잭션에서는 종료까지 전표의 배타적 소유권을 확보하고 최신 상태를 반환합니다.
     * 전기 시 상태 검증, GL/SL 저장, 잔액 갱신을 같은 쓰기 트랜잭션에서 수행해야 합니다.
     * 읽기 전용 트랜잭션에서는 잠금 없이 상세를 조회합니다.
     *
     * @param id 전표 내부 PK
     * @return 상세 라인 포함 전표 (없으면 Optional.empty())
     */
    Optional<JournalEntry> findByIdWithDetails(Long id);

    /**
     * 전표 상세를 현재 쓰기 트랜잭션에서도 행 잠금 없이 조회합니다.
     *
     * <p>동일 원본 역분개 재요청은 원본 행 잠금으로 이미 직렬화되어 있습니다. 기존 역분개
     * 응답을 반환하면서 그 역분개 행까지 잠그면 전기/취소 경로와 잠금 순서가 뒤집힐 수 있으므로
     * 이 조회는 명시적으로 비잠금 JOIN FETCH 계약을 사용합니다.</p>
     *
     * @param id 전표 내부 PK
     * @return 상세 라인 포함 전표 (없으면 Optional.empty())
     */
    default Optional<JournalEntry> findByIdWithDetailsWithoutLock(Long id) {
        // 기존 포트 구현의 source compatibility를 유지하는 비잠금 fallback입니다.
        // 상세 초기화가 필요한 영속 어댑터는 JOIN FETCH 구현으로 override해야 합니다.
        return findById(id);
    }

    /**
     * 전표번호(slipNo)로 전표를 조회합니다.
     *
     * [업무 설명]
     * 전표번호는 "JE-20260101-001" 형태의 외부 공개용 식별자입니다.
     * 거래 명세서, 세금계산서와 전표를 연결하거나, 감사 요청 시 특정 전표를 조회할 때 사용합니다.
     *
     * [개발 설명]
     * slipNo에 UNIQUE 제약이 있어 항상 단건(1개 또는 없음)이 반환됩니다.
     * Optional 반환: 존재하지 않는 전표번호 요청에 안전하게 대응합니다.
     *
     * @param slipNo 전표번호 (예: "JE-20260101-001")
     * @return 해당 전표 (없으면 Optional.empty())
     */
    Optional<JournalEntry> findBySlipNo(String slipNo);

    /**
     * 회계 반영일 기준으로 특정 기간의 전표 목록을 조회합니다.
     *
     * [업무 설명]
     * 월말 마감, 기간별 원장 검토, 재무제표 작성 시 특정 기간의 전표 전체를 조회합니다.
     * 조회 기준은 accountingDate(회계 반영일)이며, slipDate(전표 작성일)이 아닙니다.
     * 예: 1월분 마감 → accountingDate between 2026-01-01 and 2026-01-31
     *
     * [개발 설명]
     * BETWEEN 조건 (startDate <= accountingDate <= endDate) 포함 양끝값 조회입니다.
     * 결과 리스트가 클 수 있으므로 페이징이 필요할 경우 별도 메서드 추가를 고려하세요.
     *
     * @param startDate 조회 시작일 (포함)
     * @param endDate   조회 종료일 (포함)
     * @return 해당 기간 전표 목록 (없으면 빈 리스트)
     */
    List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 원천 문서 정보를 기반으로 전표를 조회합니다.
     */
    List<JournalEntry> findBySource(String sourceType, String sourceId);
}
