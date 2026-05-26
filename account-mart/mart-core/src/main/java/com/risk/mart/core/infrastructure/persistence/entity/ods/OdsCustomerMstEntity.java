package com.risk.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

/**
 * [원천 데이터] 고객 마스터(Customer Master) 엔티티
 * 
 * 💡 [초보자를 위한 금융 개념 설명]
 * 이 클래스는 은행과 거래하는 '고객'의 기본 정보를 관리합니다.
 * 리스크 시스템에서 고객 정보는 매우 중요합니다. 
 * 고객이 개인인지 기업인지(고객 유형), 신용 등급은 어떠한지(신용도), 어떤 산업에 종사하는지(산업 분류)에 따라
 * 돈을 갚지 못할 확률(PD)이 크게 달라지기 때문입니다.
 */
@Entity
@Table(name = "ods_customer_mst")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdsCustomerMstEntity {

    @Id 
    @Column(name = "customer_code", length = 50) 
    @Comment("고객내부코드: 시스템 내부적으로 고객을 식별하기 위한 고유 번호")
    private String customerCode;

    @Column(name = "biz_no", length = 20) 
    @Comment("식별번호: 주민등록번호 또는 사업자등록번호")
    private String bizNo;

    @Column(name = "cust_nm", length = 100, nullable = false) 
    @Comment("고객명: 개인 성명 또는 법인 명칭")
    private String customerName;

    @Column(name = "cust_type", length = 20, nullable = false) 
    @Comment("고객유형: RETAIL(개인), CORPORATE(법인), SME(중소기업) 등 분류")
    private String customerType;

    @Column(name = "country_cd", length = 3) 
    @Comment("국가코드: ISO 국가 코드 (국가리스크 측정 시 사용)")
    private String countryCode;

    @Column(name = "internal_rating", length = 10) 
    @Comment("내부신용등급: 내부 등급법(IRB) 산출 시 사용되는 기초 등급")
    private String internalRating;

    @Column(name = "rating_cd", length = 10)
    @Comment("통합등급코드: 산출 엔진에서 매핑용으로 사용하는 통합 등급 코드")
    private String ratingCode;

    @Column(name = "external_rating", length = 10) 
    @Comment("외부신용등급: S&P, Moodys 등 외부 평가기관 등급 (표준방법 RW 매핑 시 사용)")
    private String externalRating;

    @Column(name = "industry_cd", length = 20) 
    @Comment("산업분류코드: 특정 산업군에 대한 집중도 리스크 분석 시 사용")
    private String industryCode;

    @Column(name = "industry_nm", length = 100) 
    @Comment("산업분류명")
    private String industryName;

    @Column(name = "is_sme")
    @Comment("중소기업여부: 바젤 III 규제상 SME 차주에 대한 기업규모 보정(Size Adjustment) 적용 여부 결정")
    private Boolean isSme;

    @Column(name = "branch_cd", length = 10)
    @Comment("관리지점코드")
    private String branchCode;

    @Column(name = "credit_status_cd", length = 10)
    @Comment("채무상태코드: 정상, 연체, 부도(Default) 상태 분류")
    private String creditStatusCd;
}
