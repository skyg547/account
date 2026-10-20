package com.ho.account.masterdata.core.domain.model;

import com.ho.account.masterdata.core.domain.policy.MasterDataFieldRules;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final BigDecimal MAX_PRICE = new BigDecimal("999999999999999.9999");
    private static final int PRICE_SCALE = 4;

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
     * NUMERIC(19,4)에 손실 없이 저장할 수 있는 단가만 허용합니다.
     * 의미 없는 후행 0은 허용하고 저장 스케일은 4로 맞춥니다.
     */
    public static BigDecimal requireValidPrice(BigDecimal value) {
        if (value == null) {
            throw new MasterDataFieldRules.FieldValidationException("Product price is required.");
        }
        if (value.signum() < 0) {
            throw new MasterDataFieldRules.FieldValidationException("Product price must be nonnegative.");
        }
        if (value.compareTo(MAX_PRICE) > 0) {
            throw new MasterDataFieldRules.FieldValidationException("Product price exceeds NUMERIC(19,4).");
        }
        try {
            return value.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new MasterDataFieldRules.FieldValidationException(
                    "Product price has more than four meaningful fractional digits.", ex);
        }
    }

    public void setPrice(BigDecimal price) {
        this.price = requireValidPrice(price);
    }

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

    /** Validate the effective row before SCD2 history is changed. */
    public void validateBusinessFields() {
        productCode = MasterDataFieldRules.code(productCode, "Product code", MasterDataFieldRules.PRODUCT_CODE_MAX);
        name = MasterDataFieldRules.requiredText(name, "Product name", MasterDataFieldRules.PRODUCT_NAME_MAX);
        description = MasterDataFieldRules.optionalText(description, "Product description", MasterDataFieldRules.PRODUCT_DESCRIPTION_MAX);
        unitOfMeasure = MasterDataFieldRules.optionalText(unitOfMeasure, "Product unitOfMeasure", MasterDataFieldRules.PRODUCT_UNIT_MAX);
        price = MasterDataFieldRules.productPrice(price);
        if (productType == null) {
            throw new MasterDataFieldRules.FieldValidationException("Product productType is required.");
        }
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
