package com.ho.account.masterdata.api.dto;

import com.ho.account.masterdata.core.domain.model.Product;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public class ProductRequestDto {

    @NotBlank(message = "?곹뭹 肄붾뱶???꾩닔?낅땲??")
    private String productCode;

    @NotBlank(message = "?곹뭹紐낆? ?꾩닔?낅땲??")
    private String name;

    private String description;

    private String unitOfMeasure;

    @NotNull(message = "媛寃⑹? ?꾩닔?낅땲??")
    @PositiveOrZero(message = "媛寃⑹? 0 ?댁긽?댁뼱???⑸땲??")
    private Double price;

    @NotNull(message = "?곹뭹 ??낆? ?꾩닔?낅땲??")
    private Product.ProductType productType;

    @NotNull(message = "?좏슚 ?쒖옉?쇱? ?꾩닔?낅땲??")
    private LocalDate validFrom;

    @NotNull(message = "?좏슚 醫낅즺?쇱? ?꾩닔?낅땲??")
    private LocalDate validTo;

    // Getter 諛?Setter
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
