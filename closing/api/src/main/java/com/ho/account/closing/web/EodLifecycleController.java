package com.ho.account.closing.web;

import com.ho.account.closing.application.port.in.EodLifecycleUseCase;
import com.ho.account.closing.dto.EodStatusDto;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    static final String AUTH_USER_HEADER = ClosingCommandAuthority.AUTH_USER_HEADER;
    static final String AUTH_ROLES_HEADER = ClosingCommandAuthority.AUTH_ROLES_HEADER;

    // Daily-closing audit storage explicitly supports 80-character actors.
    private static final int EOD_ACTOR_MAX_LENGTH = 80;

    private final EodLifecycleUseCase eodLifecycleUseCase;

    public EodLifecycleController(EodLifecycleUseCase eodLifecycleUseCase) {
        this.eodLifecycleUseCase = eodLifecycleUseCase;
    }

    @GetMapping("/{businessDate}")
    public EodStatusDto findStatus(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor) {
        ClosingCommandAuthority.requireAuthenticatedActor(actor, EOD_ACTOR_MAX_LENGTH);
        return EodStatusDto.from(eodLifecycleUseCase.findStatus(businessDate));
    }

    @PostMapping("/{businessDate}/bootstrap")
    public EodStatusDto bootstrap(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(
                actor, roles, EOD_ACTOR_MAX_LENGTH);
        return EodStatusDto.from(eodLifecycleUseCase.bootstrap(businessDate, trustedActor));
    }

    @PostMapping("/{businessDate}/eod/prepare")
    public EodStatusDto prepareEod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(
                actor, roles, EOD_ACTOR_MAX_LENGTH);
        return EodStatusDto.from(eodLifecycleUseCase.prepareEod(businessDate, trustedActor));
    }

    @PostMapping("/{businessDate}/eod/cancel-preparation")
    public EodStatusDto cancelEodPreparation(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(
                actor, roles, EOD_ACTOR_MAX_LENGTH);
        return EodStatusDto.from(eodLifecycleUseCase.cancelEodPreparation(businessDate, trustedActor));
    }

    @PostMapping("/{businessDate}/eod/start")
    public EodStatusDto startEod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(
                actor, roles, EOD_ACTOR_MAX_LENGTH);
        return EodStatusDto.from(eodLifecycleUseCase.startEod(businessDate, trustedActor));
    }

    @PostMapping("/{businessDate}/eod/complete")
    public EodStatusDto completeEod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(
                actor, roles, EOD_ACTOR_MAX_LENGTH);
        return EodStatusDto.from(eodLifecycleUseCase.completeEod(businessDate, trustedActor));
    }

    @PostMapping("/{closedDate}/bod/{nextBusinessDate}/start")
    public EodStatusDto startBod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closedDate,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextBusinessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(
                actor, roles, EOD_ACTOR_MAX_LENGTH);
        return EodStatusDto.from(
                eodLifecycleUseCase.startBod(closedDate, nextBusinessDate, trustedActor));
    }

    @PostMapping("/{businessDate}/bod/complete")
    public EodStatusDto completeBod(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestHeader(name = AUTH_USER_HEADER, required = false) String actor,
            @RequestHeader(name = AUTH_ROLES_HEADER, required = false) String roles) {
        String trustedActor = ClosingCommandAuthority.requireCommandAuthority(
                actor, roles, EOD_ACTOR_MAX_LENGTH);
        return EodStatusDto.from(eodLifecycleUseCase.completeBod(businessDate, trustedActor));
    }
}
