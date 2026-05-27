package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [ODS] 금리 상세 정보 (Rate Info) 엔티티.
 * 금리 리스크(IRRBB) 산출을 위한 VaR, 민감도, 갭 분석 등의 핵심 입력 금리 데이터를 관리합니다.
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 테이블은 금리 리스크를 계산할 때 쓰는 '금리 명부'입니다.
 * 시장에서 공시되는 각종 기준금리뿐만 아니라, 은행 내부에서 정의한 가중평균금리 등을
 * 일별/만기별(Tenor)로 관리합니다. 이 금리 정보가 정확해야
 * 현재 우리 은행이 금리 변화에 얼마나 취약한지(VaR)를 비로소 계산할 수 있습니다.
 */
@Entity
@Table(name = "ods_rate_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdsRateInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 데이터 기준일자 - 해당 금리가 고시된 날짜입니다. */
    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDate;

    /** 금리 코드 - 금리를 식별하기 위한 코드입니다 (예: CD91, KORIBOR3M). */
    @Column(name = "rate_cd", length = 20, nullable = false)
    private String rateCode;

    /** 금리 명칭 - 사람이 이해할 수 있는 금리 이름입니다 (예: CD 91일물). */
    @Column(name = "rate_name", length = 100)
    private String rateName;

    /** 통화 코드 - 해당 금리가 적용되는 화폐 단위입니다 (KRW, USD 등). */
    @Column(name = "currency", length = 3)
    private String currency;

    /** 만기(Tenor) - 금리가 적용되는 기간을 개월 단위로 나타냅니다. */
    @Column(name = "tenor_months")
    private Integer tenorMonths;

    /** 
     * 금리 값 - 실제 고시된 금리 수치입니다 (단위: %). 
     * 소수점 이하 정밀도가 중요하므로 6자리까지 관리합니다.
     */
    @Column(name = "rate_val", precision = 10, scale = 6)
    private BigDecimal rateValue;

    /** 이자 계산 방식 - 이자를 어떻게 굴리는지 정의합니다 (단리, 복리 등). */
    @Column(name = "compounding_method", length = 20)
    private String compoundingMethod;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
