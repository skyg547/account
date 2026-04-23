package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountSubjectRepository extends JpaRepository<AccountSubject, Long> {
    Optional<AccountSubject> findFirstByCodeOrderByValidFromDesc(String code);

    default Optional<AccountSubject> findByCode(String code) {
        return findFirstByCodeOrderByValidFromDesc(code);
    }

    boolean existsByCode(String code);
}
