package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.application.port.in.DepartmentUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangePayloadDecoder;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
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

    @Override
    public MasterDataType targetType() {
        return MasterDataType.DEPARTMENT;
    }

    @Override
    public void validate(MasterDataChangeRequest request) {
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.DEACTIVATE) {
            return;
        }
        DepartmentCommand command = command(request);
        // UPDATE의 null 필드는 서비스가 기존 값으로 채웁니다. 검증을 위해 기존 부서를 조회하지 않습니다.
        if (request.getChangeType() == MasterDataChangeRequest.ChangeType.CREATE && command.name() == null) {
            throw new IllegalArgumentException("Department name is required.");
        }
        MasterDataValidityPolicy.requireValidityWindow(command.validFrom(),
                command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        switch (request.getChangeType()) {
            case CREATE -> departmentUseCase.createDepartment(command(request));
            case UPDATE -> departmentUseCase.updateDepartment(request.getTargetKey(), command(request));
            case DEACTIVATE -> departmentUseCase.deactivateDepartment(
                    request.getTargetKey(), request.getEffectiveDate());
        }
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
