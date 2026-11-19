package com.ho.account.auth.core.application.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand;
import com.ho.account.auth.core.domain.model.GovernanceRoleChange;
import com.ho.account.auth.core.domain.model.GovernanceChangePolicy;
import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase.CreateRoleCommand;
import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase.GrantAuthorizationCommand;
import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static com.ho.account.auth.core.domain.model.GovernanceChangePolicy.*;

/** Maps JSON to validated domain changes and application commands; domain policy owns their constraints. */
final class GovernancePayloads {
    private final ObjectMapper json;

    GovernancePayloads(ObjectMapper mapper) {
        json = mapper.copy().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    Object validate(MasterApproval approval) {
        supportedChange(approval.getMasterType(), approval.getRequestType() == null ? null : approval.getRequestType().name());
        return switch (approval.getMasterType()) {
            case "AUTH_USER_ROLE" -> userRoles(approval);
            case "SYSTEM_ROLE" -> {
                CreateRoleCommand c = read(approval.getPayload(), CreateRoleCommand.class);
                GovernanceChangePolicy.roleDefinition(approval.getMasterKey(), c.roleCode(), c.roleName(), c.description());
                yield c;
            }
            case "AUTHORIZATION" -> {
                if (approval.getRequestType() == MasterApproval.ChangeRequestType.DELETE) {
                    Long id = read(approval.getPayload(), Long.class);
                    require(id != null && id > 0);
                    yield id;
                }
                GrantAuthorizationCommand c = read(approval.getPayload(), GrantAuthorizationCommand.class);
                GovernanceChangePolicy.authorization(approval.getMasterKey(), c.roleCode(), c.functionCode(),
                        c.accessType() == null ? null : c.accessType().name());
                yield new GrantAuthorizationCommand(c.roleCode(), c.functionCode(), c.accessType(), scope(c.dataScope()));
            }
            default -> throw new IllegalArgumentException("Unsupported governance master type");
        };
    }

    private ReplaceRoleAssignmentsCommand userRoles(MasterApproval approval) {
        JsonNode node = read(approval.getPayload(), JsonNode.class);
        require(node != null && node.isObject());
        String username = node.has("username") ? string(node.get("username")) : approval.getMasterKey();
        // masterKey is the sole target identity. Legacy numeric userId is display metadata only.
        List<String> roles = new ArrayList<>();
        if (node.has("roles")) {
            require(node.get("roles").isArray());
            node.get("roles").forEach(role -> roles.add(string(role)));
        } else {
            JsonNode role = node.has("roleCode") ? node.get("roleCode") : node.get("role");
            roles.add(string(role));
        }
        String dataScope = node.has("dataScope") ? string(node.get("dataScope")) : null;
        Instant validTo = node.hasNonNull("validTo") ? instant(string(node.get("validTo"))) : null;
        GovernanceRoleChange change = GovernanceRoleChange.forTarget(approval.getMasterKey(), username, roles,
                dataScope, approval.getEffectiveDate(), validTo);
        return new ReplaceRoleAssignmentsCommand(change.username(), change.roleCodes(), change.dataScope(),
                change.validFrom(), change.validTo(), approval.getApproverUser(), "governance-approval-id=" + approval.getId());
    }

    private String string(JsonNode value) {
        require(value != null && value.isTextual());
        return value.textValue();
    }

    private Instant instant(String value) {
        try { return Instant.parse(value); }
        catch (java.time.DateTimeException exception) { throw new IllegalArgumentException("Invalid validity date"); }
    }

    <T> T read(String value, Class<T> type) {
        text(value, 16000);
        try {
            T parsed = json.readValue(value, type);
            require(parsed != null);
            return parsed;
        }
        catch (Exception e) { throw new IllegalArgumentException("Invalid governance payload"); }
    }

    String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid governance payload"); }
    }
}
