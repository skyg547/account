package com.ho.account.internalaudit.core.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "eval_deficiency")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DeficiencyJpaEntity {

    @Id
    private String deficiencyId;
    
    @Column(nullable = false)
    private String evaluationId;
    private String description;
    private String remediationPlan;
    private String status;
}
