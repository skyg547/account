package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상품(Product) 마스터 엔티티
 * 은행의 여신/수신 상품 또는 일반 기업의 재화/용역을 관리합니다.
 * SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 관리합니다.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_code_valid", columnList = "product_code, valid_from, valid_to")
})
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품 코드 (SCD2를 위해 유니크 제약 조건 제거)
     */
    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    /**
     * 상품명
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 상품 설명
     */
    @Column(length = 500)
    private String description;

    /**
     * 단위 (예: EA, KG, L)
     */
    @Column(length = 20)
    private String unitOfMeasure;

    /**
     * 기본 단가 (금융 상품의 경우 이자율 등으로 활용 가능)
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    /**
     * 상품 유형 (예: PHYSICAL, SERVICE, DIGITAL, LOAN, DEPOSIT)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductType productType;

    /**
     * 유효 시작일(SCD2)
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * 유효 종료일(SCD2)
     */
    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 50)
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) this.auditUser = "SYSTEM";
        if (this.validFrom == null) this.validFrom = LocalDate.now();
        if (this.validTo == null) this.validTo = LocalDate.of(9999, 12, 31);
        if (this.price == null) this.price = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
