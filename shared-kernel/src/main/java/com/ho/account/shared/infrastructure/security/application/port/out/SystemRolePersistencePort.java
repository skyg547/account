package com.ho.account.shared.infrastructure.security.application.port.out;

import com.ho.account.shared.infrastructure.security.domain.SystemRole;
import java.util.List;
import java.util.Optional;

public interface SystemRolePersistencePort {

    boolean existsByRoleCode(String roleCode);

    SystemRole save(SystemRole role);

    List<SystemRole> findAll();

    Optional<SystemRole> findByRoleCode(String roleCode);
}

