package com.ho.account.audit.infrastructure.persistence;

import com.ho.account.audit.application.port.out.SystemRolePersistencePort;
import com.ho.account.audit.domain.SystemRole;
import com.ho.account.audit.repository.SystemRoleRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemRoleJpaAdapter implements SystemRolePersistencePort {

    private final SystemRoleRepository systemRoleRepository;

    @Override
    public boolean existsByRoleCode(String roleCode) {
        return systemRoleRepository.existsByRoleCode(roleCode);
    }

    @Override
    public SystemRole save(SystemRole role) {
        return systemRoleRepository.save(role);
    }

    @Override
    public List<SystemRole> findAll() {
        return systemRoleRepository.findAll();
    }

    @Override
    public Optional<SystemRole> findByRoleCode(String roleCode) {
        return systemRoleRepository.findByRoleCode(roleCode);
    }
}

