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
@Table(name = "eval_design")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DesignEvaluationJpaEntity {

    @Id
    private String evaluationId;
    
    private String controlId;
    private String evaluatorId;
    private String evaluationDate;
    private String result;
    private String remarks;
}
