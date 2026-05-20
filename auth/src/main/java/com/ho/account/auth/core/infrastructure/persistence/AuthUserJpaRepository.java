package com.ho.account.auth.core.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthUserJpaRepository extends JpaRepository<AuthUserJpaEntity, String> {

    @EntityGraph(attributePaths = "roleAssignments")
    Optional<AuthUserJpaEntity> findByUsername(String username);
}
