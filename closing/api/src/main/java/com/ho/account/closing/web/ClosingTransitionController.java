package com.ho.account.closing.web;

import com.ho.account.closing.application.port.in.ClosingTransitionRecoveryUseCase;
import com.ho.account.closing.dto.ClosingTransitionDto;
import com.ho.account.closing.dto.ClosingPreparedTransitionCancellationRequest;
import com.ho.account.closing.dto.ClosingTransitionRecoveryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Privileged recovery uses Gateway-verified identity; direct public access is unsupported. */
@RestController
@RequestMapping("/api/closing/calendars/{calendarId}/transition")
@RequiredArgsConstructor
public class ClosingTransitionController {
    private final ClosingTransitionRecoveryUseCase recovery;

    @GetMapping
    public ClosingTransitionDto find(@PathVariable Long calendarId,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        return ClosingTransitionDto.from(recovery.findTransition(calendarId));
    }

    @PostMapping("/recover")
    public ClosingTransitionDto recover(@PathVariable Long calendarId,
            @Valid @RequestBody ClosingTransitionRecoveryRequest request,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        return ClosingTransitionDto.from(recovery.recoverTransition(calendarId, request.operationId(),
                request.remoteRequestTerminated(), trustedActor));
    }

    @PostMapping("/cancel-prepared")
    public ClosingTransitionDto cancelPrepared(@PathVariable Long calendarId,
            @Valid @RequestBody ClosingPreparedTransitionCancellationRequest request,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = ClosingCommandAuthority.AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(actor, roles);
        return ClosingTransitionDto.from(recovery.cancelPreparedClose(
                calendarId, request.operationId(), request.reason(), trustedActor));
    }
}
