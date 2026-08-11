package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.AccountSubjectEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountSubjectRepository extends JpaRepository<AccountSubjectEntity, Long> {

    @Query("""
            SELECT a FROM AccountSubjectEntity a
            WHERE a.code = :code
              AND a.validFrom <= :date
              AND a.validTo >= :date
            ORDER BY a.validFrom DESC
            """)
    Optional<AccountSubjectEntity> findActiveByCode(String code, LocalDate date);

    default Optional<AccountSubjectEntity> findByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    @Query("""
            SELECT a FROM AccountSubjectEntity a
            WHERE a.validFrom <= :date
              AND a.validTo >= :date
            ORDER BY a.code, a.validFrom
            """)
    List<AccountSubjectEntity> findActiveVersions(LocalDate date);

    boolean existsByCode(String code);

    long countByCode(String code);

    @Query("SELECT COUNT(a) FROM AccountSubjectEntity a WHERE a.validFrom <= :date AND a.validTo >= :date")
    long countActiveAt(LocalDate date);
}

