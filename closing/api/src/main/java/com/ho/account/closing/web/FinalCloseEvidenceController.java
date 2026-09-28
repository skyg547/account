package com.ho.account.closing.web;

import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase;
import com.ho.account.closing.application.service.FinalCloseEvidenceProperties;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import com.ho.account.closing.dto.FinalCloseEvidenceRequest;
import com.ho.account.closing.dto.FinalCloseEvidenceResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Trusted inbound adapter for immutable final-close evidence supplied by internal providers.
 *
 * <p>The Gateway must strip client-provided {@code X-Auth-*} headers and rebuild them from the
 * verified identity. The Closing API port must therefore remain private and must not be exposed
 * directly to public clients.</p>
 */
@Validated
@RestController
@RequestMapping("/api/closing/calendars/{calendarId}/final-close-evidence")
public class FinalCloseEvidenceController {

    static final String AUTH_USER_HEADER = "X-Auth-User";
    static final String AUTH_ROLES_HEADER = "X-Auth-Roles";

    private static final String EVIDENCE_PROVIDER_ROLE = "ROLE_CLOSING_EVIDENCE_PROVIDER";

    private final FinalCloseEvidenceUseCase useCase;
    private final FinalCloseEvidenceProperties properties;

    public FinalCloseEvidenceController(
            FinalCloseEvidenceUseCase useCase,
            FinalCloseEvidenceProperties properties) {
        this.useCase = useCase;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<FinalCloseEvidenceResponse> record(
            @PathVariable @Positive Long calendarId,
            @Valid @RequestBody FinalCloseEvidenceRequest request,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        requireEvidenceProvider(actor, roles);
        FinalCloseEvidenceSet recorded = useCase.record(calendarId, request.toSubmission(), actor.trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(FinalCloseEvidenceResponse.from(recorded));
    }

    private void requireEvidenceProvider(String actor, String roles) {
        if (actor == null || actor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated actor is required.");
        }
        boolean authorized = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .anyMatch(EVIDENCE_PROVIDER_ROLE::equals);
        if (!authorized) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The closing evidence provider role is required.");
        }
        if (!properties.isTrustedSubmitter(actor)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A configured closing evidence provider identity is required.");
        }
    }
}
