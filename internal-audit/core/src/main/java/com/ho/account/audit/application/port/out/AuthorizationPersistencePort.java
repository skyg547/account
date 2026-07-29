package com.ho.account.shared.infrastructure.security.application.port.out;

import com.ho.account.shared.infrastructure.security.domain.AccessType;
import com.ho.account.shared.infrastructure.security.domain.Authorization;
import java.util.List;
import java.util.Optional;

public interface AuthorizationPersistencePort {

    boolean existsByRoleIdAndFunctionCodeAndAccessType(Long roleId, String functionCode, AccessType accessType);

    Authorization save(Authorization authorization);

    Optional<Authorization> findById(Long authorizationId);

    void deleteById(Long authorizationId);

    List<Authorization> findByRoleId(Long roleId);
}
