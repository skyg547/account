package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountSubjectRepository extends JpaRepository<AccountSubject, Long> {

    @Query("""
            SELECT a FROM AccountSubject a
            WHERE a.code = :code
              AND a.validFrom <= :date
              AND a.validTo >= :date
            ORDER BY a.validFrom DESC
            """)
    Optional<AccountSubject> findActiveByCode(String code, LocalDate date);

    default Optional<AccountSubject> findByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    @Query("""
            SELECT a FROM AccountSubject a
            WHERE a.validFrom <= :date
              AND a.validTo >= :date
            ORDER BY a.code, a.validFrom
            """)
    List<AccountSubject> findActiveVersions(LocalDate date);

    boolean existsByCode(String code);

    long countByCode(String code);

    @Query("SELECT COUNT(a) FROM AccountSubject a WHERE a.validFrom <= :date AND a.validTo >= :date")
    long countActiveAt(LocalDate date);
}
