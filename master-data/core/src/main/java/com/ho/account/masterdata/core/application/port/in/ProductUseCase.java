package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.domain.model.Product;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductUseCase {

    Product createProduct(ProductCommand command);

    Optional<Product> getProductById(Long id);

    Optional<Product> getProductByProductCode(String productCode);

    List<Product> getAllActiveProducts();

    Product updateProduct(Long id, ProductCommand command);

    void deactivateProduct(Long id);

    /**
     * 승인 워크플로에서 정한 종료일로 현재 SCD2 버전을 비활성화합니다.
     */
    void deactivateProduct(Long id, LocalDate effectiveDate);
}