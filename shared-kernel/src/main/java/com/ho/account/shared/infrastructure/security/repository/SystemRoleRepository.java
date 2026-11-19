package com.ho.account.shared.infrastructure.security.repository;

import com.ho.account.shared.infrastructure.security.domain.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemRoleRepository extends JpaRepository<SystemRole, Long> {

    Optional<SystemRole> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from SystemRole r where r.roleCode = :code")
    Optional<SystemRole> findByRoleCodeForDecision(@org.springframework.data.repository.query.Param("code") String code);
}
