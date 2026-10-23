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

    /** 업무 키 잠금 후, 기존 영속성 캐시 대신 최신 현재 버전을 잠가 읽습니다. */
    Optional<Department> findActiveByCodeForUpdate(String code);

    Optional<Department> findActiveByCodeAt(String code, java.time.LocalDate asOfDate);

    List<Department> findAll();


    List<Department> findAllActive();

    Department save(Department department);
}
