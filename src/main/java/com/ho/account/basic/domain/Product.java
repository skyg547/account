package com.ho.account.basic.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상품(Product) 마스터 엔티티.
 * 판매 및 구매 가능한 상품 또는 서비스를 정의합니다.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품 코드
     */
    @Column(nullable = false, unique = true, length = 50)
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
     * 기본 판매 가격
     */
    @Column(nullable = false)
    private Double price;

    /**
     * 상품 타입 (예: PHYSICAL, SERVICE, DIGITAL)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductType productType;

    /**
     * 유효 시작일 (SCD2)
     */
    @Column(nullable = false)
    private LocalDate validFrom;

    /**
     * 유효 종료일 (SCD2)
     */
    @Column(nullable = false)
    private LocalDate validTo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    // Enum Definitions
    public enum ProductType {
        PHYSICAL, SERVICE, DIGITAL
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
