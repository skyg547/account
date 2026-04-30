package com.ho.account.journalledger.domain.unsettled;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 미결 항목 (Unsettled Item) — 반제(Settlement) 처리가 필요한 미결 채권·채무.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 미결(未決) 항목이란 전표가 전기됐으나 아직 반제(상계·결제)가 완료되지 않은 항목입니다.
 *
 * 예시:
 *   - 매출채권(외상 매출): 물건을 팔았지만 아직 대금을 못 받음 → 미결
 *   - 매입채무(외상 매입): 물건을 샀지만 아직 대금을 안 냄 → 미결
 *   - 선급금: 지급했지만 해당 서비스/물품이 아직 납품 안 됨 → 미결
 *
 * 반제(Settlement) 처리 흐름:
 *   [OPEN] → settle(일부 금액) → [PARTIAL] → settle(나머지 금액) → [CLEARED]
 *
 * 주요 필드:
 *   - managementNo  : 미결 관리 번호 (고유, "UNS-" + UUID 앞 8자리 자동 생성)
 *   - originalAmount : 최초 미결 금액
 *   - settledAmount  : 현재까지 반제된 금액
 *   - remainingAmount: 아직 반제되지 않은 잔액 (= originalAmount - settledAmount)
 *   - status         : OPEN(미결) → PARTIAL(일부반제) → CLEARED(완전반제)
 *   - resolved       : CLEARED이면 true (조회 필터링 편의용)
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - unsettled_items 테이블에 매핑됩니다.
 * - settle(BigDecimal amount): 반제 금액을 입력하면 settledAmount 누적,
 *   remainingAmount 차감, 상태(status/resolved) 자동 갱신.
 * - UnsettledService.settleItem()에서 이 메서드를 호출합니다.
 * - managementNo는 @PrePersist에서 UUID 기반으로 자동 생성됩니다.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "unsettled_items")
@Getter @Setter
@NoArgsConstructor
public class UnsettledItem {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 미결 관리 번호 (유니크).
     * 형식: "UNS-XXXXXXXX" (UUID 앞 8자리 대문자)
     * @PrePersist에서 자동 생성됩니다.
     * 외부 시스템과 연동 시 이 번호로 미결 항목을 식별합니다.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String managementNo;

    /**
     * 원천 전표 상세 라인.
     * 이 미결 항목이 어느 전표 라인에서 발생했는지 추적합니다.
     * drill-down: UnsettledItem → JournalDetail → JournalEntry → 원천 문서
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_detail_id", nullable = false)
    private JournalDetail journalDetail;

    /**
     * 계정과목.
     * 미결 항목의 성격을 나타냅니다.
     * 예: 매출채권(11000), 매입채무(21100), 선급금(13100)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountSubject accountSubject;

    /**
     * 거래처.
     * 채권/채무가 누구에 대한 것인지를 나타냅니다.
     * null 가능: 거래처 없는 경우.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bp_id")
    private BusinessPartner businessPartner;

    /** 미결 발생일 (전표 전기일 기준) */
    @Column(nullable = false)
    private LocalDate occurrenceDate;

    /**
     * 최초 미결 금액.
     * 반제 후에도 변경되지 않습니다. settledAmount + remainingAmount = originalAmount.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    /**
     * 누적 반제 금액.
     * settle() 호출 시마다 증가합니다.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount = BigDecimal.ZERO;

    /**
     * 잔여 미결 금액 (= originalAmount - settledAmount).
     * 0이 되면 CLEARED 상태로 전환됩니다.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    /**
     * 미결 상태.
     * - OPEN    : 아직 반제가 없는 완전 미결 상태
     * - PARTIAL : 일부 반제 완료, 잔액 남아있음
     * - CLEARED : 전액 반제 완료 (resolved=true)
     */
    @Column(length = 20)
    private String status;

    /**
     * 완전 반제 여부.
     * status == "CLEARED"이면 true로 자동 설정됩니다.
     * 미결 항목 필터링 조회 시 사용합니다 (findByResolvedFalse).
     */
    @Column(nullable = false)
    private boolean resolved = false;

    // ─── 생명주기 콜백 ──────────────────────────────────────

    /**
     * 최초 저장 시 기본값 설정:
     * - status 기본값: "OPEN"
     * - remainingAmount 초기값: originalAmount와 동일
     * - managementNo 자동 생성: "UNS-" + UUID 앞 8자리 대문자
     */
    @PrePersist
    protected void onCreate() {
        if (status == null) status = "OPEN";
        if (remainingAmount == null) remainingAmount = originalAmount;
        if (managementNo == null) {
            managementNo = "UNS-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        updateResolvedStatus();
    }

    /** status == "CLEARED"이면 resolved = true로 동기화 */
    private void updateResolvedStatus() {
        this.resolved = "CLEARED".equals(this.status);
    }

    // ─── 도메인 비즈니스 메서드 ──────────────────────────────

    /**
     * 반제를 처리합니다.
     *
     * [업무 설명]
     * 거래처로부터 대금을 수령하거나, 거래처에 대금을 지급했을 때 호출합니다.
     * 반제 금액만큼 settledAmount를 누적하고 remainingAmount를 차감합니다.
     * - remainingAmount > 0: PARTIAL 상태
     * - remainingAmount == 0: CLEARED 상태 (resolved=true)
     *
     * [예외]
     * 반제 금액이 잔액보다 크면 IllegalArgumentException 발생.
     *
     * @param amount 반제할 금액 (양수여야 함)
     */
    public void settle(BigDecimal amount) {
        if (remainingAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException("반제 금액이 잔액보다 클 수 없습니다. 잔액: " + remainingAmount + ", 반제요청: " + amount);
        }
        this.settledAmount = this.settledAmount.add(amount);
        this.remainingAmount = this.remainingAmount.subtract(amount);

        // 잔액이 0이면 완전 반제, 아니면 부분 반제
        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = "CLEARED";
        } else {
            this.status = "PARTIAL";
        }
        updateResolvedStatus();
    }
}
