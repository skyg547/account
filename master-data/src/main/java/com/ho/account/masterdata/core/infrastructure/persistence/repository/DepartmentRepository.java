package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.Department;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("SELECT d FROM Department d WHERE d.code = :code AND d.validFrom <= :date AND d.validTo >= :date")
    Optional<Department> findActiveByCode(String code, LocalDate date);

    default Optional<Department> findCurrentByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    @Query("SELECT d FROM Department d WHERE d.validFrom <= :date AND d.validTo >= :date")
    List<Department> findActiveVersions(LocalDate date);

    @Query("SELECT COUNT(d) FROM Department d WHERE d.validFrom <= :date AND d.validTo >= :date")
    long countActiveAt(LocalDate date);
}