package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.DeferredItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DeferredItemType ?”í‹°?°ë? ?„í•œ Spring Data JPA Repository
 */
@Repository
public interface DeferredItemTypeRepository extends JpaRepository<DeferredItemType, Long> {
    Optional<DeferredItemType> findByCode(String code);
}
