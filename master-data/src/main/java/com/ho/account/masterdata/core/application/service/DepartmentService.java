package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.Department;
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
            throw new IllegalArgumentException("?대? 議댁옱?섎뒗 遺??肄붾뱶?낅땲?? " + command.code());
        }

        Department department = command.toEntity();

        if (command.hasParentCode()) {
            Department parent = departmentPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(
                            () -> new IllegalArgumentException("?곸쐞 遺?쒕? 李얠쓣 ???놁뒿?덈떎. 肄붾뱶: " + command.parentCode()));
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
                .orElseThrow(() -> new IllegalArgumentException("遺?쒕? 李얠쓣 ???놁뒿?덈떎. 肄붾뱶: " + code));

        if (command.hasParentCode()) {
            Department parent = departmentPersistencePort.findByCode(command.parentCode())
                    .orElseThrow(
                            () -> new IllegalArgumentException("?곸쐞 遺?쒕? 李얠쓣 ???놁뒿?덈떎. 肄붾뱶: " + command.parentCode()));
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
                .orElseThrow(() -> new IllegalArgumentException("遺?쒕? 李얠쓣 ???놁뒿?덈떎. 肄붾뱶: " + code));

        MasterDataValidityPolicy.closeIfActive(department::getValidTo, department::setValidTo);
        departmentPersistencePort.save(department);
    }
}
