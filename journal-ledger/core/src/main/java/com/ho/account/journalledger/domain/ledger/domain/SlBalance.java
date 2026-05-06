package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;

/**
 * 보조원장 잔액 (Subsidiary Ledger Balance, SL Balance).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * GlBalance가 계정과목 단위 잔액을 관리한다면,
 * SlBalance는 계정과목 + 거래처 + 부서 + 통화 조합 단위의 상세 잔액을 관리합니다.
 *
 * 사용 사례:
 *   - 매출채권(11000): 거래처별 채권 잔액 조회 → 누가 얼마를 빌렸는지
 *   - 매입채무(21100): 거래처별 채무 잔액 조회 → 누구에게 얼마를 갚아야 하는지
 *   - 복리후생비(82100): 부서별 비용 집계 → 어떤 부서가 얼마나 썼는지
 *
 * 잔액 계산 공식 (GlBalance와 동일):
 *   기말잔액 = 기초잔액 + 차변합계 - 대변합계
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - sl_balances 테이블에 매핑됩니다.
 * - 유니크 제약: account_id + bp_id + dept_id + currency_code + balance_date + period
 * - LedgerService.updateSlBalance()에서 생성/갱신됩니다.
 * - GlBalance와 구조 동일하나 businessPartner, department 집계 키가 추가됩니다.
 * - businessPartner, department는 null 허용 (거래처/부서 없는 계정도 지원).
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "sl_balances", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sl_balance_key", columnNames = {"account_id", "bp_id", "dept_id", "currency_code", "balance_date", "period"})
}, indexes = {
    @Index(name = "idx_sl_balance_lookup", columnList = "account_id, bp_id, dept_id, balance_date"),
    @Index(name = "idx_sl_balance_period", columnList = "period, account_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SlBalance {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 계정과목 (집계 1차 키) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountSubject accountSubject;

    /**
     * 거래처 (집계 2차 키).
     * null 허용: 거래처 없는 계정(예: 현금)은 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bp_id")
    private BusinessPartner businessPartner;

    /**
     * 귀속 부서 (집계 3차 키).
     * null 허용: 부서 귀속 불필요한 계정은 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department;

    /** 통화 (집계 4차 키) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    /** 잔액 기준 날짜 */
    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    /** 회계 기간 (연월, YearMonth) */
    @Column(nullable = false)
    private YearMonth period;

    /** 기초잔액 (직전 기간 기말잔액에서 이월) */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance = BigDecimal.ZERO;

    /** 기간 내 차변 합계 */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    /** 기간 내 대변 합계 */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    /**
     * 기말잔액 (= 기초 + 차변 - 대변).
     * recalculate() 호출 시 자동 갱신됩니다.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance = BigDecimal.ZERO;

    /** 최초 생성 일시 (수정 불가) */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 최종 수정 일시 */
    private LocalDateTime updatedAt;

    // ─── 도메인 비즈니스 메서드 ──────────────────────────────

    /**
     * 차변 금액을 누적하고 기말잔액을 재계산합니다.
     * @param amount 기본통화(KRW) 기준 차변 금액
     */
    public void addDebit(BigDecimal amount) {
        this.debitAmount = (this.debitAmount == null ? BigDecimal.ZERO : this.debitAmount).add(amount);
        recalculate();
    }

    /**
     * 대변 금액을 누적하고 기말잔액을 재계산합니다.
     * @param amount 기본통화(KRW) 기준 대변 금액
     */
    public void addCredit(BigDecimal amount) {
        this.creditAmount = (this.creditAmount == null ? BigDecimal.ZERO : this.creditAmount).add(amount);
        recalculate();
    }

    /**
     * 기말잔액 재계산: endingBalance = beginningBalance + debitAmount - creditAmount
     */
    public void recalculate() {
        BigDecimal beg = this.beginningBalance == null ? BigDecimal.ZERO : this.beginningBalance;
        BigDecimal dr  = this.debitAmount      == null ? BigDecimal.ZERO : this.debitAmount;
        BigDecimal cr  = this.creditAmount     == null ? BigDecimal.ZERO : this.creditAmount;
        this.endingBalance = beg.add(dr).subtract(cr);
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.beginningBalance == null) this.beginningBalance = BigDecimal.ZERO;
        if (this.debitAmount      == null) this.debitAmount      = BigDecimal.ZERO;
        if (this.creditAmount     == null) this.creditAmount     = BigDecimal.ZERO;
        recalculate();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
