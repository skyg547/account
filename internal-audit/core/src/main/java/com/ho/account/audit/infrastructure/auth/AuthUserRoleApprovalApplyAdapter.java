package com.ho.account.audit.infrastructure.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.audit.application.model.AuthUserRoleAssignmentChange;
import com.ho.account.audit.application.port.out.AuthUserRoleAssignmentApplyPort;
import com.ho.account.audit.application.port.out.MasterDataChangeApplyPort;
import com.ho.account.audit.domain.MasterApproval;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserRoleApprovalApplyAdapter implements MasterDataChangeApplyPort {

    static final String MASTER_TYPE = "AUTH_USER_ROLE";

    private final AuthUserRoleAssignmentApplyPort authUserRoleAssignmentApplyPort;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String masterType) {
        return MASTER_TYPE.equalsIgnoreCase(masterType);
    }

    @Override
    public void applyApprovedChange(MasterApproval approval) {
        if (approval.getRequestType() != MasterApproval.ChangeRequestType.UPDATE
                && approval.getRequestType() != MasterApproval.ChangeRequestType.CREATE) {
            throw new IllegalArgumentException("Unsupported auth user role request type: " + approval.getRequestType());
        }

        AuthUserRolePayload payload = parsePayload(approval.getPayload());
        String username = resolveUsername(payload, approval);
        List<String> roleCodes = resolveRoleCodes(payload);
        if (roleCodes.isEmpty()) {
            throw new IllegalArgumentException("AUTH_USER_ROLE approval requires role or roles payload.");
        }

        authUserRoleAssignmentApplyPort.replaceUserRoles(new AuthUserRoleAssignmentChange(
                username,
                roleCodes,
                payload.dataScope(),
                resolveValidFrom(approval.getEffectiveDate()),
                payload.validTo(),
                approval.getApproverUser(),
                "governance-approval-id=" + approval.getId()));
    }

    private AuthUserRolePayload parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, AuthUserRolePayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid AUTH_USER_ROLE payload.", e);
        }
    }

    private String resolveUsername(AuthUserRolePayload payload, MasterApproval approval) {
        return Stream.of(payload.username(), payload.userId(), approval.getMasterKey())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("AUTH_USER_ROLE approval requires username."));
    }

    private List<String> resolveRoleCodes(AuthUserRolePayload payload) {
        List<String> roles = payload.roles();
        if (roles != null && !roles.isEmpty()) {
            return roles.stream()
                    .map(this::normalizeRoleCode)
                    .filter(role -> !role.isBlank())
                    .distinct()
                    .toList();
        }
        return Stream.of(payload.roleCode(), payload.role())
                .filter(Objects::nonNull)
                .map(this::normalizeRoleCode)
                .filter(role -> !role.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "";
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private Instant resolveValidFrom(LocalDate effectiveDate) {
        if (effectiveDate == null) {
            return null;
        }
        return effectiveDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthUserRolePayload(
            String username,
            String userId,
            String role,
            String roleCode,
            List<String> roles,
            String dataScope,
            Instant validTo) {
    }
}
