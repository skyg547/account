package com.ho.account.ecl.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [Entity] 대손충당금(IFRS9) 모델 파라미터.
 */
@Entity
@Table(name = "allowance_model_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceModelParameter {
    /** 파라미터 키 (예: PD_FLOOR, DEFAULT_DISCOUNT_RATE 등) */
    @Id
    @Column(name = "param_key")
    private String paramKey;

    /** 파라미터 값 */
    @Column(name = "param_value", nullable = false, precision = 19, scale = 8)
    private BigDecimal paramValue;

    /** 파라미터 상세 설명 */
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

