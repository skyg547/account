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
@Table(name = "rcm_risk")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RcmRiskJpaEntity {

    @Id
    private String riskId;
    
    @Column(nullable = false)
    private String processId;
    private String riskDescription;
    private String impactLevel;
    private String likelihood;
}
