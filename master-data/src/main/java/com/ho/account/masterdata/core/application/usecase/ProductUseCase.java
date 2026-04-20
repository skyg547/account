package com.ho.account.masterdata.core.application.usecase;

import com.ho.account.basic.domain.Product;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import java.util.List;
import java.util.Optional;

public interface ProductUseCase {

    Product createProduct(ProductCommand command);

    Optional<Product> getProductById(Long id);

    Optional<Product> getProductByProductCode(String productCode);

    List<Product> getAllActiveProducts();

    Product updateProduct(Long id, ProductCommand command);

    void deactivateProduct(Long id);
}
