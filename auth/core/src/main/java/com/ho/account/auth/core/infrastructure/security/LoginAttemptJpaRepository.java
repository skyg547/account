package com.ho.account.auth.core.infrastructure.security;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LoginAttemptJpaRepository extends JpaRepository<LoginAttemptJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from LoginAttemptJpaEntity attempt where attempt.username = :username")
    Optional<LoginAttemptJpaEntity> findByUsernameForUpdate(@Param("username") String username);

    // Only the winner creates the shared row; the caller then locks it before counting a failure.
    @Modifying
    @Query(value = """
            INSERT INTO auth_login_attempts (username, failure_count, updated_at)
            VALUES (:username, 0, :updatedAt)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertPlaceholderIfAbsent(@Param("username") String username, @Param("updatedAt") Instant updatedAt);
}
