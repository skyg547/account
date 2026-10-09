package com.ho.account.auth.core.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonalAccessTokenJpaRepository extends JpaRepository<PersonalAccessTokenJpaEntity, String> {
    List<PersonalAccessTokenJpaEntity> findByUsernameOrderByCreatedAtDesc(String username);
    List<PersonalAccessTokenJpaEntity> findAllByOrderByCreatedAtDesc();
    Optional<PersonalAccessTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PersonalAccessTokenJpaEntity token set token.status = 'REVOKED' "
            + "where token.id = :tokenId and token.status = 'ACTIVE'")
    int markRevokedIfActive(@Param("tokenId") String tokenId);
}
