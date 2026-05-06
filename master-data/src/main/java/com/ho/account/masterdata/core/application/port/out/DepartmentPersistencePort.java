package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.Department;
import java.util.List;
import java.util.Optional;

/**
 * 부서 정보 영속성 포트
 */
public interface DepartmentPersistencePort {

    boolean existsByCode(String code);

    Optional<Department> findById(Long id);

    Optional<Department> findActiveByCode(String code);

    List<Department> findAll();

    List<Department> findAllActive();

    Department save(Department department);
}
