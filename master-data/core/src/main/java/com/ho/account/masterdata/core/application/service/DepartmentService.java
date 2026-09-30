package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
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
    private final MasterDataBusinessKeyLockPort businessKeyLockPort;

    @Override
    @Transactional
    public Department createDepartment(DepartmentCommand command) {
        businessKeyLockPort.lock(MasterDataType.DEPARTMENT, command.code());
        if (departmentPersistencePort.existsByCode(command.code())) {
            throw new MasterDataVersionConflictException("이미 이력이 존재하는 부서 코드입니다: " + command.code());
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
        businessKeyLockPort.lock(MasterDataType.DEPARTMENT, code);
        Department currentActive = departmentPersistencePort.findActiveByCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 부서를 찾을 수 없습니다: " + code));
        if (!MasterDataValidityPolicy.isActiveAt(LocalDate.now(), currentActive.getValidFrom(), currentActive.getValidTo())) {
            throw new IllegalArgumentException("활성 부서 버전만 수정할 수 있습니다: " + code);
        }
        if (command.code() != null && !command.code().equals(code)) {
            throw new IllegalArgumentException("부서 코드는 SCD2 버전 수정에서 변경할 수 없습니다: " + code);
        }

        // SCD2: 기존 활성 버전 종료 (새로운 버전 시작일의 전날로 종료)
        LocalDate newValidFrom = command.validFrom() != null ? command.validFrom() : LocalDate.now();
        LocalDate newValidTo = command.validTo() != null
                ? command.validTo()
                : LocalDate.of(9999, 12, 31);
        MasterDataValidityPolicy.requireVersionSplit(
                currentActive.getValidFrom(), currentActive.getValidTo(), newValidFrom, newValidTo);
        LocalDate oldValidTo = newValidFrom.minusDays(1);
        
        // 참조 검증과 신규 버전 조립을 먼저 끝내야 잘못된 parentCode가 현재 버전을 건드리지 않습니다.
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
        newVersion.setValidTo(newValidTo);

        currentActive.terminate(oldValidTo);
        departmentPersistencePort.save(currentActive);

        return departmentPersistencePort.save(newVersion);
    }

    @Override
    @Transactional
    public void deactivateDepartment(String code) {
        deactivateDepartment(code, LocalDate.now());
    }

    @Override
    @Transactional
    public void deactivateDepartment(String code, LocalDate effectiveDate) {
        businessKeyLockPort.lock(MasterDataType.DEPARTMENT, code);
        Department department = departmentPersistencePort.findActiveByCodeForUpdate(code)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 부서를 찾을 수 없습니다: " + code));

        // SCD2: API 직접 호출은 오늘, 승인 요청 반영은 승인된 effectiveDate를 종료일로 사용합니다.
        LocalDate terminationDate = MasterDataValidityPolicy.requireNewTerminationDate(
                effectiveDate, department.getValidFrom(), department.getValidTo());
        department.terminate(terminationDate);
        departmentPersistencePort.save(department);
    }
}
