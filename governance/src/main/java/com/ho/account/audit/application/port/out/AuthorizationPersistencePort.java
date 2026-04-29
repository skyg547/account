package com.ho.account.audit.application.port.out;

import com.ho.account.audit.domain.AccessType;
import com.ho.account.audit.domain.Authorization;
import java.util.List;

public interface AuthorizationPersistencePort {

    boolean existsByRoleIdAndFunctionCodeAndAccessType(Long roleId, String functionCode, AccessType accessType);

    Authorization save(Authorization authorization);

    void deleteById(Long authorizationId);

    List<Authorization> findByRoleId(Long roleId);
}

