package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 부서 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class DepartmentService implements DepartmentUseCase {

    private final DepartmentPersistencePort departmentPersistencePort;

    @Override
    @Transactional
    public Department createDepartment(DepartmentCommand command) {
        if (departmentPersistencePort.existsByCode(command.code())) {
            throw new IllegalArgumentException("이미 존재하는 부서 코드입니다: " + command.code());
        }

        Department department = new Department();
        department.setCode(command.code());
        department.setName(command.name());
        
        if (command.parentCode() != null) {
            Department parent = departmentPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다: " + command.parentCode()));
            department.setParent(parent);
        }
        
        department.setValidFrom(command.validFrom());
        department.setValidTo(command.validTo());
        department.setUseYn(true);

        return departmentPersistencePort.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> getDepartmentByCode(String code) {
        return departmentPersistencePort.findByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findDepartmentByCode(String code) {
        return departmentPersistencePort.findByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentPersistencePort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> findAllActiveDepartments() {
        return departmentPersistencePort.findAllActive();
    }

    @Override
    @Transactional
    public Department updateDepartment(String code, DepartmentCommand command) {
        Department department = departmentPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다: " + code));

        department.setName(command.name());
        if (command.parentCode() != null) {
            Department parent = departmentPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다: " + command.parentCode()));
            department.setParent(parent);
        }
        
        department.setValidFrom(command.validFrom());
        department.setValidTo(command.validTo());

        return departmentPersistencePort.save(department);
    }

    @Override
    @Transactional
    public void deactivateDepartment(String code) {
        Department department = departmentPersistencePort.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다: " + code));
        department.setUseYn(false);
        departmentPersistencePort.save(department);
    }
}
