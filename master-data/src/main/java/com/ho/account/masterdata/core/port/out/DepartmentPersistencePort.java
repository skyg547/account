package com.ho.account.masterdata.core.port.out;

import com.ho.account.basic.domain.Department;
import java.util.List;
import java.util.Optional;

/**
 * 부서 저장소 출력 포트입니다.
 *
 * <p>부서 마스터는 비용센터/손익센터 계층을 표현합니다. 유스케이스는 이 포트를 통해
 * 상위 부서 연결과 유효기간 종료를 처리하고, JPA 세부사항에는 의존하지 않습니다.</p>
 */
public interface DepartmentPersistencePort {

    boolean existsByCode(String code);

    Optional<Department> findByCode(String code);

    List<Department> findAll();

    Department save(Department department);
}
