package com.ho.account.masterdata.core.application.usecase;

import com.ho.account.basic.domain.Department;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import java.util.List;
import java.util.Optional;

public interface DepartmentUseCase {

    Department createDepartment(DepartmentCommand command);

    Optional<Department> getDepartmentByCode(String code);

    Optional<Department> findDepartmentByCode(String code);

    List<Department> getAllDepartments();

    List<Department> findAllActiveDepartments();

    Department updateDepartment(String code, DepartmentCommand command);

    void deactivateDepartment(String code);
}
