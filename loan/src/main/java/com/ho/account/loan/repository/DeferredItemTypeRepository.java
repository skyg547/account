package com.ho.account.loan.repository;

import com.ho.account.loan.domain.DeferredItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DeferredItemType 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface DeferredItemTypeRepository extends JpaRepository<DeferredItemType, Long> {
    Optional<DeferredItemType> findByCode(String code);
}
