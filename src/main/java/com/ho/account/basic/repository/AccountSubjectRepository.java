package com.ho.account.basic.repository;

import com.ho.account.basic.domain.AccountSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountSubjectRepository extends JpaRepository<AccountSubject, String> {
    Optional<AccountSubject> findByCode(String code);
}
