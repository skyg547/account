package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.api.dto.MasterDataChangeDecisionDto;
import com.ho.account.masterdata.api.dto.MasterDataChangeRequestCreateDto;
import com.ho.account.masterdata.api.dto.MasterDataChangeRequestDto;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통제 대상 기준정보 변경 요청의 HTTP 인바운드 어댑터입니다.
 *
 * <p>현재 계정과목/부서/거래처/상품 Controller의 직접 쓰기 API가 이 승인 흐름과
 * 공존합니다. 운영 전 인바운드 권한 정책으로 직접 쓰기를 관리자 보정 전용으로 제한하거나,
 * 모든 일반 변경을 이 승인 유즈케이스로 통합해야 합니다.</p>
 *
 * <p>현재 requestedBy/approver를 요청 본문이 정합니다. Gateway가 검증한 사용자 정보를
 * Spring Security principal로 연결한 뒤 신뢰된 사용자 ID를 서버에서 주입해야 합니다.</p>
 *
 * <p>pending 목록도 요청이 누적되면 무제한 List가 됩니다. 완료 조건은 status/requestedAt/id
 * 기반 pagination과 최대 page size를 포트부터 HTTP 계약까지 연결하고, 실제 DB query limit를
 * 통합 테스트로 검증하는 것입니다.</p>
 */
@RestController
@RequestMapping("/api/master-data/change-requests")
public class MasterDataChangeRequestController {

    private final MasterDataChangeRequestUseCase useCase;

    public MasterDataChangeRequestController(MasterDataChangeRequestUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<MasterDataChangeRequestDto> requestChange(
            @Valid @RequestBody MasterDataChangeRequestCreateDto requestDto) {
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(useCase.requestChange(requestDto.toCommand())));
    }

    @GetMapping("/pending")
    public List<MasterDataChangeRequestDto> findPendingRequests() {
        return useCase.findPendingRequests().stream()
                .map(MasterDataChangeRequestDto::fromEntity)
                .toList();
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<MasterDataChangeRequestDto> approve(@PathVariable Long requestId,
            @Valid @RequestBody MasterDataChangeDecisionDto decisionDto) {
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(useCase.approve(requestId,
                decisionDto.getApprover())));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<MasterDataChangeRequestDto> reject(@PathVariable Long requestId,
            @Valid @RequestBody MasterDataChangeDecisionDto decisionDto) {
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(useCase.reject(requestId,
                decisionDto.getApprover(), decisionDto.getReason())));
    }

    @PostMapping("/{requestId}/apply")
    public ResponseEntity<MasterDataChangeRequestDto> markApplied(@PathVariable Long requestId) {
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(useCase.applyApprovedChange(requestId)));
    }

    @PostMapping("/apply-due")
    public List<MasterDataChangeRequestDto> applyDueApprovedChanges() {
        return useCase.applyDueApprovedChanges().stream()
                .map(MasterDataChangeRequestDto::fromEntity)
                .toList();
    }
}
