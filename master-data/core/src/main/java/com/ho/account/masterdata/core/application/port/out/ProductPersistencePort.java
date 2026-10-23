package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.Product;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 상품 영속성 포트 (Product Persistence Port)
 * 상품 마스터 데이터의 CRUD 및 조회를 담당합니다.
 */
public interface ProductPersistencePort {

    boolean existsByProductCode(String productCode);

    Optional<Product> findById(Long id);

    /** 업무 키 잠금 후 ID가 지정한 행의 최신 유효기간을 잠가 읽습니다. */
    Optional<Product> findByIdForUpdate(Long id);

    /** 변경 가능한 aggregate를 미리 읽지 않고 잠글 업무 키만 조회합니다. */
    Optional<String> findBusinessKeyById(Long id);

    Optional<Product> findByProductCode(String productCode);

    Optional<Product> findActiveByProductCode(String productCode);

    /** 업무 키 잠금 후, 기존 영속성 캐시 대신 최신 현재 버전을 잠가 읽습니다. */
    Optional<Product> findActiveByProductCodeForUpdate(String productCode);

    List<Product> findAll();

    List<Product> findAllActive(LocalDate asOfDate);

    Product save(Product product);
}
