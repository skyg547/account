package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
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

        Department department = command.toEntity();
        
        if (command.hasParentCode()) {
            Department parent = departmentPersistencePort.findActiveByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다: " + command.parentCode()));
            department.setParent(parent);
        }

        MasterDataValidityPolicy.applyDefaultWindow(department::getValidFrom, department::setValidFrom,
                department::getValidTo, department::setValidTo);

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
        if (!MasterDataValidityPolicy.isActiveAt(LocalDate.now(), currentActive.getValidFrom(), currentActive.getValidTo())) {
            throw new IllegalArgumentException("활성 부서 버전만 수정할 수 있습니다: " + code);
        }
        if (command.code() != null && !command.code().equals(code)) {
            throw new IllegalArgumentException("부서 코드는 SCD2 버전 수정에서 변경할 수 없습니다: " + code);
        }

        // SCD2: 기존 활성 버전 종료 (새로운 버전 시작일의 전날로 종료)
        LocalDate newValidFrom = command.validFrom() != null ? command.validFrom() : LocalDate.now();
        LocalDate oldValidTo = newValidFrom.minusDays(1);
        
        if (oldValidTo.isBefore(currentActive.getValidFrom())) {
            throw new IllegalArgumentException("새로운 유효 시작일이 기존 시작일보다 빠를 수 없습니다.");
        }
        
        currentActive.terminate(oldValidTo);
        departmentPersistencePort.save(currentActive);

        // SCD2: 새로운 버전 생성
        Department newVersion = new Department();
        newVersion.setCode(code);
        newVersion.setName(command.name() != null ? command.name() : currentActive.getName());
        newVersion.setType(command.type() != null ? command.type() : currentActive.getType());
        
        if (command.hasParentCode()) {
            Department parent = departmentPersistencePort.findActiveByCode(command.parentCode())
                    .orElseThrow(() -> new IllegalArgumentException("상위 부서를 찾을 수 없습니다: " + command.parentCode()));
            newVersion.setParent(parent);
        } else {
            newVersion.setParent(currentActive.getParent());
        }
        
        newVersion.setValidFrom(newValidFrom);
        newVersion.setValidTo(command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));

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
