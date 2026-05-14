package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 전표 애플리케이션 서비스 (Journal Entry Service).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전표(Journal Entry)의 생성, 조회, 승인, 전기를 담당하는 핵심 서비스입니다.
 *
 * 전표 처리 흐름:
 *   1. createJournalEntry()       → 전표 작성 (DRAFT 상태)
 *   2. approveJournalEntry()      → 전표 승인 (DRAFT → APPROVED)
 *   3. postJournalEntry()         → 전기 처리 (APPROVED → POSTED)
 *                                    → GL/SL 원장에 잔액 반영
 *
 * 이벤트 기반 자동 전표:
 *   createJournalEntryFromEvent() → JournalRuleEngine이 이벤트 분석 후 자동 전표 생성
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * 헥사고날 아키텍처에서 Application Service 계층에 위치합니다.
 *
 * 역할:
 *   - JournalUseCase (Inbound Port) 구현체
 *   - 비즈니스 흐름 조정 (Orchestration): 도메인 객체 호출 순서 결정
 *   - 도메인 로직은 JournalEntry 도메인 엔티티에 위임 (Rich Domain Model)
 *   - 영속성 기술과의 접점은 JournalPersistencePort (Outbound Port)를 통해서만 접근
 *
 * 의존성 (모두 인터페이스, 구체 구현체에 의존하지 않음):
 *   - JournalPersistencePort → JournalPersistenceAdapter (JPA) 구현체가 주입됨
 *   - JournalRuleEngine → 자동분개 규칙 엔진
 *
 * 트랜잭션 전략:
 *   - 쓰기 메서드(@Transactional): createJournalEntry, approveJournalEntry, postJournalEntry
 *   - 읽기 메서드: 별도 @Transactional 없음 (Repository 레벨에서 처리)
 * ─────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class JournalEntryService implements JournalUseCase {

    /**
     * 전표 영속성 포트 (Outbound Port).
     * 실제 DB 접근은 이 포트의 구현체(JournalPersistenceAdapter)가 담당합니다.
     * 이 서비스는 "저장해줘", "찾아줘"라는 의도만 표현하고, 기술 세부사항은 모릅니다.
     */
    private final JournalPersistencePort journalPersistencePort;

    /**
     * 자동분개 규칙 엔진.
     * 이벤트 데이터를 분석해 자동으로 전표를 생성하는 로직을 담당합니다.
     * 예: "매입 이벤트" → 매입채무 차변/대변 자동 분개
     */
    private final JournalRuleEngine journalRuleEngine;

    /**
     * 전기 서비스 (Posting Service).
     * 전표 승인 후 실제 원장(GL/SL)에 반영하는 역할을 담당합니다.
     */
    private final PostingService postingService;

    /**
     * 전표를 수동으로 생성합니다.
     *
     * [업무 설명]
     * 회계 담당자가 직접 작성한 전표를 저장합니다.
     * 저장 전 반드시 차변합계 = 대변합계 검증을 수행합니다.
     * (복식부기 원칙: 차변과 대변은 항상 같아야 합니다)
     *
     * [개발 설명]
     * validateBalance()는 JournalEntry 도메인 엔티티의 메서드입니다.
     * 도메인 로직(검증)을 서비스가 아닌 도메인 객체 내부에 캡슐화한 패턴입니다.
     *
     * @param journalEntry 저장할 전표 (Controller에서 DTO → 도메인 변환 후 전달)
     * @return 저장된 전표 (DB에서 생성된 id, slipNo 포함)
     */
    @Override
    @Transactional
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        // 전표 번호 채번 (Simple Implementation)
        if (journalEntry.getSlipNo() == null) {
            String datePart = journalEntry.getSlipDate().toString().replace("-", "");
            String uniquePart = java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            journalEntry.setSlipNo("JE-" + datePart + "-" + uniquePart);
        }

        // 복식부기 검증: 차변합계 ≠ 대변합계이면 IllegalStateException 발생
        // 이 검증은 도메인 엔티티(JournalEntry) 내부에 정의되어 있습니다.
        journalEntry.validateBalance();
        return journalPersistencePort.save(journalEntry);
    }

    /**
     * 이벤트 데이터로부터 자동 전표를 생성합니다.
     *
     * [업무 설명]
     * 매입, 지출결의, 리스 납부 등 외부 이벤트가 발생하면 자동으로 전표를 생성합니다.
     * 담당자가 직접 전표를 입력하지 않아도 되므로, 수작업 오류를 줄이고 자동화가 가능합니다.
     *
     * [개발 설명]
     * JournalRuleEngine에 이벤트 데이터를 넘기면, 등록된 규칙(JournalRule)을 조회해
     * 적합한 전표를 자동 생성합니다.
     * 규칙이 없거나 매칭되지 않으면 Optional.empty()를 반환합니다.
     * 이 경우 상위 호출자(Contract Adapter 등)가 예외 처리 또는 수동 처리를 결정합니다.
     *
     * @param eventData      이벤트 데이터 (예: { "eventType": "PURCHASE", "amount": 100000 })
     * @param accountingDate 회계 반영일
     * @return 생성된 전표 Optional (규칙 미매칭 시 empty)
     */
    @Override
    @Transactional
    public Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> eventData, LocalDate accountingDate) {
        return journalRuleEngine.generateJournalEntry(eventData, accountingDate)
                .map(this::createJournalEntry);
    }

    /**
     * 특정 기간의 전표 목록을 조회합니다.
     *
     * [업무 설명]
     * 월별 마감, 원장 검토, 감사 대응 시 특정 기간의 전표 전체를 조회합니다.
     *
     * @param startDate 조회 시작일 (포함)
     * @param endDate   조회 종료일 (포함)
     * @return 해당 기간 전표 목록
     */
    @Override
    public List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate) {
        return journalPersistencePort.findByAccountingDateBetween(startDate, endDate);
    }

    @Override
    public List<JournalEntry> getJournalEntriesBySource(String sourceType, String sourceId) {
        return journalPersistencePort.findBySource(sourceType, sourceId);
    }

    /**
     * 전표번호로 전표를 조회합니다.
     *
     * [업무 설명]
     * "JE-20260101-001" 같은 전표번호로 특정 전표를 조회합니다.
     * 거래 명세서나 감사 요청 시 특정 전표를 검색할 때 사용합니다.
     *
     * @param slipNo 전표번호
     * @return 해당 전표 (없으면 Optional.empty())
     */
    @Override
    public Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo) {
        return journalPersistencePort.findBySlipNo(slipNo);
    }

    /**
     * 전표를 상세 라인까지 함께 조회합니다.
     *
     * [업무 설명]
     * 전표 상세 화면에서 차변/대변 각 라인(계정과목, 금액, 적요)을 보여줄 때 사용합니다.
     *
     * [개발 설명]
     * findByIdWithDetails()는 JOIN FETCH로 JournalDetail까지 한 번에 로딩합니다.
     * 일반 findById()는 헤더만 가져오므로 상세 라인 접근 시 LazyInitializationException이 발생합니다.
     *
     * @param id 전표 내부 PK
     * @return 상세 라인 포함 전표 (없으면 Optional.empty())
     */
    @Override
    public Optional<JournalEntry> getJournalEntryWithDetails(Long id) {
        return journalPersistencePort.findByIdWithDetails(id);
    }

    @Override
    public Optional<JournalEntry> getJournalEntry(Long id) {
        return journalPersistencePort.findById(id);
    }

    /**
     * 전표를 승인합니다.
     *
     * [업무 설명]
     * 결재권자(승인자)가 전표를 검토하고 승인합니다.
     * 승인된 전표만 이후 전기(Posting) 처리가 가능합니다.
     * 상태 전환: DRAFT → APPROVED (또는 REQUESTED → APPROVED)
     *
     * [개발 설명]
     * 상태 전환 로직은 서비스가 아닌 도메인 엔티티(JournalEntry.approve())에 구현됩니다.
     * 이것이 Rich Domain Model 패턴입니다.
     * 서비스는 "1. 조회 → 2. 도메인 메서드 호출 → 3. 저장"의 흐름만 조정합니다.
     *
     * @param id       승인할 전표의 내부 PK
     * @param approver 승인자 식별자
     * @throws IllegalArgumentException 존재하지 않는 전표 ID
     */
    @Override
    @Transactional
    public void approveJournalEntry(Long id, String approver) {
        // 1. 전표 조회 (없으면 즉시 예외)
        JournalEntry entry = journalPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전표입니다: " + id));

        // 2. 도메인 엔티티에 상태 전환 위임 (Rich Domain Model)
        //    approve() 내부에서 현재 상태가 DRAFT/REQUESTED인지 검증합니다.
        entry.approve(approver);

        // 3. 변경된 상태를 저장
        journalPersistencePort.save(entry);
    }

    /**
     * 전표를 전기(Post)합니다.
     *
     * [업무 설명]
     * 전기는 회계상 가장 중요한 단계입니다.
     * 전기가 완료되어야 총계정원장(GL)과 보조원장(SL)에 잔액이 반영되고,
     * 재무제표(재무상태표, 손익계산서)에 해당 거래가 포함됩니다.
     *
     * 전기 후 다음 처리가 연이어 발생합니다 (PostingService가 담당):
     *   1. GL Entry 생성 → GL Balance 잔액 업데이트
     *   2. SL Entry 생성 → SL Balance 잔액 업데이트
     *   3. 미결 항목(채권·채무) 생성 (UnsettledItem)
     *
     * [개발 설명]
     * 도메인 엔티티의 post(poster) 메서드 호출로 상태를 POSTED로 변경합니다.
     * APPROVED 상태인 전표만 전기 가능합니다 (도메인 엔티티 내부 검증).
     * 전기 후 원장 갱신은 PostingService가 별도로 처리합니다.
     *
     * @param id     전기할 전표의 내부 PK
     * @param poster 전기 처리자 식별자
     * @throws IllegalArgumentException 존재하지 않는 전표 ID
     */
    @Override
    @Transactional
    public void postJournalEntry(Long id, String poster) {
        // 상태 전이와 GL/SL 원장 반영은 PostingService에서 하나의 트랜잭션으로 처리합니다.
        postingService.postJournalEntry(id, poster);
    }

    @Override
    @Transactional
    public JournalEntry reverseJournalEntry(Long id, LocalDate accountingDate, String creator, String reason) {
        // 1. 원본 전표 조회 (상세 내역 포함)
        JournalEntry original = journalPersistencePort.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전표입니다: " + id));

        // 2. 도메인 엔티티에 역분개 생성 위임 (Rich Domain Model)
        JournalEntry reversal = original.createReversal(creator, accountingDate, reason);

        // 3. 역분개 전표 저장 및 반환
        return createJournalEntry(reversal);
    }
}
