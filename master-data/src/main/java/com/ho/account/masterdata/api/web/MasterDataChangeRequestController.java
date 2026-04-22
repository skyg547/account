package com.ho.account.masterdata.api.web;

import com.ho.account.masterdata.api.dto.MasterDataChangeDecisionDto;
import com.ho.account.masterdata.api.dto.MasterDataChangeRequestCreateDto;
import com.ho.account.masterdata.api.dto.MasterDataChangeRequestDto;
import com.ho.account.masterdata.core.application.usecase.MasterDataChangeRequestUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-data/change-requests")
public class MasterDataChangeRequestController {

    private final MasterDataChangeRequestUseCase useCase;

    public MasterDataChangeRequestController(MasterDataChangeRequestUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<MasterDataChangeRequestDto> requestChange(
            @RequestBody MasterDataChangeRequestCreateDto requestDto) {
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
            @RequestBody MasterDataChangeDecisionDto decisionDto) {
        return ResponseEntity.ok(MasterDataChangeRequestDto.fromEntity(useCase.approve(requestId,
                decisionDto.getApprover())));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<MasterDataChangeRequestDto> reject(@PathVariable Long requestId,
            @RequestBody MasterDataChangeDecisionDto decisionDto) {
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
