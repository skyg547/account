package com.ho.account.ecl.core.domain.exposure;

import com.ho.account.shared.finance.entity.BaseEntity;
import com.ho.account.shared.finance.enums.CustomerType;
import jakarta.persistence.*;
import lombok.*;

/**
 * [Entity] 차주(Counterparty) 마스터 엔티티
 * 대손충당금(IFRS9) 산출 대상이 되는 고객(개인, 기업 등)의 기본 정보를 관리합니다.
 *
 * [초보자를 위한 개념 설명]
 * 리스크 관리에서 '차주'란 은행에서 돈을 빌려간 사람이나 기업을 말합니다. 
 * 차주의 신용등급(Internal/External Rating)과 산업 분류는 
 * "이 고객이 부도날 확률(PD)이 얼마인가"를 결정하는 가장 중요한 기초 데이터가 됩니다.
 */
@Entity
@Table(name = "cr_customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrCustomer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 고객 번호 (고유 식별자) */
    @Column(name = "customer_code", nullable = false, unique = true, length = 50)
    private String customerCode;

    /** 고객명 / 법인명 */
    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    /** 차주 유형 (CORPORATE, RETAIL, SME 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 30)
    private CustomerType customerType;

    /** 내부 신용 등급 (예: 1, 2, ..., 10 또는 AAA, AA, ...) */
    @Column(name = "internal_rating", length = 10)
    private String internalRating;

    /** 외부 신용 등급 (S&P, Moody's 등) */
    @Column(name = "external_rating", length = 10)
    private String externalRating;

    /** 산업 분류 코드 */
    @Column(name = "industry_code", length = 20)
    private String industryCode;

    /** 국가 코드 (ISO 3166-1 alpha-2) */
    @Column(name = "country_code", length = 2)
    private String countryCode;

    /** 중소기업(SME) 여부 */
    @Column(name = "is_sme")
    @Builder.Default
    private Boolean isSme = false;

    /** 
     * 금융기관 여부 및 세부 업권 코드 (BANK, SECURITIES, INSURANCE 등)
     * 💡 [비즈니스 가이드] 금융기관 간의 거래는 서로 리스크가 전염되기 쉬워 
     * 상관계수를 1.25배 할증하여 자본을 더 많이 쌓게 합니다.
     */
    @Column(name = "fin_sector_cd", length = 20)
    private String financialSectorCode;

    /** 연매출액 (단위: 억 원) - SME 상관계수 보정 산출에 사용됩니다. */
    @Column(name = "annual_sales", precision = 19, scale = 4)
    private java.math.BigDecimal annualSales;

    /** 조기경보 레벨 (NORMAL, WATCH, WARNING, CRITICAL) */
    @Column(name = "warning_level", length = 20)
    @Builder.Default
    private String warningLevel = "NORMAL";

    /** 활성 상태 여부 */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
