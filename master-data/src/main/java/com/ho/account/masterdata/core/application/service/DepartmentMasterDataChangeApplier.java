package com.ho.account.masterdata.core.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.masterdata.core.application.command.DepartmentCommand;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentMasterDataChangeApplier implements MasterDataChangeApplier {

    private final DepartmentService departmentService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MasterDataType targetType) {
        return targetType == MasterDataType.DEPARTMENT;
    }

    @Override
    public void apply(MasterDataChangeRequest request) {
        DepartmentCommand command = parsePayload(request);
        if (request.getChangeType() == ChangeType.CREATE) {
            departmentService.createDepartment(command);
        } else if (request.getChangeType() == ChangeType.UPDATE) {
            departmentService.updateDepartment(request.getTargetKey(), command);
        } else if (request.getChangeType() == ChangeType.DEACTIVATE) {
            departmentService.deactivateDepartment(request.getTargetKey());
        } else {
            throw new IllegalArgumentException("Unsupported department changeType: " + request.getChangeType());
        }
    }

    private DepartmentCommand parsePayload(MasterDataChangeRequest request) {
        try {
            DepartmentCommand parsed = objectMapper.readValue(request.getPayloadJson(), DepartmentCommand.class);
            String code = parsed.code() != null && !parsed.code().isBlank() ? parsed.code() : request.getTargetKey();
            return new DepartmentCommand(
                    code,
                    parsed.name(),
                    parsed.parentCode(),
                    parsed.type(),
                    request.getEffectiveDate(),
                    parsed.validTo());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid DEPARTMENT change payload for request " + request.getId(), e);
        }
    }
}
