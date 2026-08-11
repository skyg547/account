package com.ho.account.masterdata.core.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 부서(Department/Cost Center) 도메인 모델
 * 조직 구조를 관리하며 SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 조직 개편 이력을 추적합니다.
 * 
 * 🐣 [DDD & Pure POJO 원칙 교육적 주석]
 * 1. Pure POJO 원칙:
 *    도메인 모델은 JPA 기술 어노테이션에 취약해지지 않도록 완전히 격리됩니다.
 * 2. 도메인-영속성 모델 분리:
 *    infrastructure/persistence/entity/DepartmentEntity와 Data Mapper를 활용하여 DB 스키마 변화에 영향받지 않습니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Department {

    private Long id;
    private String code;
    /**
     * 부서의 명칭
     */
    private String name;
    /**
     * 상위 부서 연관관계 (도메인 참조)
     */
    private Department parent;
    /**
     * 부서의 유형 (예: 비용 센터, 이익 센터, 지원 부서 등)
     */
    private DepartmentType type;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시
    private String auditUser; // 감사 사용자

    public enum DepartmentType {
        COST_CENTER, // 비용 센터
        PROFIT_CENTER, // 이익 센터
        SUPPORT, // 지원 부서
        OTHER // 기타
    }

    /**
     * 특정 시점에 유효한지 확인
     */
    public boolean isValid(LocalDate date) {
        return (date.isEqual(validFrom) || date.isAfter(validFrom)) && (date.isEqual(validTo) || date.isBefore(validTo));
    }

    /**
     * 현재 이력을 종료
     */
    public void terminate(LocalDate endDate) {
        this.validTo = endDate;
        this.updatedAt = LocalDateTime.now();
    }
}

