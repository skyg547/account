package com.risk.credit.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [Entity] 신용 리스크 규제 파라미터.
 * 바젤(Basel) 규제 및 내부 정책에 따라 전역적으로 사용되는 산출 계수들을 관리합니다.
 *
 * [초보자를 위한 개념 설명]
 * 리스크 산출 공식에는 상황에 따라 변할 수 있는 '상수'들이 필요합니다. 
 * 예를 들어 "부도율(PD)의 최저 하한선(Floor)은 0.05%로 한다"거나, 
 * "내부등급법 산식에서 기업 자산의 상관계수 비중은 얼마로 한다" 같은 
 * 법적/정책적 기준값들을 여기서 관리합니다.
 */
@Entity
@Table(name = "cr_regulatory_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrRegulatoryParameter {
    /** 파라미터 키 (예: PD_FLOOR, CORRELATION_FACTOR 등) */
    @Id
    @Column(name = "param_key")
    private String paramKey;

    /** 파라미터 값 */
    @Column(name = "param_value", nullable = false, precision = 19, scale = 8)
    private BigDecimal paramValue;

    /** 파라미터 상세 설명 (단위, 규제 근거 등) */
    @Column(name = "description")
    private String description;

    /** 업데이트 일시 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
