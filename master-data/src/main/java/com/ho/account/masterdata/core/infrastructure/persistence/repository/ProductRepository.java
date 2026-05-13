package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.Product;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByProductCodeOrderByValidFromDesc(String productCode);

    @Query("SELECT p FROM Product p WHERE p.productCode = :productCode AND p.validFrom <= :date AND p.validTo >= :date")
    Optional<Product> findActiveByProductCode(String productCode, LocalDate date);

    boolean existsByProductCode(String productCode);
}
