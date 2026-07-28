package com.ho.account.reconciliation.api.adapter.in.web;

import com.ho.account.reconciliation.api.dto.DifferenceReasonCodeDto;
import com.ho.account.reconciliation.api.dto.DifferenceReasonCodeRequestDto;
import com.ho.account.reconciliation.api.dto.ReconciliationDifferenceAssignmentRequestDto;
import com.ho.account.reconciliation.api.dto.ReconciliationDifferenceDto;
import com.ho.account.reconciliation.api.dto.ReconciliationDifferenceResolutionRequestDto;
import com.ho.account.reconciliation.api.dto.ReconciliationRuleDto;
import com.ho.account.reconciliation.api.dto.ReconciliationRuleRequestDto;
import com.ho.account.reconciliation.api.dto.ReconciliationRunRequestDto;
import com.ho.account.reconciliation.api.dto.ReconciliationRunResponseDto;
import com.ho.account.reconciliation.api.dto.ReconciliationUnitDto;
import com.ho.account.reconciliation.api.dto.ReconciliationUnitRequestDto;
import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.service.ReconciliationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 대사(Reconciliation) 관련 REST API를 제공하는 컨트롤러.
 *
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 대사 시스템의 '안내 데스크'입니다.
 * 사용자가 웹 화면에서 "대사 규칙을 새로 만들어줘", "지금 바로 대사 작업 시작해"라고 요청하면,
 * 그 요청을 받아서 내부 서비스(`ReconciliationService`)가 이해할 수 있는 명령으로 번역해서 전달하는 역할을 합니다.
 * 화면(UI)과 내부 로직(Core) 사이의 통로라고 보시면 됩니다.
 *
 * <p>현재 컨트롤러는 HTTP 요청 검증과 DTO/command 변환만 담당합니다. 대사 단위 조회, 규칙 연결,
 * 차이 해결 정책, 조정 전표 검증은 core 서비스가 처리합니다.</p>
 */
@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    // --- ReconciliationUnit (대사 단위) API ---

    /**
     * 새로운 대사 단위를 생성합니다.
     * @param requestDto 생성할 대사 단위 정보
     * @return 생성된 대사 단위 정보
     */
    @PostMapping("/units")
    public ResponseEntity<ReconciliationUnitDto> createReconciliationUnit(
            @Valid @RequestBody ReconciliationUnitRequestDto requestDto) {
        ReconciliationUnit createdUnit = reconciliationService.createReconciliationUnit(requestDto.toCommand());
        return new ResponseEntity<>(ReconciliationUnitDto.fromEntity(createdUnit), HttpStatus.CREATED);
    }

    /** 모든 대사 단위를 조회합니다. */
    @GetMapping("/units")
    public ResponseEntity<List<ReconciliationUnitDto>> getAllReconciliationUnits() {
        List<ReconciliationUnit> units = reconciliationService.findAllReconciliationUnits();
        return ResponseEntity.ok(units.stream().map(ReconciliationUnitDto::fromEntity).toList());
    }

    /** ID로 대사 단위를 조회합니다. */
    @GetMapping("/units/{id}")
    public ResponseEntity<ReconciliationUnitDto> getReconciliationUnitById(@PathVariable("id") Long id) {
        ReconciliationUnit unit = reconciliationService.findReconciliationUnitById(id);
        return ResponseEntity.ok(ReconciliationUnitDto.fromEntity(unit));
    }

    /** 대사 단위를 업데이트합니다. */
    @PutMapping("/units/{id}")
    public ResponseEntity<ReconciliationUnitDto> updateReconciliationUnit(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReconciliationUnitRequestDto requestDto) {
        ReconciliationUnit updatedUnit = reconciliationService.updateReconciliationUnit(id, requestDto.toCommand());
        return ResponseEntity.ok(ReconciliationUnitDto.fromEntity(updatedUnit));
    }

    /** 대사 단위를 삭제합니다. */
    @DeleteMapping("/units/{id}")
    public ResponseEntity<Void> deleteReconciliationUnit(@PathVariable("id") Long id) {
        reconciliationService.deleteReconciliationUnit(id);
        return ResponseEntity.noContent().build();
    }

    // --- ReconciliationRule (대사 규칙) API ---

    /** 새로운 대사 규칙을 생성합니다. */
    @PostMapping("/rules")
    public ResponseEntity<ReconciliationRuleDto> createReconciliationRule(
            @Valid @RequestBody ReconciliationRuleRequestDto requestDto) {
        ReconciliationRule createdRule = reconciliationService.createReconciliationRule(requestDto.toCommand());
        return new ResponseEntity<>(ReconciliationRuleDto.fromEntity(createdRule), HttpStatus.CREATED);
    }

    /** 특정 대사 단위에 속한 모든 대사 규칙을 조회합니다. */
    @GetMapping("/units/{unitId}/rules")
    public ResponseEntity<List<ReconciliationRuleDto>> getRulesByReconciliationUnit(@PathVariable("unitId") Long unitId) {
        List<ReconciliationRule> rules = reconciliationService.findRulesByReconciliationUnit(unitId);
        return ResponseEntity.ok(rules.stream().map(ReconciliationRuleDto::fromEntity).toList());
    }

    /** 대사 규칙을 업데이트합니다. */
    @PutMapping("/rules/{id}")
    public ResponseEntity<ReconciliationRuleDto> updateReconciliationRule(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReconciliationRuleRequestDto requestDto) {
        ReconciliationRule updatedRule = reconciliationService.updateReconciliationRule(id, requestDto.toCommand());
        return ResponseEntity.ok(ReconciliationRuleDto.fromEntity(updatedRule));
    }

    /** 대사 규칙을 삭제합니다. */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteReconciliationRule(@PathVariable("id") Long id) {
        reconciliationService.deleteReconciliationRule(id);
        return ResponseEntity.noContent().build();
    }

    // --- DifferenceReasonCode (차이 사유 코드) API ---

    /** 새로운 차이 사유 코드를 생성합니다. */
    @PostMapping("/reason-codes")
    public ResponseEntity<DifferenceReasonCodeDto> createDifferenceReasonCode(
            @Valid @RequestBody DifferenceReasonCodeRequestDto requestDto) {
        DifferenceReasonCode createdCode = reconciliationService.createDifferenceReasonCode(requestDto.toCommand());
        return new ResponseEntity<>(DifferenceReasonCodeDto.fromEntity(createdCode), HttpStatus.CREATED);
    }

    /** 모든 차이 사유 코드를 조회합니다. */
    @GetMapping("/reason-codes")
    public ResponseEntity<List<DifferenceReasonCodeDto>> getAllDifferenceReasonCodes() {
        List<DifferenceReasonCode> reasonCodes = reconciliationService.findAllDifferenceReasonCodes();
        return ResponseEntity.ok(reasonCodes.stream().map(DifferenceReasonCodeDto::fromEntity).toList());
    }

    /** ID로 차이 사유 코드를 조회합니다. */
    @GetMapping("/reason-codes/{id}")
    public ResponseEntity<DifferenceReasonCodeDto> getDifferenceReasonCodeById(@PathVariable("id") Long id) {
        DifferenceReasonCode reasonCode = reconciliationService.findDifferenceReasonCodeById(id);
        return ResponseEntity.ok(DifferenceReasonCodeDto.fromEntity(reasonCode));
    }

    /** 차이 사유 코드를 업데이트합니다. */
    @PutMapping("/reason-codes/{id}")
    public ResponseEntity<DifferenceReasonCodeDto> updateDifferenceReasonCode(
            @PathVariable("id") Long id,
            @Valid @RequestBody DifferenceReasonCodeRequestDto requestDto) {
        DifferenceReasonCode updatedCode = reconciliationService.updateDifferenceReasonCode(id, requestDto.toCommand());
        return ResponseEntity.ok(DifferenceReasonCodeDto.fromEntity(updatedCode));
    }

    /** 차이 사유 코드를 삭제합니다. */
    @DeleteMapping("/reason-codes/{id}")
    public ResponseEntity<Void> deleteDifferenceReasonCode(@PathVariable("id") Long id) {
        reconciliationService.deleteDifferenceReasonCode(id);
        return ResponseEntity.noContent().build();
    }

    // --- Reconciliation Run API ---

    /** 특정 대사 단위를 기준으로 대사를 실행합니다. */
    @PostMapping("/run")
    public ResponseEntity<ReconciliationRunResponseDto> runReconciliation(
            @Valid @RequestBody ReconciliationRunRequestDto requestDto,
            @RequestHeader(value = "X-Audit-User", defaultValue = "SYSTEM") String auditUser) {
        ReconciliationRun run = reconciliationService.performReconciliation(requestDto.toCommand(auditUser));
        return new ResponseEntity<>(ReconciliationRunResponseDto.fromEntity(run), HttpStatus.CREATED);
    }

    // --- Reconciliation Difference API ---

    /** 특정 대사 차이를 사용자에게 할당하고 SLA 기한을 설정합니다. */
    @PostMapping("/differences/assign")
    public ResponseEntity<ReconciliationDifferenceDto> assignDifference(
            @Valid @RequestBody ReconciliationDifferenceAssignmentRequestDto requestDto) {
        ReconciliationDifference assignedDifference = reconciliationService.assignDifference(requestDto.toCommand());
        return ResponseEntity.ok(ReconciliationDifferenceDto.fromEntity(assignedDifference));
    }

    /** 특정 대사 차이를 해결 또는 무시 처리합니다. */
    @PostMapping("/differences/resolve")
    public ResponseEntity<ReconciliationDifferenceDto> resolveDifference(
            @Valid @RequestBody ReconciliationDifferenceResolutionRequestDto requestDto) {
        ReconciliationDifference resolvedDifference = reconciliationService.resolveDifference(requestDto.toCommand());
        return ResponseEntity.ok(ReconciliationDifferenceDto.fromEntity(resolvedDifference));
    }

    /** 특정 ReconciliationRun에 속한 모든 ReconciliationDifference를 조회합니다. */
    @GetMapping("/runs/{runId}/differences")
    public ResponseEntity<List<ReconciliationDifferenceDto>> getDifferencesByRunId(@PathVariable("runId") Long runId) {
        List<ReconciliationDifference> differences = reconciliationService.findDifferencesByReconciliationRunId(runId);
        return ResponseEntity.ok(differences.stream().map(ReconciliationDifferenceDto::fromEntity).toList());
    }
}