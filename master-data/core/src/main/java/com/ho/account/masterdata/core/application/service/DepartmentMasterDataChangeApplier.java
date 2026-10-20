package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataVersionConflictException;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.domain.policy.MasterDataFieldRules;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 승인된 부서 변경 요청을 실제 SCD2 유즈케이스에 연결합니다.
 *
 * <p>DEACTIVATE는 변경할 필드가 없으므로 payload를 요구하지 않고, 승인된 effectiveDate를
 * 그대로 종료일로 사용합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class DepartmentMasterDataChangeApplier implements MasterDataChangeApplier {

    private final DepartmentUseCase departmentUseCase;
    private final MasterDataChangePayloadDecoder payloadDecoder;
    private final DepartmentPersistencePort departmentPersistencePort;

    @Override
    public MasterDataType targetType() {
        return MasterDataType.DEPARTMENT;
    }

    @Override
    public void validate(MasterDataChangeRequest request) {
        if (request.getChangeType() != MasterDataChangeRequest.ChangeType.DEACTIVATE) {
            validatedCommand(request);
        }
    }

    private DepartmentCommand validatedCommand(MasterDataChangeRequest request) {
        DepartmentCommand command = command(request);
        if (command.parentCode() != null && !command.parentCode().isBlank()) {
            MasterDataFieldRules.code(command.parentCode(), "Department parentCode", MasterDataFieldRules.DEPARTMENT_CODE_MAX);
        }
        MasterDataValidityPolicy.requireValidityWindow(command.validFrom(),
                command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));
        Department candidate;
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.CREATE) {
            candidate = command.toEntity();
        } else {
            Department current = departmentUseCase.findDepartmentByCode(request.getTargetKey())
                    .orElseThrow(() -> new MasterDataVersionConflictException("Active department no longer exists."));
            candidate = new Department();
            candidate.setCode(current.getCode());
            candidate.setName(command.name() != null ? command.name() : current.getName());
        }
        candidate.validateBusinessFields();
        return command;
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        validate(request);
        switch (request.getChangeType()) {
            case CREATE -> departmentUseCase.createDepartment(validatedCommand(request));
            case UPDATE -> {
                DepartmentCommand command = validatedCommand(request);
                Department current = current(request);
                MasterDataChangeApplierSupport.requireCurrentUpdateWindow(
                        current.getValidTo(), command.validFrom(), command.validTo());
                departmentUseCase.updateDepartment(request.getTargetKey(), command);
            }
            case DEACTIVATE -> {
                current(request);
                departmentUseCase.deactivateDepartment(request.getTargetKey(), request.getEffectiveDate());
            }
        }
    }

    private Department current(MasterDataChangeRequest request) {
        // 종료만 된 이력은 개수가 그대로이므로 현재 버전 부재도 승인 상태 충돌입니다.
        return departmentPersistencePort.findActiveByCodeForUpdate(request.getTargetKey())
                .orElseThrow(() -> new MasterDataVersionConflictException(
                        "Active department no longer exists. Code: " + request.getTargetKey()));
    }

    private DepartmentCommand command(MasterDataChangeRequest request) {
        DepartmentCommand payload = MasterDataChangeApplierSupport.decode(
                request, payloadDecoder, DepartmentCommand.class);
        if (payload == null) {
            throw new IllegalArgumentException("Department change payload must not be null.");
        }
        String code = MasterDataChangeApplierSupport.targetKey(request, payload.code());
        return new DepartmentCommand(
                code,
                payload.name(),
                payload.parentCode(),
                payload.type(),
                request.getEffectiveDate(),
                payload.validTo());
    }
}
