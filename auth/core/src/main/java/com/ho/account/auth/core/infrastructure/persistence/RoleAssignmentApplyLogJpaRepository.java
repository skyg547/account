package com.ho.account.auth.core.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface RoleAssignmentApplyLogJpaRepository
        extends JpaRepository<RoleAssignmentApplyLogJpaEntity, String> {
}
