package com.ho.account.internalaudit.core.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "eval_operating")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OperatingEvaluationJpaEntity {

    @Id
    private String evaluationId;
    
    @Column(nullable = false)
    private String controlId;
    private String evaluatorId;
    private String evaluationDate;
    private Integer sampleSize;
    private Integer exceptionCount;
    
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> evidenceFilePaths;
    
    private String result;
    private String remarks;
}
