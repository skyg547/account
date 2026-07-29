package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.domain.model.Department;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DepartmentUseCase {

    Department createDepartment(DepartmentCommand command);

    Optional<Department> getDepartmentByCode(String code);

    Optional<Department> findDepartmentByCode(String code);

    List<Department> getAllDepartments();

    List<Department> findAllActiveDepartments();

    // Alias for findAllActiveDepartments to match controller
    default List<Department> getActiveDepartments() {
        return findAllActiveDepartments();
    }

    Department updateDepartment(String code, DepartmentCommand command);

    void deactivateDepartment(String code);

    /**
     * 승인 워크플로에서 정한 종료일로 현재 SCD2 버전을 비활성화합니다.
     */
    void deactivateDepartment(String code, LocalDate effectiveDate);
}