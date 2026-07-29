package com.ho.account.shared.infrastructure.security.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase.CreateRoleCommand;
import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase.GrantAuthorizationCommand;
import com.ho.account.shared.infrastructure.security.application.port.out.AuthorizationPersistencePort;
import com.ho.account.shared.infrastructure.security.application.port.out.MasterDataChangeApplyPort;
import com.ho.account.shared.infrastructure.security.application.port.out.SystemRolePersistencePort;
import com.ho.account.shared.infrastructure.security.domain.Authorization;
import com.ho.account.shared.infrastructure.security.domain.AuthorizationPolicy;
import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import com.ho.account.shared.infrastructure.security.domain.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 승인된 시스템 역할/권한 변경을 실제 IAM 통제 모델에 반영하는 출력 어댑터입니다.
 *
 * <p>초보자 설명: 이 어댑터가 지원하지 않는 변경 유형을 조용히 무시하면 승인 상태만
 * APPROVED가 되고 실제 권한은 바뀌지 않습니다. 따라서 모든 미지원 조합은 fail-closed로
 * 예외를 발생시킵니다.</p>
 */
@Component
@RequiredArgsConstructor
public class SystemRoleApprovalApplyAdapter implements MasterDataChangeApplyPort {

    static final String SYSTEM_ROLE_MASTER_TYPE = "SYSTEM_ROLE";
    static final String AUTHORIZATION_MASTER_TYPE = "AUTHORIZATION";

    private final SystemRolePersistencePort systemRolePersistencePort;
    private final AuthorizationPersistencePort authorizationPersistencePort;
    private final ObjectMapper objectMapper;
    private final AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();

    @Override
    public boolean supports(String masterType) {
        return SYSTEM_ROLE_MASTER_TYPE.equalsIgnoreCase(masterType)
                || AUTHORIZATION_MASTER_TYPE.equalsIgnoreCase(masterType);
    }

    @Override
    public void applyApprovedChange(MasterApproval approval) {
        if (approval == null) {
            throw new IllegalArgumentException("Approval is required.");
        }
        if (SYSTEM_ROLE_MASTER_TYPE.equalsIgnoreCase(approval.getMasterType())) {
            applySystemRoleChange(approval);
            return;
        }
        if (AUTHORIZATION_MASTER_TYPE.equalsIgnoreCase(approval.getMasterType())) {
            applyAuthorizationChange(approval);
            return;
        }
        throw new IllegalArgumentException(
                "Unsupported governance masterType: " + approval.getMasterType());
    }

    private void applySystemRoleChange(MasterApproval approval) {
        if (approval.getRequestType() != MasterApproval.ChangeRequestType.CREATE) {
            throw unsupportedRequestType(approval);
        }

        CreateRoleCommand command = readPayload(approval, CreateRoleCommand.class);
        SystemRole role = SystemRole.create(
                command.roleCode(),
                command.roleName(),
                command.description(),
                approval.getApproverUser());
        systemRolePersistencePort.save(role);
    }

    private void applyAuthorizationChange(MasterApproval approval) {
        switch (approval.getRequestType()) {
            case CREATE -> grantAuthorization(approval);
            case DELETE -> deleteAuthorization(approval);
            case UPDATE -> throw unsupportedRequestType(approval);
        }
    }

    private void grantAuthorization(MasterApproval approval) {
        GrantAuthorizationCommand command = readPayload(approval, GrantAuthorizationCommand.class);
        SystemRole role = systemRolePersistencePort.findByRoleCode(command.roleCode())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleCode()));
        List<Authorization> existingAuthorizations = authorizationPersistencePort.findByRoleId(role.getId());
        authorizationPolicy.validateGrant(
                role,
                existingAuthorizations,
                command.functionCode(),
                command.accessType(),
                command.dataScope());

        if (authorizationPersistencePort.existsByRoleIdAndFunctionCodeAndAccessType(
                role.getId(), command.functionCode(), command.accessType())) {
            return;
        }

        Authorization authorization = Authorization.grant(
                role,
                command.functionCode(),
                command.accessType(),
                authorizationPolicy.normalizeDataScope(command.dataScope()),
                approval.getApproverUser());
        authorizationPersistencePort.save(authorization);
    }

    private void deleteAuthorization(MasterApproval approval) {
        Long authorizationId = readPayload(approval, Long.class);
        if (authorizationId == null || authorizationId < 1) {
            throw new IllegalArgumentException(
                    "AUTHORIZATION DELETE requires a positive authorization ID.");
        }
        authorizationPersistencePort.deleteById(authorizationId);
    }

    private <T> T readPayload(MasterApproval approval, Class<T> payloadType) {
        String payload = approval.getPayload();
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    approval.getMasterType() + " " + approval.getRequestType() + " payload is required.");
        }
        try {
            return objectMapper.readValue(payload, payloadType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Invalid " + approval.getMasterType() + " " + approval.getRequestType() + " payload.",
                    exception);
        }
    }

    private IllegalArgumentException unsupportedRequestType(MasterApproval approval) {
        return new IllegalArgumentException(
                "Unsupported " + approval.getMasterType() + " requestType: " + approval.getRequestType());
    }
}
