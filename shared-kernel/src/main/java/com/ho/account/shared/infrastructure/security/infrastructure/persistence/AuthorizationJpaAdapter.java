package com.ho.account.shared.infrastructure.security.infrastructure.persistence;

import com.ho.account.shared.infrastructure.security.application.port.out.AuthorizationPersistencePort;
import com.ho.account.shared.infrastructure.security.domain.AccessType;
import com.ho.account.shared.infrastructure.security.domain.Authorization;
import com.ho.account.shared.infrastructure.security.repository.AuthorizationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationJpaAdapter implements AuthorizationPersistencePort {

    private final AuthorizationRepository authorizationRepository;

    @Override
    public boolean existsByRoleIdAndFunctionCodeAndAccessType(Long roleId, String functionCode, AccessType accessType) {
        return authorizationRepository.existsByRoleIdAndFunctionCodeAndAccessType(roleId, functionCode, accessType);
    }

    @Override
    public Authorization save(Authorization authorization) {
        return authorizationRepository.save(authorization);
    }

    @Override
    public Optional<Authorization> findById(Long authorizationId) {
        return authorizationRepository.findById(authorizationId);
    }

    @Override
    public void deleteById(Long authorizationId) {
        authorizationRepository.deleteById(authorizationId);
    }

    @Override
    public List<Authorization> findByRoleId(Long roleId) {
        return authorizationRepository.findByRoleId(roleId);
    }
}

