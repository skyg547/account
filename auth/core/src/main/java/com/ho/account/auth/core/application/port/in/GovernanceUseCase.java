package com.ho.account.auth.core.application.port.in;

import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase.CreateRoleCommand;
import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase.GrantAuthorizationCommand;
import com.ho.account.shared.infrastructure.security.application.port.in.MasterApprovalUseCase.RequestApprovalCommand;
import com.ho.account.shared.infrastructure.security.domain.*;
import java.time.LocalDateTime;
import java.util.List;

/** Verified governance operations; every implementation must bind actors from the credential. */
public interface GovernanceUseCase {
    MasterApproval request(String credential, RequestApprovalCommand command);
    MasterApproval createRole(String credential, CreateRoleCommand command);
    MasterApproval grant(String credential, GrantAuthorizationCommand command);
    MasterApproval revoke(String credential, Long id);
    MasterApproval decide(String credential, Long id, boolean approve, String remarks);
    List<MasterApproval> pending(String credential);
    List<SystemRole> roles(String credential);
    SystemRole role(String credential, String code);
    List<Authorization> authorizations(String credential, String code);
    boolean permission(String credential, String roleCode, String function, AccessType access);
    List<AuditLog> logs(String credential, String kind, String key, String target, LocalDateTime start, LocalDateTime end);
}
