package com.ho.account.journalledger.domain.ledger.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 총계정원장 계정별 잔액 (General Ledger Account Balance)
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 회사의 특정 '계정과목(예: 보통예금)' 통장에 현재 돈이 얼마가 들어있는지(잔액)를 보여주는 요약 장부입니다.
 * 수백, 수천 개의 전표가 쌓일 때마다 매번 잔액을 새로 계산하면 시스템이 느려지기 때문에,
 * 전표가 원장(GL)에 전기(Posting)될 때마다 이 '잔액장'의 차변(들어온 돈)과 대변(나간 돈) 금액을 
 * 즉시 업데이트(updateBalance)해 두어 언제든 빠르게 현재 잔액을 조회할 수 있도록 합니다.
 */
@Entity
@Table(name = "gl_account_balances")
@Getter @Setter
@NoArgsConstructor
public class GlAccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false)
    private LocalDate balanceDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GlBalanceType balanceType; // DEBIT, CREDIT

    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private BigDecimal endingBalance = BigDecimal.ZERO;

    public void updateBalance(BigDecimal debit, BigDecimal credit) {
        this.debitAmount = this.debitAmount.add(debit);
        this.creditAmount = this.creditAmount.add(credit);
        // 계산 로직은 계정 유형(자산/부채)에 따라 달라질 수 있음
        this.endingBalance = this.debitAmount.subtract(this.creditAmount);
    }
}