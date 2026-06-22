package com.ho.account.auth.core.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;

interface LoginAttemptJpaRepository extends JpaRepository<LoginAttemptJpaEntity, String> {
}
