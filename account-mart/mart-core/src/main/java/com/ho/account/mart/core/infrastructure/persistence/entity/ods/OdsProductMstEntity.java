package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [원천 데이터] 상품 마스터(Product Master) 엔티티
 * 
 * 💡 [초보자를 위한 금융 개념 설명]
 * 은행의 '상품'은 단순히 눈에 보이는 물건이 아니라, 대출/예금/적금과 같은 금융 계약의 틀을 의미합니다.
 * 상품 마스터 정보는 각 계약이 어떤 성격(금리 방식, 상환 주기 등)을 가지지는 정의합니다.
 */
@Entity
@Table(name = "ods_product_mst")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdsProductMstEntity {

    /** 상품 코드 - 은행에서 정의한 유니크한 상품 식별 번호입니다. */
    @Id 
    @Column(name = "prod_cd", nullable = false, length = 20)
    private String productCode;

    /** 상품명 - 상품의 공식 명칭입니다. */
    @Column(name = "prod_nm", length = 100, nullable = false) 
    private String productName;

    /** 상품 카테고리 - 대분류 기준입니다 (예: LOAN: 대출, DEPOSIT: 수신, DERIV: 파생상품 등). */
    @Column(name = "prod_category", length = 30) 
    private String productCategory;

    @Column(name = "asset_liability_type", length = 20)
    private String assetLiabilityType;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    /** 회계 계정 코드 - 원장과 연계되는 계정 코드 */
    @Column(name = "subj_cd", length = 20)
    private String subjectCode;

    /** 금리 유형 - FIXED, FLOATING */
    @Column(name = "default_rate_type", length = 10) 
    private String defaultRateType;

    /** 금리 유형 (대손충당금 산출용) */
    @Column(name = "rate_type", length = 10)
    private String rateType;

    /** 이자 지급 주기 - 단위: 개월 */
    @Column(name = "default_payment_freq") 
    private Integer defaultPaymentFreq;

    /** 이자 지급 주기 (대손충당금 산출용) */
    @Column(name = "payment_freq")
    private Integer paymentFreq;

    /** 대손충당금(IFRS9) 산출 제외 여부 */
    @Builder.Default
    @Column(name = "is_excluded")
    private Boolean isExcluded = false;

    /** 난외 자산 여부 - 한도 대출 등 장부 외 거래 여부 */
    @Column(name = "is_off_balance") 
    @Builder.Default
    private Boolean isOffBalance = false;

    /** 
     * 기본 신용환산계수(CCF, Credit Conversion Factor)
     * 💡 [개념]: 부도가 났을 때 한도 잔액 중 실제로 인출될 것으로 예상되는 비율입니다.
     */
    @Column(name = "default_ccf", precision = 5, scale = 4) 
    private BigDecimal defaultCcf;

    @Column(name = "created_at") 
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

