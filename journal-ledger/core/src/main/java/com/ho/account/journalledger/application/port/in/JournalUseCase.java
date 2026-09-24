package com.ho.account.journalledger.application.port.in;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 전표 유스케이스 (Journal Entry Use Case) — Inbound Port (인바운드 포트).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전표(Journal Entry)는 회계의 모든 거래를 기록하는 기본 단위입니다.
 * 예를 들어 "거래처에 상품을 판매하고 외상 매출채권이 발생한 경우"를 전표로 기록합니다:
 *   차변: 매출채권(11000)  100,000원
 *   대변: 매출(41000)     100,000원
 *
 * 전표의 처리 흐름:
 *   [작성] → createJournalEntry()
 *   [승인] → approveJournalEntry()
 *   [전기] → postJournalEntry()  ← 이 시점에 총계정원장(GL)/보조원장(SL)에 잔액 반영
 *
 * 이벤트 기반 자동 전표:
 *   외부 이벤트(매입, 지출결의, 리스 납부 등)가 발생하면
 *   createJournalEntryFromEvent()를 통해 자동으로 전표를 생성합니다.
 *   JournalRuleEngine이 이벤트 데이터를 분석해 적절한 계정과목과 금액을 자동으로 채웁니다.
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * 헥사고날 아키텍처에서 Inbound Port(인바운드 포트)의 역할을 합니다.
 *
 * - 이 인터페이스는 애플리케이션이 외부에 제공하는 기능 목록(API 계약)입니다.
 * - 구현체: JournalEntryService (application/service/journal/)
 * - 호출자: JournalController (adapter/in/web/), 다른 서비스의 Contract Adapter
 * - 의존 방향: Controller → JournalUseCase ← JournalEntryService
 *   (Controller는 인터페이스에만 의존 → 구현체 교체/테스트 용이)
 *
 * 핵심 원칙: 이 인터페이스만 보면 "이 모듈이 무엇을 할 수 있는가"를 알 수 있어야 합니다.
 * ─────────────────────────────────────────────────
 */
public interface JournalUseCase {

    /**
     * 전표를 수동으로 생성하고 저장합니다.
     *
     * [업무 설명]
     * 회계 담당자가 직접 전표를 작성할 때 사용합니다.
     * 예: 수기 입력 전표, 정기 감가상각 전표 등.
     * 저장 전 차변합계 = 대변합계 검증(validateBalance)이 자동 수행됩니다.
     *
     * [개발 설명]
     * JournalEntry 도메인 객체를 그대로 받아 저장합니다.
     * Controller에서 DTO → 도메인 변환 후 이 메서드를 호출하는 흐름입니다.
     *
     * @param journalEntry 저장할 전표 도메인 객체 (details 포함)
     * @return 저장된 전표 (id, slipNo 등 DB에서 생성된 필드 포함)
     * @throws IllegalStateException 차변합계 ≠ 대변합계일 경우
     */
    JournalEntry createJournalEntry(JournalEntry journalEntry);

    /**
     * 이벤트 데이터로부터 자동 전표를 생성합니다.
     *
     * [업무 설명]
     * 매입, 지출결의, 리스 납부 등 외부 비즈니스 이벤트가 발생하면
     * 해당 이벤트 정보를 Map 형태로 전달받아 전표를 자동 생성합니다.
     * JournalRuleEngine이 등록된 자동분개 규칙을 조회하여 적절한 계정과목을 결정합니다.
     *
     * [개발 설명]
     * eventData는 이벤트 유형에 따라 다른 키를 가집니다.
     * 예: { "eventType": "PURCHASE", "amount": 100000, "vendorId": "BP001" }
     * 규칙이 매칭되지 않으면 Optional.empty()를 반환합니다.
     *
     * @param eventData      이벤트 데이터 (키-값 쌍)
     * @param accountingDate 회계 반영일 (전표의 accountingDate)
     * @return 생성된 전표 (Optional) — 규칙 미매칭 시 empty
     */
    Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> eventData, LocalDate accountingDate);

    /**
     * 특정 기간의 전표 목록을 조회합니다.
     *
     * [업무 설명]
     * 회계 기간(예: 1월 1일 ~ 1월 31일) 내의 모든 전표를 조회합니다.
     * 월말 마감 작업, 원장 검토, 감사 대응 시 활용됩니다.
     * 조회 기준: JournalEntry.accountingDate (전기일)
     *
     * @param startDate 조회 시작일 (포함)
     * @param endDate   조회 종료일 (포함)
     * @return 해당 기간의 전표 목록 (없으면 빈 리스트)
     */
    List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate);

    /**
     * 원천 문서 역추적(Drill-down)을 위한 전표 목록을 조회합니다.
     */
    List<JournalEntry> getJournalEntriesBySource(String sourceType, String sourceId);

    /**
     * 전표번호(slipNo)로 전표를 조회합니다.
     *
     * [업무 설명]
     * 전표번호는 "JE-20260101-001" 형태의 고유 식별자입니다.
     * 감사 추적, 고객 문의 대응, 특정 전표 재확인 시 사용합니다.
     *
     * @param slipNo 전표번호 (예: "JE-20260101-001")
     * @return 해당 전표 (없으면 Optional.empty())
     */
    Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo);

    /**
     * 전표를 상세 라인(JournalDetail)까지 함께 조회합니다.
     *
     * [업무 설명]
     * 전표 상세 화면에서 차변/대변 각 라인을 보여줄 때 사용합니다.
     * 예: 전표 ID 123 → 헤더(날짜, 적요) + 상세 라인 목록(계정, 금액)
     *
     * [개발 설명]
     * JournalDetail이 LAZY 로딩이므로, 상세 라인까지 필요한 경우
     * 이 메서드(JOIN FETCH 포함)를 사용해야 합니다.
     * findById()는 헤더만 조회하며 details 접근 시 LazyInitializationException 위험이 있습니다.
     *
     * @param id 전표 내부 PK
     * @return 상세 라인 포함 전표 (없으면 Optional.empty())
     */
    Optional<JournalEntry> getJournalEntryWithDetails(Long id);

    /**
     * 전표를 조회합니다. (헤더 정보만)
     */
    Optional<JournalEntry> getJournalEntry(Long id);

    /**
     * 작성자가 DRAFT 전표를 승인 요청 상태로 전환합니다.
     *
     * @param id 승인 요청할 전표의 내부 PK
     * @param requester trusted/canonical 작성자 식별자
     */
    void requestJournalEntryApproval(Long id, String requester);

    /**
     * 별도 결재자가 REQUESTED 전표를 검토하고 승인합니다.
     *
     * @param id       승인할 전표의 내부 PK
     * @param approver 승인자 식별자 (사용자 ID 또는 이름)
     * @throws IllegalArgumentException 존재하지 않는 전표 ID
     * @throws IllegalStateException    REQUESTED 상태가 아니거나 maker와 approver가 같은 경우
     */
    void approveJournalEntry(Long id, String approver);

    /**
     * 전표를 전기(Post)합니다.
     *
     * [업무 설명]
     * 전기(Posting)는 회계에서 가장 중요한 단계입니다.
     * 전기가 완료되어야 총계정원장(GL)과 보조원장(SL)에 잔액이 반영됩니다.
     * 즉, 전기 전까지는 아무리 전표를 작성해도 재무제표에 영향이 없습니다.
     *
     * 전기 후 발생하는 일:
     *   1. JournalEntry.status → POSTED
     *   2. PostingService가 GL Entry / SL Entry 생성
     *   3. LedgerService가 GL Balance / SL Balance 잔액 업데이트
     *   4. UnsettledService가 미결 항목(채권·채무) 생성
     *
     * [개발 설명]
     * 도메인 엔티티의 post(poster) 메서드를 호출합니다.
     * APPROVED 상태여야만 전기 가능합니다.
     *
     * @param id     전기할 전표의 내부 PK
     * @param poster 전기 처리자 식별자
      * @throws IllegalArgumentException 존재하지 않는 전표 ID
      * @throws IllegalStateException    APPROVED 상태가 아닌 경우
      */
     void postJournalEntry(Long id, String poster);

     /**
      * 특정 전표를 취소하는 역분개(Reversal) 전표를 생성합니다.
      *
      * [업무 설명]
      * 이미 전기(POSTED)된 전표에 오류가 발견된 경우, 해당 전표를 직접 수정하는 대신
      * 차대변 방향을 반전시킨 새로운 전표를 생성하여 기존 회계 처리를 무효화합니다 (취소 분개).
      * 예: "차: 현금 100 / 대: 매출 100" 실수 → "차: 매출 100 / 대: 현금 100" 역분개 생성
      *
      * [개발 설명]
      * 원본 전표의 내용을 복사하여 차대변을 반전시킨 새 JournalEntry 객체를 생성하고 저장합니다.
      * 도메인 엔티티의 createReversal() 메서드를 활용합니다.
      *
      * @param id             원본 전표 ID (POSTED 상태여야 함)
      * @param accountingDate 역분개 회계 반영일
      * @param creator        역분개 작성자
      * @param reason         취소 사유
      * @return 생성된 역분개 전표
      * @throws IllegalStateException 원본 전표가 POSTED 상태가 아닌 경우
      */
     JournalEntry reverseJournalEntry(Long id, LocalDate accountingDate, String creator, String reason);
     }
