package com.ho.account.ecl.core.domain.exposure;

import com.ho.account.shared.finance.entity.BaseEntity;
import com.ho.account.shared.finance.enums.CrStaging;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [Entity] 대손충당금(IFRS9) 익스포저 계좌(Account) 원장 엔티티
 * 개별 대출, 채권, 보증 등 리스크가 발생하는 모든 계약 거래 정보를 관리합니다.
 *
 * [초보자를 위한 개념 설명]
 * '계좌'는 대손충당금(IFRS9) 산출의 가장 기본 단위입니다. 
 * 고객(차주) 한 명이 여러 개의 대출(계좌)을 가질 수 있으며, 
 * 각 계좌마다 전체 한도, 잔액, 담보 설정 현황이 다르기 때문에 
 * 개별적으로 리스크를 정밀하게 측정해야 합니다.
 */
@Entity
@Table(name = "cr_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 계좌 번호 */
    @Column(name = "account_no", nullable = false, unique = true, length = 50)
    private String accountNo;

    /** 차주 정보 (Many-to-One) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CrCustomer customer;

    /** 상품 코드 (CrProductMaster 연계) */
    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    /** 통화 코드 (KRW, USD 등) */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** 약정 한도 금액 (Limit) */
    @Column(name = "notional_amt", nullable = false, precision = 19, scale = 4)
    private BigDecimal notionalAmount;

    /** 실행 잔액 (Outstanding, On-Balance) */
    @Column(name = "outstanding_amt", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingAmount;

    /** [고도화] 상품 분류 (LOAN, BOND, GUARANTEE 등) */
    @Column(name = "prod_category", length = 30)
    private String productCategory;

    /** [고도화] 적용 금리 */
    @Column(name = "int_rate", precision = 19, scale = 6)
    private BigDecimal interestRate;

    /** [고도화] 상환 방법 (BULLET, AMORTIZATION 등) */
    @Column(name = "repayment_method", length = 30)
    private String repaymentMethod;

    /** [고도화] 원금 거치 기간 (개월) */
    @Column(name = "grace_period")
    private Integer gracePeriod;

    /** [고도화] 원금 상환 주기 (개월) */
    @Column(name = "repayment_freq")
    private Integer repaymentFreq;

    /** [고도화] 관리 영업점 코드 */
    @Column(name = "branch_cd", length = 20)
    private String branchCode;

    /** [고도화] 사업 단위 코드 (CB, RB 등) */
    @Column(name = "biz_unit_cd", length = 20)
    private String bizUnitCode;

    /** IFRS 9 스테이징 상태 (STAGE 1, 2, 3) */
    @Enumerated(EnumType.STRING)
    @Column(name = "staging", length = 20)
    @Builder.Default
    private CrStaging staging = CrStaging.STAGE1;

    /** 연체 일수 */
    @Column(name = "delinquent_days")
    @Builder.Default
    private Integer delinquentDays = 0;

    /** 최초 취급 일자 */
    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    /** 만기일 */
    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    /** 최초 취급 시점 등급 (SICR 판정용) */
    @Column(name = "original_rating", length = 20)
    private String originalRating;
    
    /** [고도화] 계좌별 개별 내부 신용 등급 (차주 등급보다 우선 적용) */
    @Column(name = "internal_rating", length = 10)
    private String internalRating;

    /** 채권 재조정 여부 */
    @Column(name = "is_debt_restructured")
    @Builder.Default
    private Boolean isDebtRestructured = false;

    /** 활성 상태 여부 */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /** 데이터 품질 점검 오류 메시지 */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
