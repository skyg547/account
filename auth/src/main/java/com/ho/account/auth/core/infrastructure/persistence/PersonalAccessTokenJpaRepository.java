package com.ho.account.auth.core.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalAccessTokenJpaRepository extends JpaRepository<PersonalAccessTokenJpaEntity, String> {
    List<PersonalAccessTokenJpaEntity> findByUsernameOrderByCreatedAtDesc(String username);
    List<PersonalAccessTokenJpaEntity> findAllByOrderByCreatedAtDesc();
    Optional<PersonalAccessTokenJpaEntity> findByTokenHash(String tokenHash);
}
