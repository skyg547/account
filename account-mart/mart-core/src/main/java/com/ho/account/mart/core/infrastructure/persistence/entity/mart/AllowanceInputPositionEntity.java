package com.ho.account.mart.core.infrastructure.persistence.entity.mart;

import com.ho.account.mart.core.domain.mart.AllowanceInputPositionId;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.shared.finance.enums.CustomerType;
import com.ho.account.shared.finance.enums.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [Infrastructure Layer Entity] IFRS 9 대손충당금 입력 포지션 JPA 엔티티.
 *
 * <p><strong>교육적 주석 (Pedagogical Comments):</strong></p>
 * <ul>
 *   <li><strong>헥사고날 아키텍처(Port & Adapter Pattern) 원칙:</strong> DB 기술(JPA, ORM, 테이블 스키마)은
 *       외부 영속성 어댑터(Infrastructure Layer)의 구현 세부사항입니다.</li>
 *   <li><strong>도메인-영속성 모델 분리:</strong> 도메인 클래스({@code AllowanceInputPosition})는 순수 Java 객체로
 *       핵심 금융/회계 비즈니스 개념을 정의하고, 본 엔티티 클래스가 데이터베이스 테이블({@code allowance_input_positions})과의
 *       O/R 매핑 및 영속성 수명주기를 전담 관리합니다.</li>
 * </ul>
 */
@Entity
@Table(name = "allowance_input_positions")
@IdClass(AllowanceInputPositionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceInputPositionEntity {

    @Id
    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDt;

    @Id
    @Column(name = "acc_no", nullable = false, length = 50)
    private String accNo;

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "cust_type", length = 30)
    private CustomerType customerType;

    @Column(name = "is_sme")
    private Boolean isSme;

    @Column(name = "country_cd", length = 10)
    private String countryCode;

    @Column(name = "prod_cd", length = 20)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "prod_category", length = 30)
    private ProductCategory productCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "bs_class", length = 10)
    private String bsClass;

    @Column(name = "cur_bal", precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "outstd_amt", precision = 19, scale = 4)
    private BigDecimal outstandingAmount;

    @Column(name = "limit_amt", precision = 19, scale = 4)
    private BigDecimal limitAmount;

    @Column(name = "market_val", precision = 19, scale = 4)
    private BigDecimal marketValue;

    @Column(name = "int_rate", precision = 10, scale = 6)
    private BigDecimal interestRate;

    @Column(name = "coupon_rate", precision = 10, scale = 6)
    private BigDecimal couponRate;

    @Column(name = "rate_type", length = 10)
    private String rateType;

    @Column(name = "base_rate_cd", length = 20)
    private String baseRateCode;

    @Column(name = "spread", precision = 10, scale = 6)
    private BigDecimal spread;

    @Column(name = "payment_freq")
    private Integer paymentFreq;

    @Column(name = "next_reset_dt")
    private LocalDate nextResetDate;

    @Column(name = "open_dt")
    private LocalDate openDate;

    @Column(name = "maturity_dt")
    private LocalDate maturityDate;

    @Column(name = "repayment_method", length = 20)
    private String repaymentMethod;

    @Column(name = "grace_period")
    private Integer gracePeriod;

    @Column(name = "repayment_freq")
    private Integer repaymentFreq;

    @Column(name = "int_rate_floor", precision = 10, scale = 6)
    private BigDecimal interestRateFloor;

    @Column(name = "int_rate_cap", precision = 10, scale = 6)
    private BigDecimal interestRateCap;

    @Column(name = "ref_index_cd", length = 20)
    private String refIndexCode;

    @Column(name = "internal_rating", length = 10)
    private String internalRating;

    @Column(name = "external_rating", length = 10)
    private String externalRating;

    @Column(name = "industry_cd", length = 20)
    private String industryCode;

    @Column(name = "warning_level", length = 20)
    private String warningLevel;

    @Column(name = "is_debt_restructured")
    private Boolean isDebtRestructured;

    @Column(name = "delinquent_days")
    private Integer delinquentDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "staging", length = 20)
    private CrStaging staging;

    @Column(name = "coll_amt", precision = 19, scale = 4)
    private BigDecimal collateralAmount;

    @Column(name = "recognized_coll_amt", precision = 19, scale = 4)
    private BigDecimal recognizedCollateralAmount;

    @Column(name = "coll_type", length = 20)
    private String collateralType;

    @Column(name = "pd", precision = 10, scale = 8)
    private BigDecimal pd;

    @Column(name = "lgd", precision = 10, scale = 8)
    private BigDecimal lgd;

    @Column(name = "expected_loss", precision = 19, scale = 4)
    private BigDecimal expectedLoss;

    @Column(name = "branch_cd", length = 10)
    private String branchCd;

    @Column(name = "biz_unit_cd", length = 10)
    private String bizUnitCd;

    @Column(name = "margin_rate", precision = 10, scale = 6)
    private BigDecimal marginRate;

    @Column(name = "repricing_freq")
    private Integer repricingFreq;
}
