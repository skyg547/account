package com.ho.account.closing.web;

import com.ho.account.closing.application.port.in.EodLifecycleUseCase;
import com.ho.account.closing.dto.EodStatusDto;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * EOD/BOD 유스케이스를 HTTP 명령으로 변환하는 인바운드 어댑터입니다.
 *
 * <p>임의의 목표 상태를 받지 않고 업무 동작별 경로만 제공합니다. Gateway는 외부에서
 * 들어온 {@code X-Auth-*} 헤더를 제거한 뒤 검증된 JWT의 값으로 다시 만들기 때문에,
 * Closing API 포트는 외부에 직접 공개하지 않아야 합니다.</p>
 */
@RestController
@RequestMapping("/api/closing/eod")
public class EodLifecycleController {

    static final String AUTH_USER_HEADER = "X-Auth-User";
    static final String AUTH_ROLES_HEADER = "X-Auth-Roles";

    private static final Set<String> COMMAND_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_ACCOUNTING_ADMIN",
            "ROLE_CLOSING_MANAGER");

    private final EodLifecycleUseCase eodLifecycleUseCase;

    public EodLifecycleController(EodLifecycleUseCase eodLifecycleUseCase) {
        this.eodLifecycleUseCase = eodLifecycleUseCase;
    }

    @GetMapping("/{businessDate}")
    public EodStatusDto findStatus(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor) {
        requireAuthenticatedActor(actor);
        return EodStatusDto.from(eodLifecycleUseCase.findStatus(businessDate));
    }

    @PostMapping("/{businessDate}/bootstrap")
    public EodStatusDto bootstrap(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        requireCommandAuthority(actor, roles);
        return EodStatusDto.from(eodLifecycleUseCase.bootstrap(businessDate, actor.trim()));
    }

    @PostMapping("/{businessDate}/eod/prepare")
    public EodStatusDto prepareEod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        requireCommandAuthority(actor, roles);
        return EodStatusDto.from(eodLifecycleUseCase.prepareEod(businessDate, actor.trim()));
    }

    @PostMapping("/{businessDate}/eod/cancel-preparation")
    public EodStatusDto cancelEodPreparation(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        requireCommandAuthority(actor, roles);
        return EodStatusDto.from(eodLifecycleUseCase.cancelEodPreparation(businessDate, actor.trim()));
    }

    @PostMapping("/{businessDate}/eod/start")
    public EodStatusDto startEod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        requireCommandAuthority(actor, roles);
        return EodStatusDto.from(eodLifecycleUseCase.startEod(businessDate, actor.trim()));
    }

    @PostMapping("/{businessDate}/eod/complete")
    public EodStatusDto completeEod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        requireCommandAuthority(actor, roles);
        return EodStatusDto.from(eodLifecycleUseCase.completeEod(businessDate, actor.trim()));
    }

    @PostMapping("/{closedDate}/bod/{nextBusinessDate}/start")
    public EodStatusDto startBod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closedDate,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextBusinessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        requireCommandAuthority(actor, roles);
        return EodStatusDto.from(
                eodLifecycleUseCase.startBod(closedDate, nextBusinessDate, actor.trim()));
    }

    @PostMapping("/{businessDate}/bod/complete")
    public EodStatusDto completeBod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        requireCommandAuthority(actor, roles);
        return EodStatusDto.from(eodLifecycleUseCase.completeBod(businessDate, actor.trim()));
    }

    private void requireAuthenticatedActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated actor is required.");
        }
    }

    private void requireCommandAuthority(String actor, String roles) {
        requireAuthenticatedActor(actor);
        boolean authorized = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .anyMatch(COMMAND_ROLES::contains);
        if (!authorized) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A closing command role is required.");
        }
    }
}
