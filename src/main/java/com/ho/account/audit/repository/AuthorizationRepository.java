package com.ho.account.audit.repository;

import com.ho.account.audit.domain.AccessType;
import com.ho.account.audit.domain.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorizationRepository extends JpaRepository<Authorization, Long> {

    List<Authorization> findByRoleId(Long roleId);

    List<Authorization> findByFunctionCode(String functionCode);

    List<Authorization> findByRoleIdAndFunctionCode(Long roleId, String functionCode);

    boolean existsByRoleIdAndFunctionCodeAndAccessType(Long roleId, String functionCode, AccessType accessType);
}
