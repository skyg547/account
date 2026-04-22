package com.ho.account.masterdata.core.port.out;

import com.ho.account.basic.domain.Product;
import java.util.List;
import java.util.Optional;

/**
 * 상품 마스터 저장소 출력 포트입니다.
 *
 * <p>상품은 Banking/ERP 이벤트가 회계 룰을 탈 때 중요한 분류 키가 됩니다. core 계층은
 * 이 포트만 사용해서 상품 코드 중복, 조회, 저장을 수행합니다.</p>
 */
public interface ProductPersistencePort {

    boolean existsByProductCode(String productCode);

    Optional<Product> findById(Long id);

    Optional<Product> findByProductCode(String productCode);

    List<Product> findAll();

    Product save(Product product);
}
