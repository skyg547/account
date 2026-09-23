package com.ho.account.auth.core.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AuthUserJpaRepository extends JpaRepository<AuthUserJpaEntity, String> {

    @EntityGraph(attributePaths = "roleAssignments")
    Optional<AuthUserJpaEntity> findByUsername(String username);

    @EntityGraph(attributePaths = "roleAssignments")
    List<AuthUserJpaEntity> findAllByOrderByUsernameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "roleAssignments")
    @Query("select user from AuthUserJpaEntity user where user.username = :username")
    Optional<AuthUserJpaEntity> findByUsernameForRoleAssignmentUpdate(@Param("username") String username);
}
