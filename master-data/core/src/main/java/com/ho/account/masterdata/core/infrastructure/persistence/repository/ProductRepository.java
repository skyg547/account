package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.ProductEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    // 업무 키 잠금 전에 엔티티를 영속성 컨텍스트에 담으면 대기 후에도 낡은 상태가 재사용될 수 있습니다.
    @Query("SELECT p.productCode FROM ProductEntity p WHERE p.id = :id")
    Optional<String> findBusinessKeyById(Long id);

    List<ProductEntity> findByProductCodeOrderByValidFromDesc(String productCode);

    @Query("SELECT p FROM ProductEntity p WHERE p.productCode = :productCode AND p.validFrom <= :date AND p.validTo >= :date")
    Optional<ProductEntity> findActiveByProductCode(String productCode, LocalDate date);

    @Query("""
            SELECT p FROM ProductEntity p
            WHERE p.validFrom <= :date
              AND p.validTo >= :date
            ORDER BY p.productCode, p.validFrom
            """)
    List<ProductEntity> findActiveVersions(LocalDate date);

    boolean existsByProductCode(String productCode);

    long countByProductCode(String productCode);

    @Query("SELECT COUNT(p) FROM ProductEntity p WHERE p.validFrom <= :date AND p.validTo >= :date")
    long countActiveAt(LocalDate date);
}
