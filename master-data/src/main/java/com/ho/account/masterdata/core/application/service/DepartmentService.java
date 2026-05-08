package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        if (departmentPersistencePort.findActiveByCode(command.code()).isPresent()) {
            throw new IllegalArgumentException("이미 해당 시점에 활성화된 부서 코드입니다: " + command.code());
        }

        Department department = new Department();
        department.setCode(command.code());
        department.setName(command.name());
        
        if (command.parentCode() != null) {
            Department parent = departmentPersistencePort.findActiveByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다: " + command.parentCode()));
            department.setParent(parent);
        }
        
        department.setValidFrom(command.validFrom());
        department.setValidTo(command.validTo());

        return departmentPersistencePort.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> getDepartmentByCode(String code) {
        return departmentPersistencePort.findActiveByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findDepartmentByCode(String code) {
        return departmentPersistencePort.findActiveByCode(code);
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
        Department currentActive = departmentPersistencePort.findActiveByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 부서를 찾을 수 없습니다: " + code));

        // SCD2: 기존 활성 버전 종료 (새로운 버전 시작일의 전날로 종료)
        LocalDate newValidFrom = command.validFrom();
        LocalDate oldValidTo = newValidFrom.minusDays(1);
        
        if (oldValidTo.isBefore(currentActive.getValidFrom())) {
            throw new IllegalArgumentException("새로운 유효 시작일이 기존 시작일보다 빠를 수 없습니다.");
        }
        
        currentActive.terminate(oldValidTo);
        departmentPersistencePort.save(currentActive);

        // SCD2: 새로운 버전 생성
        Department newVersion = new Department();
        newVersion.setCode(code);
        newVersion.setName(command.name());
        
        if (command.parentCode() != null) {
            Department parent = departmentPersistencePort.findActiveByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다: " + command.parentCode()));
            newVersion.setParent(parent);
        }
        
        newVersion.setValidFrom(newValidFrom);
        newVersion.setValidTo(command.validTo());

        return departmentPersistencePort.save(newVersion);
    }

    @Override
    @Transactional
    public void deactivateDepartment(String code) {
        Department department = departmentPersistencePort.findActiveByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 부서를 찾을 수 없습니다: " + code));
        
        // SCD2: 현재 활성 버전을 오늘 날짜로 종료
        department.terminate(java.time.LocalDate.now());
        departmentPersistencePort.save(department);
    }
}
