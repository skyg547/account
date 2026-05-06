package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 상품 등록/수정 요청 DTO
 */
public class ProductRequestDto {

    @NotBlank(message = "상품 코드는 필수입니다.")
    private String productCode;

    @NotBlank(message = "상품 명칭은 필수입니다.")
    private String name;

    private String description;

    private String unitOfMeasure;

    @NotNull(message = "가격은 필수입니다.")
    @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
    private BigDecimal price;

    @NotNull(message = "상품 유형은 필수입니다.")
    private Product.ProductType productType;

    @NotNull(message = "유효 시작일은 필수입니다.")
    private LocalDate validFrom;

    @NotNull(message = "유효 종료일은 필수입니다.")
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
