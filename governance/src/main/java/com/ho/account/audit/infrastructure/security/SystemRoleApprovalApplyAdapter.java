package com.ho.account.audit.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.audit.application.port.in.AuthorizationUseCase.CreateRoleCommand;
import com.ho.account.audit.application.port.in.AuthorizationUseCase.GrantAuthorizationCommand;
import com.ho.account.audit.application.port.out.AuthorizationPersistencePort;
import com.ho.account.audit.application.port.out.MasterDataChangeApplyPort;
import com.ho.account.audit.application.port.out.SystemRolePersistencePort;
import com.ho.account.audit.domain.Authorization;
import com.ho.account.audit.domain.AuthorizationPolicy;
import com.ho.account.audit.domain.MasterApproval;
import com.ho.account.audit.domain.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemRoleApprovalApplyAdapter implements MasterDataChangeApplyPort {

    private final SystemRolePersistencePort systemRolePersistencePort;
    private final AuthorizationPersistencePort authorizationPersistencePort;
    private final ObjectMapper objectMapper;
    private final AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();

    @Override
    public boolean supports(String masterType) {
        return "SYSTEM_ROLE".equalsIgnoreCase(masterType) || "AUTHORIZATION".equalsIgnoreCase(masterType);
    }

    @Override
    public void applyApprovedChange(MasterApproval approval) {
        try {
            if ("SYSTEM_ROLE".equalsIgnoreCase(approval.getMasterType())) {
                if (approval.getRequestType() == MasterApproval.ChangeRequestType.CREATE) {
                    CreateRoleCommand command = objectMapper.readValue(approval.getPayload(), CreateRoleCommand.class);
                    SystemRole role = SystemRole.create(
                            command.roleCode(),
                            command.roleName(),
                            command.description(),
                            approval.getApproverUser());
                    systemRolePersistencePort.save(role);
                }
            } else if ("AUTHORIZATION".equalsIgnoreCase(approval.getMasterType())) {
                if (approval.getRequestType() == MasterApproval.ChangeRequestType.CREATE) {
                    GrantAuthorizationCommand command = objectMapper.readValue(approval.getPayload(), GrantAuthorizationCommand.class);
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
                    Authorization auth = Authorization.grant(
                            role,
                            command.functionCode(),
                            command.accessType(),
                            authorizationPolicy.normalizeDataScope(command.dataScope()),
                            approval.getApproverUser());
                    authorizationPersistencePort.save(auth);
                } else if (approval.getRequestType() == MasterApproval.ChangeRequestType.DELETE) {
                    Long authId = Long.parseLong(approval.getPayload());
                    authorizationPersistencePort.deleteById(authId);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply governance approval", e);
        }
    }
}
