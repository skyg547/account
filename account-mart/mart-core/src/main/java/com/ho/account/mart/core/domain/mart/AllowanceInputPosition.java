package com.ho.account.mart.core.domain.mart;

import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.shared.finance.enums.CustomerType;
import com.ho.account.shared.finance.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [Domain Entity] Account Mart가 소유하는 IFRS 9 대손충당금 입력 포지션.
 *
 * <p><strong>교육적 주석 (Pedagogical Comments):</strong></p>
 * <ul>
 *   <li><strong>헥사고날 아키텍처 (Port and Adapter Pattern) 준수:</strong>
 *       도메인 엔티티는 특정 영속성 프레임워크(JPA, Hibernate)에 종속되지 않도록 Pure Java POJO로 작성되어야 합니다.
 *       기존에는 {@code @Entity}, {@code @Table}, {@code @IdClass}, {@code @Column}, {@code @Enumerated} 등
 *       JPA 기술 어노테이션이 도메인 레이어에 침범하여 계층 분리 원칙을 위반했었습니다.</li>
 *   <li><strong>도메인 모델의 순수성과 책임 격리:</strong>
 *       JPA 어노테이션을 전면 제거하고 Pure Java POJO로 격리함으로써 비즈니스 도메인의 순수성을 보장하고,
 *       단위 테스트 작성 시 DB 의존성을 배제할 수 있습니다.
 *       테이블 적재, ORM 매핑 및 스키마 관리는 Infrastructure 레이어의 {@code AllowanceInputPositionEntity}가 전담합니다.</li>
 *   <li><strong>Data Mapper 패턴 적용:</strong>
 *       도메인 POJO 모델과 JPA Entity 간의 양방향 데이터 변환은 어댑터 레이어의
 *       {@code AllowanceInputPositionMapper}에 위임합니다.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceInputPosition {

    private LocalDate baseDt;

    private String accNo;

    private String customerCode;

    private String customerName;

    private CustomerType customerType;

    private Boolean isSme;

    private String countryCode;

    private String productCode;

    private ProductCategory productCategory;

    private CurrencyCode currency;

    private String bsClass;

    private BigDecimal currentBalance;

    private BigDecimal outstandingAmount;

    private BigDecimal limitAmount;

    private BigDecimal marketValue;

    private BigDecimal interestRate;

    private BigDecimal couponRate;

    private String rateType;

    private String baseRateCode;

    private BigDecimal spread;

    private Integer paymentFreq;

    private LocalDate nextResetDate;

    private LocalDate openDate;

    private LocalDate maturityDate;

    private String repaymentMethod;

    private Integer gracePeriod;

    private Integer repaymentFreq;

    private BigDecimal interestRateFloor;

    private BigDecimal interestRateCap;

    private String refIndexCode;

    private String internalRating;

    private String externalRating;

    private String industryCode;

    private String warningLevel;

    private Boolean isDebtRestructured;

    private Integer delinquentDays;

    private CrStaging staging;

    private BigDecimal collateralAmount;

    private BigDecimal recognizedCollateralAmount;

    private String collateralType;

    private BigDecimal pd;

    private BigDecimal lgd;

    private BigDecimal expectedLoss;

    private String branchCd;

    private String bizUnitCd;

    private BigDecimal marginRate;

    private Integer repricingFreq;
}


