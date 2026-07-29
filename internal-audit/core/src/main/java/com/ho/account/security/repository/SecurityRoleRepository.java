package com.ho.account.security.repository;

import com.ho.account.security.domain.SecurityRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecurityRoleRepository extends JpaRepository<SecurityRole, String> {
    Optional<SecurityRole> findByRoleCode(String roleCode);
}
