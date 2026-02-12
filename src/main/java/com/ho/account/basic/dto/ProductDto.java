package com.ho.account.basic.dto;

import com.ho.account.basic.domain.Product;
import java.time.LocalDate;

public class ProductDto {

    private Long id;
    private String productCode;
    private String name;
    private String description;
    private String unitOfMeasure;
    private Double price;
    private Product.ProductType productType;
    private LocalDate validFrom;
    private LocalDate validTo;

    public ProductDto() {
    }

    public ProductDto(Long id, String productCode, String name, String description, String unitOfMeasure, Double price, Product.ProductType productType, LocalDate validFrom, LocalDate validTo) {
        this.id = id;
        this.productCode = productCode;
        this.name = name;
        this.description = description;
        this.unitOfMeasure = unitOfMeasure;
        this.price = price;
        this.productType = productType;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    // Static factory method for conversion from Product entity
    public static ProductDto fromEntity(Product product) {
        return new ProductDto(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getDescription(),
                product.getUnitOfMeasure(),
                product.getPrice(),
                product.getProductType(),
                product.getValidFrom(),
                product.getValidTo()
        );
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

    public Product.ProductType getProductType() {
        return productType;
    }

    public void setProductType(Product.ProductType productType) {
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
}
