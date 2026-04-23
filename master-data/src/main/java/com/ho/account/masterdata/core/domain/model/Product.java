package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?πÎ?(Product) çÎçâ???????
 * ??Ñì ??¥—â‚Ñì õ¬???πÎ? ??íó ??ïÌâ¨??? ?ìΩ??∏Îï≤??
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ?πÎ? ?ÑÎ∂æ?
     */
    @Column(nullable = false, unique = true, length = 50)
    private String productCode;

    /**
     * ?πÎ??
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * ?πÎ? ??ªÏ±∏
     */
    @Column(length = 500)
    private String description;

    /**
     * ??êû (?? EA, KG, L)
     */
    @Column(length = 20)
    private String unitOfMeasure;

    /**
     * ≤Í≥ï????Ñì õ¬?
     */
    @Column(nullable = false)
    private Double price;

    /**
     * ?πÎ? ????(?? PHYSICAL, SERVICE, DIGITAL)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductType productType;

    /**
     * ?èÏäö ??ñÏòâ??(SCD2)
     */
    @Column(nullable = false)
    private LocalDate validFrom;

    /**
     * ?èÏäö ?ÇÖ??(SCD2)
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

    // Getter ?Setter
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
