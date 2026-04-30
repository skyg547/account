package com.ho.account.journalledger.domain.ledger.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 총계정원장 잔액 (General Ledger Balance, GL Balance).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * GL Balance는 특정 계정과목 + 통화 + 날짜 조합에 대한 누적 잔액을 관리합니다.
 * 전표가 전기될 때마다 LedgerService가 이 잔액을 실시간으로 갱신합니다.
 *
 * 잔액 계산 공식:
 *   기말잔액(endingBalance) = 기초잔액(beginningBalance) + 차변합계(debitAmount) - 대변합계(creditAmount)
 *
 * 예시) 현금(10100) 계정의 1월 잔액:
 *   기초잔액:  5,000,000 (전월 이월)
 *   차변합계: +3,000,000 (현금 입금)
 *   대변합계: -1,500,000 (현금 지출)
 *   기말잔액: 6,500,000
 *
 * 이 데이터가 시산표(Trial Balance), 재무상태표(BS), 손익계산서(PL) 작성에 사용됩니다.
 *
 * 기초잔액 이월(Carry-forward):
 *   해당 날짜/기간에 대한 잔액 레코드가 없으면, 직전 잔액의 기말잔액을
 *   새 레코드의 기초잔액으로 이월합니다 (LedgerService 참고).
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - gl_balances 테이블에 매핑됩니다.
 * - 유니크 제약: account_id + currency_code + balance_date + period 조합은 유일합니다.
 * - LedgerService.updateGlBalance()에서 생성/갱신됩니다.
 * - addDebit() / addCredit(): 차변/대변 금액을 누적하고 recalculate()를 호출합니다.
 * - recalculate(): endingBalance를 재계산합니다. 데이터 정합성 보장을 위해
 *   addDebit/addCredit 호출 시마다 자동 실행됩니다.
 * - Carry-forward(기초잔액 이월): LedgerService에서 처리합니다.
 * ─────────────────────────────────────────────────
 */
@Entity
@Table(name = "gl_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_id", "currency_code", "balance_date", "period"})
}, indexes = {
    @Index(name = "idx_gl_balance_date", columnList = "balance_date")
})
@Getter
@Setter
@NoArgsConstructor
public class GlBalance {

    /** 시스템 내부 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 계정과목.
     * 잔액 집계의 기본 키. 예: 현금(10100), 매출채권(11000).
     * account_id + currency_code + balance_date + period가 유니크 조합입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountSubject accountSubject;

    /**
     * 통화.
     * 외화 잔액을 별도로 관리합니다. 원화면 KRW.
     * account_id + currency_code + balance_date + period가 유니크 조합입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    /**
     * 잔액 기준 날짜.
     * 일별로 잔액을 관리합니다. 같은 날짜에 여러 전기가 있으면 이 레코드에 누적됩니다.
     */
    @Column(name = "balance_date", nullable = false)
    private LocalDate balanceDate;

    /**
     * 회계 기간 (연월, YearMonth).
     * 예: 2026-01 (2026년 1월)
     * period별 집계 쿼리와 시산표 조회에 사용됩니다.
     */
    @Column(nullable = false)
    private YearMonth period;

    /**
     * 기초잔액 (Beginning Balance).
     * 해당 기간 시작 시점의 잔액 (직전 기간 기말잔액에서 이월).
     * 새 레코드 생성 시 직전 날짜의 endingBalance가 이 필드에 복사됩니다.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance = BigDecimal.ZERO;

    /**
     * 기간 내 차변 합계 (Debit Amount).
     * 해당 날짜에 전기된 모든 차변 GL Entry의 baseDrAmount 합계.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    /**
     * 기간 내 대변 합계 (Credit Amount).
     * 해당 날짜에 전기된 모든 대변 GL Entry의 baseCrAmount 합계.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    /**
     * 기말잔액 (Ending Balance).
     * = beginningBalance + debitAmount - creditAmount
     * addDebit() / addCredit() 호출 시 recalculate()에 의해 자동 갱신됩니다.
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
     *
     * [업무 설명]
     * 차변 거래(자산 증가, 비용 발생 등)가 발생할 때 호출됩니다.
     *
     * @param amount 기본통화(KRW) 기준 차변 금액 (GlEntry.baseDrAmount)
     */
    public void addDebit(BigDecimal amount) {
        this.debitAmount = (this.debitAmount == null ? BigDecimal.ZERO : this.debitAmount).add(amount);
        recalculate();
    }

    /**
     * 대변 금액을 누적하고 기말잔액을 재계산합니다.
     *
     * [업무 설명]
     * 대변 거래(자산 감소, 부채 증가, 수익 발생 등)가 발생할 때 호출됩니다.
     *
     * @param amount 기본통화(KRW) 기준 대변 금액 (GlEntry.baseCrAmount)
     */
    public void addCredit(BigDecimal amount) {
        this.creditAmount = (this.creditAmount == null ? BigDecimal.ZERO : this.creditAmount).add(amount);
        recalculate();
    }

    /**
     * 기말잔액을 재계산합니다.
     * 공식: endingBalance = beginningBalance + debitAmount - creditAmount
     * updatedAt도 함께 갱신합니다.
     *
     * [개발 참고]
     * - addDebit() / addCredit() 내부에서 자동 호출됩니다.
     * - LedgerService.mergeIntoGlBalance()에서도 직접 호출합니다.
     * - null 안전 처리: 각 필드가 null이면 BigDecimal.ZERO로 대체합니다.
     */
    public void recalculate() {
        BigDecimal beg = this.beginningBalance == null ? BigDecimal.ZERO : this.beginningBalance;
        BigDecimal dr  = this.debitAmount      == null ? BigDecimal.ZERO : this.debitAmount;
        BigDecimal cr  = this.creditAmount     == null ? BigDecimal.ZERO : this.creditAmount;
        this.endingBalance = beg.add(dr).subtract(cr);
        this.updatedAt = LocalDateTime.now();
    }

    /** 최초 저장 시 기본값 설정 및 기말잔액 초기 계산 */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.beginningBalance == null) this.beginningBalance = BigDecimal.ZERO;
        if (this.debitAmount      == null) this.debitAmount      = BigDecimal.ZERO;
        if (this.creditAmount     == null) this.creditAmount     = BigDecimal.ZERO;
        recalculate();
    }

    /** 수정 시 updatedAt 자동 갱신 */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
