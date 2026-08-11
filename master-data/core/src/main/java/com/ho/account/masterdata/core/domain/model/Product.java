package com.ho.account.masterdata.core.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상품(Product) 도메인 모델
 * 은행의 여신/수신 상품 또는 일반 기업의 재화/용역을 관리합니다.
 * SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 관리합니다.
 * 
 * 🐣 [DDD & Pure POJO 원칙 교육적 주석]
 * 1. Pure POJO 원칙:
 *    도메인 객체는 JPA 어노테이션(@Entity, @Table 등)을 제거하여 순수한 자바 구조체로 유지합니다.
 * 2. 도메인-영속성 모델 분리:
 *    영속성(JPA) 어노테이션은 infrastructure/persistence/entity/ProductEntity로 격리하고,
 *    mapper/ProductMapper를 통해 변환함으로써 영속성 변경이 도메인 로직에 전파되지 않습니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Product {

    private Long id;

    /**
     * 상품 코드 (SCD2를 위해 유니크 제약 조건 제거)
     */
    private String productCode;

    /**
     * 상품명
     */
    private String name;

    /**
     * 상품 설명
     */
    private String description;

    /**
     * 단위 (예: EA, KG, L)
     */
    private String unitOfMeasure;

    /**
     * 기본 단가 (금융 상품의 경우 이자율 등으로 활용 가능)
     */
    private BigDecimal price;

    /**
     * 상품 유형 (예: PHYSICAL, SERVICE, DIGITAL, LOAN, DEPOSIT)
     */
    private ProductType productType;

    /**
     * 유효 시작일(SCD2)
     */
    private LocalDate validFrom;

    /**
     * 유효 종료일(SCD2)
     */
    private LocalDate validTo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String auditUser;

    public enum ProductType {
        PHYSICAL, SERVICE, DIGITAL, LOAN, DEPOSIT
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

