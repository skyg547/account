package com.ho.account.shared.infrastructure.security.repository;

import com.ho.account.shared.infrastructure.security.domain.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemRoleRepository extends JpaRepository<SystemRole, Long> {

    Optional<SystemRole> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);
}
