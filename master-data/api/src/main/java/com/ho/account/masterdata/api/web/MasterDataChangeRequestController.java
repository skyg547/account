package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.api.dto.MasterDataChangeDecisionDto;
import com.ho.account.masterdata.api.dto.MasterDataChangeRequestCreateDto;
import com.ho.account.masterdata.api.dto.MasterDataChangeRequestDto;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 통제 대상 기준정보 변경 요청의 HTTP 인바운드 어댑터입니다.
 *
 * <p>@todo 현재 계정과목/부서/거래처/상품 Controller의 직접 쓰기 API가 이 승인 흐름과
 * 공존합니다. 운영 전 인바운드 권한 정책으로 직접 쓰기를 관리자 보정 전용으로 제한하거나,
 * 모든 일반 변경을 이 승인 유즈케이스로 통합해야 합니다.</p>
 *
 * <p>서비스 입구의 인증 필터가 JWT 서명과 claim을 재검증하고 만든
 * {@code X-Auth-User}/{@code X-Auth-Roles}를 actor와 권한으로 사용합니다.</p>
 *
 * <p>@todo pending 목록도 요청이 누적되면 무제한 List가 됩니다. 완료 조건은 status/requestedAt/id
 * 기반 pagination과 최대 page size를 포트부터 HTTP 계약까지 연결하고, 실제 DB query limit를
 * 통합 테스트로 검증하는 것입니다.</p>
 */
@RestController
@RequestMapping("/api/master-data/change-requests")
public class MasterDataChangeRequestController {

    private static final String AUTH_USER_HEADER = "X-Auth-User";
    private static final String AUTH_ROLES_HEADER = "X-Auth-Roles";
    private static final Set<String> APPROVAL_ROLES =
            Set.of("PARTNER_MANAGER", "MASTER_MANAGER", "ACCOUNTING_ADMIN", "SYSTEM_ADMIN", "ADMIN");

    private final MasterDataChangeRequestUseCase useCase;

    public MasterDataChangeRequestController(MasterDataChangeRequestUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<MasterDataChangeRequestDto> requestChange(
            @RequestHeader(AUTH_USER_HEADER) String authenticatedUser,
            @RequestHeader(AUTH_ROLES_HEADER) String authenticatedRoles,
            @Valid @RequestBody MasterDataChangeRequestCreateDto requestDto) {
        requireApprovalRole(authenticatedRoles);
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(
                useCase.requestChange(requestDto.toCommand(authenticatedUser))));
    }

    @GetMapping("/pending")
    public List<MasterDataChangeRequestDto> findPendingRequests(
            @RequestHeader(AUTH_ROLES_HEADER) String authenticatedRoles) {
        requireApprovalRole(authenticatedRoles);
        return useCase.findPendingRequests().stream()
                .map(MasterDataChangeRequestDto::fromEntity)
                .toList();
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<MasterDataChangeRequestDto> approve(@PathVariable Long requestId,
            @RequestHeader(AUTH_USER_HEADER) String authenticatedUser,
            @RequestHeader(AUTH_ROLES_HEADER) String authenticatedRoles) {
        requireApprovalRole(authenticatedRoles);
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(
                useCase.approve(requestId, authenticatedUser)));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<MasterDataChangeRequestDto> reject(@PathVariable Long requestId,
            @RequestHeader(AUTH_USER_HEADER) String authenticatedUser,
            @RequestHeader(AUTH_ROLES_HEADER) String authenticatedRoles,
            @Valid @RequestBody MasterDataChangeDecisionDto decisionDto) {
        requireApprovalRole(authenticatedRoles);
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(
                useCase.reject(requestId, authenticatedUser, decisionDto.getReason())));
    }

    @PostMapping("/{requestId}/apply")
    public ResponseEntity<MasterDataChangeRequestDto> markApplied(@PathVariable Long requestId,
            @RequestHeader(AUTH_ROLES_HEADER) String authenticatedRoles) {
        requireApprovalRole(authenticatedRoles);
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(useCase.applyApprovedChange(requestId)));
    }

    @PostMapping("/apply-due")
    public List<MasterDataChangeRequestDto> applyDueApprovedChanges(
            @RequestHeader(AUTH_ROLES_HEADER) String authenticatedRoles) {
        requireApprovalRole(authenticatedRoles);
        return useCase.applyDueApprovedChanges().stream()
                .map(MasterDataChangeRequestDto::fromEntity)
                .toList();
    }

    private void requireApprovalRole(String authenticatedRoles) {
        boolean authorized = Arrays.stream(authenticatedRoles.split(","))
                .map(String::trim)
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .anyMatch(APPROVAL_ROLES::contains);
        if (!authorized) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Master Data approval role is required.");
        }
    }
}
