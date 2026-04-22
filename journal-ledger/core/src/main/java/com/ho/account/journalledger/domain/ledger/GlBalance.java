package com.ho.account.journalledger.domain.ledger;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Currency;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 총계정원장 잔액 (General Ledger Balance) - Rich Domain Model
 * 함수형 스타일로 비즈니스 로직을 캡슐화합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_subject_id", nullable = false)
    private AccountSubject accountSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Column(nullable = false)
    private LocalDate balanceDate;

    @Column(nullable = false)
    private YearMonth period;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal beginningBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal endingBalance = BigDecimal.ZERO;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // --- 비즈니스 로직 (Functional Style) ---

    public void postDebit(Money amount) {
        this.debitAmount = this.debitAmount.add(amount.amount());
        this.recalculateBalance();
    }

    public void postCredit(Money amount) {
        this.creditAmount = this.creditAmount.add(amount.amount());
        this.recalculateBalance();
    }

    private void recalculateBalance() {
        // 잔액 = 기초 + 차변 - 대변 (또는 계정과목 성격에 따라 다름, 여기선 단순 합산 예시)
        this.endingBalance = this.beginningBalance.add(this.debitAmount).subtract(this.creditAmount);
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
