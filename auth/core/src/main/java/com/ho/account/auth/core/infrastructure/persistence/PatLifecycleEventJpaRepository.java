package com.ho.account.auth.core.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface PatLifecycleEventJpaRepository extends JpaRepository<PatLifecycleEventJpaEntity, String> {
}
