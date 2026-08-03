package com.ho.account.asset.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [DDD(도메인 주도 설계) - Aggregate Root]
 * 리스 계약(Lease Contract) 엔티티 — IFRS 16 기준에 따른 리스 계약 정보를 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 리스는 남의 물건을 빌려 쓰는 것이지만, 회계적으로는 '내 것처럼' 장부에 올리는 복잡한 처리입니다.
 * 이 클래스는 "매달 낼 리스료는 얼마인지", "이자율은 몇 퍼센트인지"를 기억하고,
 * 이를 바탕으로 "지금 이 계약을 자산으로 환산하면 얼마짜리인지(사용권자산)"와 
 * "앞으로 갚아야 할 빚은 얼마인지(리스부채)"를 계산하는 기준이 됩니다.
 * 타 모듈(Master Data)과는 ID(lessorCode 등) 기반으로 약하게 결합되어 있습니다.
 */
@Entity
@Table(name = "lease_contracts")
@Getter @Setter
@NoArgsConstructor
public class LeaseContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String contractNo;

    @Column(nullable = false)
    private String contractName;

    @Column(name = "lessor_code", length = 20)
    private String lessorCode;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(nullable = false)
    private int paymentDay;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal discountRate; // 증분차입이자율

    @Column(precision = 19, scale = 2)
    private BigDecimal initialRightOfUseAssetValue; // 초기 사용권자산 PV
    @Column(precision = 19, scale = 2)
    private BigDecimal initialLeaseLiabilityValue; // 초기 리스부채 PV

    private String status; // ACTIVE, TERMINATED, MODIFIED

    @Column(name = "dept_code", length = 20)
    private String departmentCode;

    @Column(name = "expense_account_code", length = 20)
    private String expenseAccountCode;

    @Column(name = "ifrs16_applicable", nullable = false)
    private boolean ifrs16Applicable = true;
    @Column(nullable = false)
    private boolean shortTermLease = false;
    @Column(nullable = false)
    private boolean lowValueLease = false;

    public void validateForRegistration() {
        if (paymentDay < 1 || paymentDay > 31) {
            throw new IllegalArgumentException("payment day must be between 1 and 31");
        }
    }
}
