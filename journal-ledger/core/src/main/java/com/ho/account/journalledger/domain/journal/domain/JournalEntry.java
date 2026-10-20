package com.ho.account.journalledger.domain.journal.domain;

import com.ho.account.journalledger.domain.ledger.domain.AccountingPrecision;
import com.ho.account.journalledger.domain.ledger.domain.Credit;
import com.ho.account.journalledger.domain.ledger.domain.Debit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 회계 전표(Journal Entry) — 분개 정보를 담는 핵심 Aggregate Root.
 *
 * 🐣 [초보자를 위한 설명]
 * 전표(傳票)는 쉽게 말해 '회계용 영수증'입니다. 
 * "언제(날짜), 누구와(거래처), 어떤 목적으로(계정과목), 얼마를(금액)" 주고받았는지 기록하는 모든 장부의 가장 기본이 되는 종이 한 장이라고 생각하면 됩니다.
 * 이 한 장의 종이는 반드시 '왼쪽(차변)'과 '오른쪽(대변)'의 금액이 같아야 한다는 아주 중요한 규칙을 가지고 있습니다.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전표(傳票, Journal Entry)는 회계에서 모든 거래를 기록하는 기본 단위입니다.
 * "언제(날짜), 어떤 계정에, 얼마를, 차변/대변 어느 쪽으로" 기록할지를 정의합니다.
 *
 * 하나의 전표(JournalEntry)는 여러 개의 전표 상세(JournalDetail) 라인으로 구성됩니다.
 * 복식부기 원칙에 따라 차변 합계 = 대변 합계가 반드시 성립해야 합니다.
 *
 * 전표 생명주기:
 *   DRAFT(초안) → REQUESTED(승인요청) → APPROVED(승인) → POSTED(전기/원장반영)
 *                                      ↘ REJECTED(반려)
 *   POSTED → REVERSED(역분개 취소)
 *
 * 핵심 필드:
 *   - slipNo       : 전표 번호 (시스템이 자동 채번, 유니크)
 *   - slipDate     : 전표 작성일 (문서 기준일)
 *   - accountingDate : 회계 반영일 (원장 기표 기준일, 회계 기간 마감에 사용)
 *   - entryType    : NORMAL(일반), REVERSAL(역분개) 등 전표 유형
 *   - lineageSourceType / lineageSourceId : 원천 문서 추적용
 *                    (예: "EXPENDITURE_RESOLUTION" / "REQ-20260101-001")
 *                    drill-down 기능에서 원천 문서로 되돌아갈 때 사용합니다.
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - 이 클래스는 헥사고날 아키텍처의 도메인 레이어에 위치합니다.
 * - 저장 매핑은 infrastructure의 META-INF/journal-ledger-orm.xml에서 정의합니다.
 * - 상태 전이(approve/reject/post)는 서비스가 아닌 도메인 메서드에서 처리합니다 (Rich Domain Model).
 * - details 컬렉션의 저장 cascade와 orphan removal은 infrastructure 매핑에서 정의합니다.
 * - validateBalance()는 차대변 일치 여부를 검증하며, createJournalEntry 시 반드시 호출됩니다.
 * - slipNo 채번은 JournalEntryService에서 담당합니다 (도메인 외부 관심사).
 * - 인덱스: slipNo, accountingDate, status, lineageSourceType+lineageSourceId
 * ─────────────────────────────────────────────────
 */
public class JournalEntry {

    static final String FINAL_HISTORY_MUTATION_MESSAGE = "POSTED 전표 이력은 변경하거나 삭제할 수 없습니다.";
    private static final int MAX_REJECTION_REASON_LENGTH = 500;

    /** 시스템 내부 PK (자동 증가) */
    private Long id;

    /**
     * 전표 번호 (슬립 번호).
     * 시스템이 자동 채번하며, 한 시스템 내에서 유일해야 합니다.
     * 예: "JE-20260101-0001"
     */
    private String slipNo;

    /**
     * 전표 작성일 (문서 기준일).
     * 사용자가 전표를 작성한 날짜입니다.
     * accountingDate와 다를 수 있습니다 (예: 월말 마감 후 소급 처리).
     */
    private LocalDate slipDate;

    /**
     * 회계 반영일 (기표일).
     * 원장(GL/SL)에 금액이 반영되는 기준 날짜입니다.
     * 회계 기간(fiscal period) 계산에 사용됩니다.
     * null이면 slipDate와 동일하게 자동 설정됩니다(최초 저장 시).
     */
    private LocalDate accountingDate;

    /** 전표 적요 — 거래 내용을 한 줄로 요약한 설명 (예: "지출결의: 사무용품 구매") */
    private String description;

    /**
     * 전표 상태.
     * DRAFT → REQUESTED → APPROVED → POSTED 순으로 진행됩니다.
     * 각 상태의 의미는 JournalEntryStatus 열거형을 참고하세요.
     */
    private JournalEntryStatus status;

    /**
     * 전표 유형.
     * - NORMAL   : 일반 분개 전표
     * - REVERSAL : 역분개(취소) 전표
     * null이면 최초 저장 시 "NORMAL"로 자동 설정됩니다.
     */
    private String entryType;

    /**
     * 거래 통화.
     * 외화 거래 시 해당 통화를 설정하고, exchangeRate로 기본 통화 환산 금액을 계산합니다.
     * 국내 원화 거래이면 null 또는 KRW.
     */
    private String currencyCode;

    /**
     * 환율 (거래 통화 → 기본 통화 변환 비율).
     * 예: USD 거래 시 1,350.00 (USD 1 = KRW 1,350)
     * 정밀도 손실 방지를 위해 BigDecimal(19,8) 사용.
     */
    private BigDecimal exchangeRate;

    /** 반려 사유 — 결재자가 전표를 반려할 때 기록하는 사유 텍스트 */
    private String rejectionReason;

    /**
     * 전표 상세 라인 목록.
     * 하나의 전표는 최소 2개(차변 1개 + 대변 1개) 이상의 상세 라인을 가집니다.
     * 저장 시 상세의 cascade와 orphan removal은 infrastructure 매핑에서 정의합니다.
     */
    private List<JournalDetail> details = new ArrayList<>();

    /** 최초 생성 일시 (수정 불가 — @Column(updatable=false)) */
    private LocalDateTime createdAt;

    /** 최종 수정 일시 */
    private LocalDateTime updatedAt;

    /** 전표 작성자 ID 또는 이름 */
    private String createdBy;

    /** 승인자 canonical ID. 전기 후에도 auditUser와 별도로 보존됩니다. */
    private String approvedBy;

    /** 최종 처리자 (승인/반려/전기한 사람). 상태 변경 시 자동 갱신됩니다. */
    private String auditUser;

    /**
     * 원천 문서 유형 (Lineage Source Type).
     * 이 전표를 생성한 원천 업무 문서의 종류를 저장합니다.
     * 예: "EXPENDITURE_RESOLUTION", "PURCHASE_INVOICE", "LEASE_PAYMENT"
     * drill-down 기능에서 원장 항목 → 원천 문서로 역추적할 때 사용합니다.
     */
    private String lineageSourceType;

    /**
     * 원천 문서 식별자 (Lineage Source ID).
     * lineageSourceType과 함께 원천 문서를 특정합니다.
     * 예: "REQ-20260101-001" (지출결의 번호), "INV-2026-0042" (매입 인보이스 번호)
     */
    private String lineageSourceId;

    private JournalEntryStatus persistedStatus;

    // ─── 생명주기 콜백 ────────────────────────────────────

    /**
     * 최초 저장(INSERT) 시 자동 실행되는 초기화 로직.
     * - createdAt, updatedAt 자동 설정
     * - status 기본값: DRAFT
     * - accountingDate 미입력 시 slipDate로 대체
     * - entryType 기본값: NORMAL
     * - auditUser 기본값: createdBy 또는 "SYSTEM"
     */
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            initializeDraft();
        }
        if (this.accountingDate == null) {
            // 회계 반영일 미입력 시 전표 작성일과 동일하게 처리
            this.accountingDate = this.slipDate;
        }
        if (this.entryType == null) {
            this.entryType = "NORMAL";
        }
        if (this.currencyCode == null || this.currencyCode.isBlank()) {
            // 통화가 없는 원장 키는 GL/SL 잔액을 재현할 수 없으므로 국내 기본통화로 명시합니다.
            this.currencyCode = "KRW";
        }
        if (this.auditUser == null) {
            this.auditUser = this.createdBy != null ? this.createdBy : "SYSTEM";
        }
    }

    /**
     * 이미 저장된 최종 상태를 기준으로 dirty checking과 detached merge 우회를 차단합니다.
     * 현재 상태만 보면 정상적인 첫 APPROVED -> POSTED 전환도 최종 상태로 오인하므로,
     * 로드/저장 직후 캡처한 상태와 현재 상태를 함께 판정합니다.
     */
    public void onUpdate() {
        if (isFinalStatus(this.persistedStatus)
                || this.status == JournalEntryStatus.REVERSED
                || (this.status == JournalEntryStatus.POSTED
                        && this.persistedStatus != JournalEntryStatus.APPROVED)) {
            throw finalHistoryMutation();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void onRemove() {
        assertHistoryMutable();
    }

    /** 상태만 캡처하며 details에는 접근하지 않아 @PostLoad에서 lazy-load/N+1을 만들지 않습니다. */

    public void capturePersistedState() {
        this.persistedStatus = this.status;
    }

    // ─── 도메인 비즈니스 메서드 ──────────────────────────────

    /**
     * 전표를 승인 요청합니다 (상태: DRAFT → REQUESTED).
     *
     * <p>작성자 본인만 자신의 초안을 제출할 수 있습니다. 이벤트/기계 전표도 동일하며,
     * createdBy에는 개별 서비스 principal을 사용해야 이후 별도 checker가 승인할 수 있습니다.</p>
     */
    public void requestApproval(String requester) {
        assertHistoryMutable();
        if (this.status != JournalEntryStatus.DRAFT) {
            throw new IllegalStateException("승인 요청 가능한 상태가 아닙니다. 현재 상태: " + this.status);
        }
        String canonicalMaker = JournalActor.canonicalize(this.createdBy);
        String canonicalRequester = JournalActor.canonicalize(requester);
        if (!canonicalMaker.equals(canonicalRequester)) {
            throw new IllegalStateException("전표 작성자만 승인 요청할 수 있습니다.");
        }
        validateInvariants();
        this.createdBy = canonicalMaker;
        this.status = JournalEntryStatus.REQUESTED;
        this.auditUser = canonicalRequester;
    }

    /**
     * 전표를 승인합니다 (상태: REQUESTED → APPROVED).
     *
     * [업무 규칙]
     * - REQUESTED 상태에서만 승인 가능합니다.
     * - canonical identity가 작성자와 같은 actor는 승인할 수 없습니다.
     * - 승인 전 반드시 차대변 합계 일치(validateBalance)를 검증합니다.
     * - 승인된 전표는 PostingService를 통해 GL/SL 원장에 전기됩니다.
     *
     * [개발 참고]
     * - 호출 위치: JournalEntryService.approveJournalEntry()
     * - Rich Domain Model 원칙: 상태 전이 로직을 엔티티 안에 캡슐화합니다.
     *
     * @param approver 승인자 ID 또는 이름
     */
    public void approve(String approver) {
        assertHistoryMutable();
        if (this.status != JournalEntryStatus.REQUESTED) {
            throw new IllegalStateException("승인 가능한 상태가 아닙니다. 현재 상태: " + this.status);
        }
        String canonicalMaker = JournalActor.canonicalize(this.createdBy);
        String canonicalApprover = JournalActor.canonicalize(approver);
        if (canonicalMaker.equals(canonicalApprover)) {
            throw new IllegalStateException("전표 작성자와 승인자는 달라야 합니다.");
        }
        validateInvariants(); // 도메인 불변성(Invariants) 및 차대변 합계 일치 검증
        this.createdBy = canonicalMaker;
        this.approvedBy = canonicalApprover;
        this.status = JournalEntryStatus.APPROVED;
        this.auditUser = canonicalApprover;
    }

    /**
     * 전표를 반려합니다 (상태: → REJECTED).
     *
     * [업무 규칙]
     * - 결재자가 전표 내용에 문제가 있다고 판단할 때 반려합니다.
     * - 반려된 전표는 작성자가 수정 후 다시 승인 요청할 수 있습니다.
     *
     * @param approver 반려자 ID 또는 이름
     * @param reason   반려 사유 (작성자에게 표시됨)
     */
    public void reject(String approver, String reason) {
        assertHistoryMutable();
        if (this.status != JournalEntryStatus.DRAFT && this.status != JournalEntryStatus.REQUESTED) {
            throw new IllegalStateException("반려 가능한 상태가 아닙니다. 현재 상태: " + this.status);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("반려 사유는 필수입니다.");
        }
        this.status = JournalEntryStatus.REJECTED;
        this.rejectionReason = reason.trim();
        this.auditUser = approver;
    }

    /**
     * 전표를 전기합니다 (상태: APPROVED → POSTED).
     *
     * [업무 설명]
     * 전기(Posting)는 승인된 전표를 실제 총계정원장(GL)과 보조원장(SL)에
     * 반영하는 최종 확정 단계입니다. POSTED 상태가 되면 전표는 변경 불가합니다.
     *
     * [개발 참고]
     * - 실제 원장 데이터 생성은 PostingService.postJournalEntry()에서 처리합니다.
     * - 이 메서드는 상태 변경과 감사 기록만 담당합니다.
     *
     * @param poster 전기 처리자 ID 또는 이름
     */
    public void post(String poster) {
        assertHistoryMutable();
        if (this.status != JournalEntryStatus.APPROVED) {
            throw new IllegalStateException("승인된 전표만 전기할 수 있습니다.");
        }
        if (this.approvedBy == null || this.approvedBy.isBlank()) {
            throw new IllegalStateException("승인자 증거가 없는 전표는 전기할 수 없습니다.");
        }
        this.status = JournalEntryStatus.POSTED;
        this.auditUser = JournalActor.canonicalize(poster);
    }

    /**
     * 현재 전표를 취소하는 역분개(Reversal) 전표를 생성합니다.
     *
     * [업무 규칙]
     * - POSTED 상태인 전표만 역분개할 수 있습니다.
     * - 원본 전표의 모든 라인의 차대변 방향을 반전시켜 생성합니다.
     * - 역분개 전표는 생성 즉시 승인(APPROVED) 상태로 두거나, 정책에 따라 DRAFT로 둘 수 있습니다.
     *   (여기서는 명시적 검토를 위해 DRAFT로 생성합니다)
     *
     * @param creator        역분개 작성자
     * @param accountingDate 역분개 회계 반영일
     * @param reason         역분개 사유
     * @return 생성된 역분개 전표 (id 없음)
     */
    public JournalEntry createReversal(String creator, LocalDate accountingDate, String reason) {
        assertPostedForReversal();
        if (accountingDate == null) {
            throw new IllegalArgumentException("역분개 회계 반영일은 필수입니다.");
        }
        String canonicalCreator = JournalActor.canonicalize(creator);
        String normalizedReason = requireReversalReason(reason);

        JournalEntry reversal = new JournalEntry();
        reversal.setSlipDate(LocalDate.now());
        reversal.setAccountingDate(accountingDate);
        reversal.setDescription(buildReversalDescription(normalizedReason));
        reversal.setEntryType("REVERSAL");
        reversal.setCurrencyCode(this.currencyCode);
        reversal.setExchangeRate(this.exchangeRate);
        reversal.setCreatedBy(canonicalCreator);
        reversal.setLineageSourceType("JOURNAL_ENTRY");
        reversal.setLineageSourceId(this.id.toString());

        for (JournalDetail originalDetail : this.details) {
            reversal.addDetail(originalDetail.copyWithFlippedSide());
        }

        reversal.validateBalance();
        return reversal;
    }

    /** 원본이 변경 불가능한 POSTED 전표인지 역분개 작업 조회 전에 확인합니다. */
    public void assertPostedForReversal() {
        if (this.status != JournalEntryStatus.POSTED) {
            throw new IllegalStateException("전기 완료된 전표만 역분개할 수 있습니다.");
        }
        if (this.id == null || this.id <= 0) {
            throw new IllegalStateException("저장된 원본 전표만 역분개할 수 있습니다.");
        }
    }

    /**
     * 아직 전기되지 않은 역분개 전표를 취소합니다.
     *
     * <p>일반 전표나 POSTED 역분개는 이 경로로 변경할 수 없습니다. 취소된 전표를
     * REJECTED로 확정하여 기존 승인·전기 경로가 다시 사용하지 못하게 합니다.</p>
     */
    public void cancelReversal(String actor, String reason) {
        assertHistoryMutable();
        if (!isReversal()) {
            throw new IllegalStateException("역분개 전표만 역분개 취소할 수 있습니다.");
        }
        if (this.status != JournalEntryStatus.DRAFT
                && this.status != JournalEntryStatus.REQUESTED
                && this.status != JournalEntryStatus.APPROVED) {
            throw new IllegalStateException("진행 중인 역분개 전표만 취소할 수 있습니다.");
        }
        String canonicalActor = JournalActor.canonicalize(actor);
        String normalizedReason = requireReversalReason(reason);
        this.status = JournalEntryStatus.REJECTED;
        this.rejectionReason = normalizedReason;
        this.auditUser = canonicalActor;
    }

    private String buildReversalDescription(String reason) {
        String sourceDescription = this.description == null ? "" : this.description;
        String reversalDescription = "[역분개 취소] " + sourceDescription + " (사유: " + reason + ")";
        if (reversalDescription.length() > 200) {
            throw new IllegalArgumentException("역분개 적요는 200자를 초과할 수 없습니다.");
        }
        return reversalDescription;
    }

    private static String requireReversalReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("역분개 사유는 필수입니다.");
        }
        String normalized = reason.trim();
        if (normalized.length() > MAX_REJECTION_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "역분개 사유는 " + MAX_REJECTION_REASON_LENGTH + "자를 초과할 수 없습니다.");
        }
        return normalized;
    }

    /**
     * 전표 Aggregate Root의 도메인 불변성(Invariant) 및 차대변 균형을 종합 검증합니다.
     *
     * 🐣 [초보자를 위한 설명]
     * 도메인 불변성(Invariant)이란 "시스템 동작 중에 절대로 깨져서는 안 되는 비즈니스 핵심 규칙"입니다.
     * 회계 전표에서 가장 중요한 불변식은 다음과 같습니다:
     * 1. 전표 작성일(slipDate) 및 회계 반영일(accountingDate) 등 필수 헤더 데이터가 존재해야 합니다.
     * 2. 복식부기(Double-entry bookkeeping) 원칙에 따라 차변 합계(Debit)와 대변 합계(Credit)가 정확히 일치해야 합니다.
     *
     * ─────────────────────────────────────────────────
     * [DDD & 헥사고날 아키텍처 관점의 교육적 설명]
     * 1. 도메인 응집도(Cohesion)와 캡슐화(Encapsulation):
     *    검증 로직이 Application Service나 외부 Validator 클래스에 흩어져 있으면 
     *    도메인 엔티티는 단순한 데이터 껍데기(Anemic Domain Model)로 전락합니다.
     *    Aggregate Root인 JournalEntry 내부로 비즈니스 규칙과 검증 로직을 일원화하여 High Cohesion을 달성합니다.
     *
     * 2. Aggregate Root의 불변성(Invariant) 보장:
     *    Aggregate Root는 바운디드 컨텍스트 내의 트랜잭션 경계(Consistency Boundary)입니다.
     *    JournalEntry는 자신과 하위 JournalDetail 엔티티 목록의 상태가 항상 유효함(Valid Invariant State)을 
     *    스스로 검증할 책임이 있습니다.
     * ─────────────────────────────────────────────────
     *
     * @throws IllegalStateException 도메인 불변식이나 차대변 정합성 검증 실패 시
     */
    public void validateInvariants() {
        validateRequiredHeaderFields();
        validateBalance();
    }

    /**
     * 전표 헤더의 필수 필드가 유효하게 설정되었는지 검증합니다.
     *
     * @throws IllegalStateException 필수 헤더 필드가 누락되었을 경우
     */
    private void validateRequiredHeaderFields() {
        if (this.slipDate == null) {
            throw new IllegalStateException("전표 작성일(slipDate)은 필수입니다.");
        }
        // Every entry, including event/machine entries, needs a durable maker identity.
        String canonicalMaker = JournalActor.canonicalize(this.createdBy);
        if (!Objects.equals(this.createdBy, canonicalMaker)) {
            assertHistoryMutable();
            this.createdBy = canonicalMaker;
        }
        if (this.accountingDate == null) {
            assertHistoryMutable();
            // 회계 반영일 미입력 시 전표 작성일로 기본 설정
            this.accountingDate = this.slipDate;
        }
    }

    /**
     * 차변/대변 합계 일치 여부 및 상세 라인 불변성을 검증합니다.
     *
     * [업무 설명]
     * 복식부기의 핵심 원칙: 모든 전표는 차변 합계 = 대변 합계여야 합니다.
     * 이 검증이 통과해야 전표 승인이 가능합니다.
     *
     * [예외 발생 조건]
     * 1. details가 비어있거나 2개 미만인 경우 (복식부기 라인 부족)
     * 2. 차변 라인 또는 대변 라인이 하나도 없는 경우
     * 3. 차변 합계 ≠ 대변 합계인 경우 (거래통화 및 기준통화 모두 체크)
     *
     * @throws IllegalStateException 정합성 검증 실패 시
     */
    public void validateBalance() {
        validateRequiredHeaderFields();

        if (details.size() < 2) {
            throw new IllegalStateException("복식부기 전표는 최소 두 개의 상세 라인이 필요합니다.");
        }

        details.forEach(JournalDetail::validateAccountingLine);
        if (details.stream().noneMatch(detail -> detail.getSide() == JournalSide.DEBIT)
                || details.stream().noneMatch(detail -> detail.getSide() == JournalSide.CREDIT)) {
            throw new IllegalStateException("전표에는 차변과 대변 라인이 각각 하나 이상 필요합니다.");
        }

        // 일반 전표는 각 통화 합계가 우연히 균형이어도 환율 의미가 틀릴 수 있으므로
        // 모든 라인의 환산을 검증합니다. 관리형 역분개는 이미 POSTED된 과거 원장의 실제
        // 금액을 그대로 상쇄해야 하며, 외부 createJournalEntry는 REVERSAL 생성을 거부합니다.
        if (!isReversal()) {
            JournalCurrencyConversionPolicy.validateBaseAmounts(currencyCode, exchangeRate, details);
        }

        // 각 라인은 DECIMAL(19,2)에 저장되지만 전표 합계 자체는 한 컬럼에 저장되지 않습니다.
        // 따라서 여러 개의 유효한 대형 라인을 더한 합계에 라인 저장 한도를 다시 적용하지
        // 않고, BigDecimal의 임의 정밀도로 합산한 뒤 차대 일치만 비교합니다.
        BigDecimal debitTotal = details.stream()
                .map(JournalDetail::debit)
                .map(Debit::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditTotal = details.stream()
                .map(JournalDetail::credit)
                .map(Credit::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal baseDebitTotal = details.stream()
                .map(JournalDetail::baseDebit)
                .map(Debit::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal baseCreditTotal = details.stream()
                .map(JournalDetail::baseCredit)
                .map(Credit::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalStateException(
                    String.format(
                            "거래통화 차대변 합계가 일치하지 않습니다. (차변: %s, 대변: %s)",
                            debitTotal,
                            creditTotal));
        }
        if (baseDebitTotal.compareTo(baseCreditTotal) != 0) {
            throw new IllegalStateException(
                    String.format(
                            "기준통화 차대변 합계가 일치하지 않습니다. (차변: %s, 대변: %s)",
                            baseDebitTotal,
                            baseCreditTotal));
        }
    }

    // ─── 상세 라인 관리 편의 메서드 ──────────────────────────

    /**
     * 전표에 상세 라인을 추가합니다.
     * 양방향 연관관계를 모두 설정합니다 (detail.journalEntry도 자동 설정).
     *
     * @param detail 추가할 전표 상세 라인
     */
    public void addDetail(JournalDetail detail) {
        assertHistoryMutable();
        if (detail == null) {
            throw new IllegalArgumentException("추가할 전표 상세는 필수입니다.");
        }
        detail.assertCanChangeJournalEntry(this);
        detail.setJournalEntry(this);
        details.add(detail);
    }

    /**
     * 전표에서 특정 상세 라인을 제거합니다.
     * orphanRemoval=true 설정으로 DB에서도 자동 삭제됩니다.
     *
     * @param detail 제거할 전표 상세 라인
     */
    public void removeDetail(JournalDetail detail) {
        assertHistoryMutable();
        detail.assertCanChangeJournalEntry(null);
        detail.setJournalEntry(null);
        details.remove(detail);
    }

    /**
     * 전표의 모든 상세 라인을 초기화합니다.
     * 전표 수정 시 기존 라인을 모두 제거하고 새로 추가할 때 사용합니다.
     */
    public void clearDetails() {
        assertHistoryMutable();
        this.details.forEach(detail -> detail.assertCanChangeJournalEntry(null));
        this.details.forEach(detail -> detail.setJournalEntry(null));
        this.details.clear();
    }

    // ─── Getter / Setter ──────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) {
        assertHistoryMutable();
        this.id = id;
    }

    public String getSlipNo() { return slipNo; }
    public void setSlipNo(String slipNo) {
        assertHistoryMutable();
        this.slipNo = slipNo;
    }

    public LocalDate getSlipDate() { return slipDate; }
    public void setSlipDate(LocalDate slipDate) {
        assertHistoryMutable();
        this.slipDate = slipDate;
    }

    public LocalDate getAccountingDate() { return accountingDate; }
    public void setAccountingDate(LocalDate accountingDate) {
        assertHistoryMutable();
        this.accountingDate = accountingDate;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        assertHistoryMutable();
        this.description = description;
    }

    public JournalEntryStatus getStatus() { return status; }

    /**
     * 신규 전표의 최초 상태를 지정합니다.
     *
     * <p>임의 상태 setter를 공개하면 호출자가 APPROVED/POSTED로 건너뛸 수 있습니다.
     * 그래서 생성 단계에서만 쓸 수 있는 의도 기반 메서드로 DRAFT 초기화를 제한합니다.</p>
     */
    public void initializeDraft() {
        assertHistoryMutable();
        if (this.status != null && this.status != JournalEntryStatus.DRAFT) {
            throw new IllegalStateException("이미 진행된 전표를 DRAFT로 되돌릴 수 없습니다.");
        }
        this.status = JournalEntryStatus.DRAFT;
    }

    public String getEntryType() { return entryType; }

    /** 대소문자나 주변 공백으로 역분개 전용 통제를 우회하지 못하게 의미를 판정합니다. */
    public boolean isReversal() {
        return entryType != null && "REVERSAL".equalsIgnoreCase(entryType.trim());
    }

    public void setEntryType(String entryType) {
        assertHistoryMutable();
        this.entryType = entryType;
    }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) {
        assertHistoryMutable();
        this.currencyCode = currencyCode == null || currencyCode.isBlank()
                ? null
                : currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) {
        assertHistoryMutable();
        this.exchangeRate = exchangeRate == null ? null : AccountingPrecision.exchangeRate(exchangeRate);
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) {
        assertHistoryMutable();
        this.rejectionReason = rejectionReason;
    }

    public List<JournalDetail> getDetails() { return List.copyOf(details); }
    public void setDetails(List<JournalDetail> details) {
        assertHistoryMutable();
        if (details == null) {
            throw new IllegalArgumentException("전표 상세 목록은 필수입니다.");
        }
        List<JournalDetail> replacement = new ArrayList<>(details);
        replacement.forEach(detail -> {
            if (detail == null) {
                throw new IllegalArgumentException("전표 상세는 필수입니다.");
            }
            detail.assertCanChangeJournalEntry(this);
        });
        clearDetails();
        replacement.forEach(this::addDetail);
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        assertHistoryMutable();
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) {
        assertHistoryMutable();
        String canonicalMaker = JournalActor.canonicalize(createdBy);
        if (this.createdBy != null && !JournalActor.sameIdentity(this.createdBy, canonicalMaker)) {
            throw new IllegalStateException("전표 작성자 identity는 변경할 수 없습니다.");
        }
        this.createdBy = canonicalMaker;
    }

    public String getApprovedBy() { return approvedBy; }

    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) {
        assertHistoryMutable();
        this.auditUser = auditUser == null ? null : JournalActor.canonicalize(auditUser);
    }

    public String getLineageSourceType() { return lineageSourceType; }
    public void setLineageSourceType(String lineageSourceType) {
        assertHistoryMutable();
        this.lineageSourceType = lineageSourceType;
    }

    public String getLineageSourceId() { return lineageSourceId; }
    public void setLineageSourceId(String lineageSourceId) {
        assertHistoryMutable();
        this.lineageSourceId = lineageSourceId;
    }

    boolean hasFinalHistory() {
        return isFinalStatus(this.status) || isFinalStatus(this.persistedStatus);
    }

    private void assertHistoryMutable() {
        if (hasFinalHistory()) {
            throw finalHistoryMutation();
        }
    }

    private static boolean isFinalStatus(JournalEntryStatus status) {
        return status == JournalEntryStatus.POSTED || status == JournalEntryStatus.REVERSED;
    }

    static IllegalStateException finalHistoryMutation() {
        return new IllegalStateException(FINAL_HISTORY_MUTATION_MESSAGE);
    }
}
