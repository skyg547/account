package com.ho.account.masterdata.core.application.service;

import com.ho.account.basic.domain.Department;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.usecase.DepartmentUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.port.out.DepartmentPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Service
@Transactional
public class DepartmentService implements DepartmentUseCase {

    private final DepartmentPersistencePort departmentPersistencePort;

    public DepartmentService(DepartmentPersistencePort departmentPersistencePort) {
        this.departmentPersistencePort = departmentPersistencePort;
    }

    public Department createDepartment(DepartmentCommand command) {
        if (departmentPersistencePort.existsByCode(command.code())) {
            throw new IllegalArgumentException("이미 존재하는 부서 코드입니다: " + command.code());
        }

        Department department = command.toEntity();

        if (command.hasParentCode()) {
            Department parent = departmentPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(
                            () -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다. 코드: " + command.parentCode()));
            department.setParent(parent);
        }

        MasterDataValidityPolicy.applyDefaultWindow(department::getValidFrom, department::setValidFrom,
                department::getValidTo, department::setValidTo);
        return departmentPersistencePort.save(department);
    }

    @Transactional(readOnly = true)
    public Optional<Department> getDepartmentByCode(String code) {
        return departmentPersistencePort.findByCode(code);
    }

    @Transactional(readOnly = true)
    public Optional<Department> findDepartmentByCode(String code) {
        return departmentPersistencePort.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentPersistencePort.findAll();
    }

    @Transactional(readOnly = true)
    public List<Department> findAllActiveDepartments() {
        return departmentPersistencePort.findAll().stream()
                .filter(MasterDataValidityPolicy.isActiveNow(Department::getValidFrom, Department::getValidTo))
                .toList();
    }

    public Department updateDepartment(String code, DepartmentCommand command) {
        Department department = departmentPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. 코드: " + code));

        if (command.hasParentCode()) {
            Department parent = departmentPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(
                            () -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다. 코드: " + command.parentCode()));
            department.setParent(parent);
        } else {
            department.setParent(null);
        }

        department.setName(command.name());
        department.setType(command.type());

        return departmentPersistencePort.save(department);
    }

    public void deactivateDepartment(String code) {
        Department department = departmentPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. 코드: " + code));

        MasterDataValidityPolicy.closeIfActive(department::getValidTo, department::setValidTo);
        departmentPersistencePort.save(department);
    }
}
