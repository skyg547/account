package com.ho.account.internalaudit.core.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rcm_control_activity")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ControlActivityJpaEntity {

    @Id
    private String controlId;
    
    private String riskId;
    private String controlDescription;
    private String controlType;
    private String executionMethod;
    private String frequency;
    private String ownerId;
}
