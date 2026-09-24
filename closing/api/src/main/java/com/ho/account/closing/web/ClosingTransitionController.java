package com.ho.account.closing.web;

import com.ho.account.closing.application.port.in.ClosingTransitionRecoveryUseCase;
import com.ho.account.closing.dto.ClosingTransitionDto;
import com.ho.account.closing.dto.ClosingTransitionRecoveryRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Privileged recovery uses Gateway-verified identity; direct public access is unsupported. */
@RestController
@RequestMapping("/api/closing/calendars/{calendarId}/transition")
@RequiredArgsConstructor
public class ClosingTransitionController {
    private static final Set<String> RECOVERY_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_ACCOUNTING_ADMIN", "ROLE_CLOSING_MANAGER");
    private final ClosingTransitionRecoveryUseCase recovery;

    @GetMapping
    public ClosingTransitionDto find(@PathVariable Long calendarId,
            @RequestHeader(name = "X-Auth-User", required = false) String actor,
            @RequestHeader(name = "X-Auth-Roles", required = false) String roles) {
        requireAuthority(actor, roles);
        return ClosingTransitionDto.from(recovery.findTransition(calendarId));
    }

    @PostMapping("/recover")
    public ClosingTransitionDto recover(@PathVariable Long calendarId,
            @Valid @RequestBody ClosingTransitionRecoveryRequest request,
            @RequestHeader(name = "X-Auth-User", required = false) String actor,
            @RequestHeader(name = "X-Auth-Roles", required = false) String roles) {
        requireAuthority(actor, roles);
        return ClosingTransitionDto.from(recovery.recoverTransition(calendarId, request.operationId(),
                request.remoteRequestTerminated(), actor.trim()));
    }

    private void requireAuthority(String actor, String roles) {
        if (actor == null || actor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated actor is required.");
        }
        boolean authorized = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim).map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .anyMatch(RECOVERY_ROLES::contains);
        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A closing recovery role is required.");
        }
    }
}
