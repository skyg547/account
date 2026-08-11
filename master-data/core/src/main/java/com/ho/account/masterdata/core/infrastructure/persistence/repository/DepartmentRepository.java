package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.DepartmentEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    @Query("SELECT d FROM DepartmentEntity d WHERE d.code = :code AND d.validFrom <= :date AND d.validTo >= :date")
    Optional<DepartmentEntity> findActiveByCode(String code, LocalDate date);

    default Optional<DepartmentEntity> findCurrentByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    @Query("SELECT d FROM DepartmentEntity d WHERE d.validFrom <= :date AND d.validTo >= :date")
    List<DepartmentEntity> findActiveVersions(LocalDate date);

    boolean existsByCode(String code);

    long countByCode(String code);

    @Query("SELECT COUNT(d) FROM DepartmentEntity d WHERE d.validFrom <= :date AND d.validTo >= :date")
    long countActiveAt(LocalDate date);
}