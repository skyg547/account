package com.ho.account.masterdata.core.application.command;

import com.ho.account.basic.domain.Product;
import java.time.LocalDate;

public record ProductCommand(
        String productCode,
        String name,
        String description,
        String unitOfMeasure,
        Double price,
        Product.ProductType productType,
        LocalDate validFrom,
        LocalDate validTo) {

    public Product toEntity() {
        Product product = new Product();
        product.setProductCode(productCode);
        product.setName(name);
        product.setDescription(description);
        product.setUnitOfMeasure(unitOfMeasure);
        product.setPrice(price);
        product.setProductType(productType);
        product.setValidFrom(validFrom);
        product.setValidTo(validTo);
        return product;
    }
}
