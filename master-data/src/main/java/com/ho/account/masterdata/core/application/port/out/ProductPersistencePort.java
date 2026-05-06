package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.Product;
import java.util.List;
import java.util.Optional;

/**
 * 상품 영속성 포트 (Product Persistence Port)
 * 상품 마스터 데이터의 CRUD 및 조회를 담당합니다.
 */
public interface ProductPersistencePort {

    boolean existsByProductCode(String productCode);

    Optional<Product> findById(Long id);

    Optional<Product> findByProductCode(String productCode);

    List<Product> findAll();

    Product save(Product product);
}
