package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 상품 등록/수정 요청 DTO.
 *
 * <p>수정 요청에서는 생략한 필드를 현재 버전에서 상속합니다. 생성 필수값과 저장 한도는
 * core 도메인이 유효 상태 전체에 대해 검증합니다.</p>
 */
public class ProductRequestDto {

    private String productCode;

    private String name;

    private String description;

    private String unitOfMeasure;

    private BigDecimal price;

    @AssertTrue(message = "가격은 0 이상이며 NUMERIC(19,4)에 손실 없이 저장할 수 있어야 합니다.")
    public boolean isPriceExactlyRepresentable() {
        if (price == null) {
            return true; // core owns missing-price validation for CREATE vs partial UPDATE inheritance.
        }
        try {
            Product.requireValidPrice(price);
            return true;
        } catch (IllegalArgumentException invalidPrice) {
            return false;
        }
    }

    private Product.ProductType productType;

    private LocalDate validFrom;

    private LocalDate validTo;

    // Getter & Setter
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
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

    public ProductCommand toCommand() {
        return new ProductCommand(
                productCode,
                name,
                description,
                unitOfMeasure,
                price,
                productType,
                validFrom,
                validTo);
    }
}
