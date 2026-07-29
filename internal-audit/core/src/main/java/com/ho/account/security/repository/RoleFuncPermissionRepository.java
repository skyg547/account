package com.ho.account.security.repository;

import com.ho.account.security.domain.RoleFuncPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleFuncPermissionRepository
        extends JpaRepository<RoleFuncPermission, RoleFuncPermission.RoleFuncPermissionId> {
    List<RoleFuncPermission> findByRoleRoleCode(String roleCode);
}
