package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountSubjectRepository extends JpaRepository<AccountSubject, Long> {

    Optional<AccountSubject> findFirstByCodeOrderByValidFromDesc(String code);

    default Optional<AccountSubject> findByCode(String code) {
        return findFirstByCodeOrderByValidFromDesc(code);
    }

    boolean existsByCode(String code);

    @Query("SELECT COUNT(a) FROM AccountSubject a WHERE a.validFrom <= :date AND a.validTo >= :date")
    long countActiveAt(LocalDate date);
}