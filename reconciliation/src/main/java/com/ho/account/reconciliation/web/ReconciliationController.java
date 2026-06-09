package com.ho.account.reconciliation.web;

import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import com.ho.account.reconciliation.domain.ReconciliationDifference; // Added import
import com.ho.account.reconciliation.domain.ReconciliationRun; // Added import
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.dto.*;
import com.ho.account.reconciliation.service.ReconciliationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 대사(Reconciliation) 관련 REST API를 제공하는 컨트롤러.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 대사 시스템의 '안내 데스크'입니다.
 * 사용자가 웹 화면에서 "대사 규칙을 새로 만들어줘", "지금 바로 대사 작업 시작해"라고 요청하면, 
 * 그 요청을 받아서 내부 서비스(`ReconciliationService`)가 이해할 수 있는 명령으로 번역해서 전달하는 역할을 합니다.
 * 화면(UI)과 내부 로직(Core) 사이의 통로라고 보시면 됩니다.
 */
@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @Autowired
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
    public ResponseEntity<ReconciliationUnitDto> createReconciliationUnit(@Valid @RequestBody ReconciliationUnitRequestDto requestDto) {
        ReconciliationUnit reconciliationUnit = requestDto.toEntity();
        ReconciliationUnit createdUnit = reconciliationService.createReconciliationUnit(reconciliationUnit);
        return new ResponseEntity<>(ReconciliationUnitDto.fromEntity(createdUnit), HttpStatus.CREATED);
    }

    /**
     * 모든 대사 단위를 조회합니다.
     * @return 모든 대사 단위 목록
     */
    @GetMapping("/units")
    public ResponseEntity<List<ReconciliationUnitDto>> getAllReconciliationUnits() {
        List<ReconciliationUnit> units = reconciliationService.findAllReconciliationUnits();
        List<ReconciliationUnitDto> dtoList = units.stream()
                .map(ReconciliationUnitDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    /**
     * ID로 대사 단위를 조회합니다.
     * @param id 대사 단위 ID
     * @return 조회된 대사 단위 정보
     */
    @GetMapping("/units/{id}")
    public ResponseEntity<ReconciliationUnitDto> getReconciliationUnitById(@PathVariable("id") Long id) {
        ReconciliationUnit unit = reconciliationService.findReconciliationUnitById(id);
        return ResponseEntity.ok(ReconciliationUnitDto.fromEntity(unit));
    }

    /**
     * 대사 단위를 업데이트합니다.
     * @param id 업데이트할 대사 단위 ID
     * @param requestDto 업데이트할 대사 단위 정보
     * @return 업데이트된 대사 단위 정보
     */
    @PutMapping("/units/{id}")
    public ResponseEntity<ReconciliationUnitDto> updateReconciliationUnit(@PathVariable("id") Long id, @Valid @RequestBody ReconciliationUnitRequestDto requestDto) {
        ReconciliationUnit updatedUnitEntity = requestDto.toEntity(); // DTO에서 Entity로 변환
        ReconciliationUnit updatedUnit = reconciliationService.updateReconciliationUnit(id, updatedUnitEntity);
        return ResponseEntity.ok(ReconciliationUnitDto.fromEntity(updatedUnit));
    }

    /**
     * 대사 단위를 삭제합니다.
     * @param id 삭제할 대사 단위 ID
     * @return 응답 없음
     */
    @DeleteMapping("/units/{id}")
    public ResponseEntity<Void> deleteReconciliationUnit(@PathVariable("id") Long id) {
        reconciliationService.deleteReconciliationUnit(id);
        return ResponseEntity.noContent().build();
    }

    // --- ReconciliationRule (대사 규칙) API ---

    /**
     * 새로운 대사 규칙을 생성합니다.
     * @param requestDto 생성할 대사 규칙 정보
     * @return 생성된 대사 규칙 정보
     */
    @PostMapping("/rules")
    public ResponseEntity<ReconciliationRuleDto> createReconciliationRule(@Valid @RequestBody ReconciliationRuleRequestDto requestDto) {
        // ReconciliationUnit을 먼저 조회하여 Rule에 설정
        ReconciliationUnit reconciliationUnit = reconciliationService.findReconciliationUnitById(requestDto.getReconciliationUnitId());

        ReconciliationRule reconciliationRule = new ReconciliationRule();
        reconciliationRule.setReconciliationUnit(reconciliationUnit);
        reconciliationRule.setName(requestDto.getName());
        reconciliationRule.setRuleDefinitionJson(requestDto.getRuleDefinitionJson());
        reconciliationRule.setToleranceType(requestDto.getToleranceType());
        reconciliationRule.setToleranceValue(requestDto.getToleranceValue());
        reconciliationRule.setPriority(requestDto.getPriority());
        reconciliationRule.setActive(requestDto.isActive());

        ReconciliationRule createdRule = reconciliationService.createReconciliationRule(reconciliationRule);
        return new ResponseEntity<>(ReconciliationRuleDto.fromEntity(createdRule), HttpStatus.CREATED);
    }

    /**
     * 특정 대사 단위에 속한 모든 대사 규칙을 조회합니다.
     * @param unitId 대사 단위 ID
     * @return 대사 규칙 목록
     */
    @GetMapping("/units/{unitId}/rules")
    public ResponseEntity<List<ReconciliationRuleDto>> getRulesByReconciliationUnit(@PathVariable("unitId") Long unitId) {
        List<ReconciliationRule> rules = reconciliationService.findRulesByReconciliationUnit(unitId);
        List<ReconciliationRuleDto> dtoList = rules.stream()
                .map(ReconciliationRuleDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    /**
     * 대사 규칙을 업데이트합니다.
     * @param id 업데이트할 대사 규칙 ID
     * @param requestDto 업데이트할 대사 규칙 정보
     * @return 업데이트된 대사 규칙 정보
     */
    @PutMapping("/rules/{id}")
    public ResponseEntity<ReconciliationRuleDto> updateReconciliationRule(@PathVariable("id") Long id, @Valid @RequestBody ReconciliationRuleRequestDto requestDto) {
        ReconciliationUnit reconciliationUnit = reconciliationService.findReconciliationUnitById(requestDto.getReconciliationUnitId());

        ReconciliationRule updatedRuleEntity = new ReconciliationRule();
        updatedRuleEntity.setReconciliationUnit(reconciliationUnit);
        updatedRuleEntity.setName(requestDto.getName());
        updatedRuleEntity.setRuleDefinitionJson(requestDto.getRuleDefinitionJson());
        updatedRuleEntity.setToleranceType(requestDto.getToleranceType());
        updatedRuleEntity.setToleranceValue(requestDto.getToleranceValue());
        updatedRuleEntity.setPriority(requestDto.getPriority());
        updatedRuleEntity.setActive(requestDto.isActive());

        ReconciliationRule updatedRule = reconciliationService.updateReconciliationRule(id, updatedRuleEntity);
        return ResponseEntity.ok(ReconciliationRuleDto.fromEntity(updatedRule));
    }

    /**
     * 대사 규칙을 삭제합니다.
     * @param id 삭제할 대사 규칙 ID
     * @return 응답 없음
     */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteReconciliationRule(@PathVariable("id") Long id) {
        reconciliationService.deleteReconciliationRule(id);
        return ResponseEntity.noContent().build();
    }

    // --- DifferenceReasonCode (차이 사유 코드) API ---

    /**
     * 새로운 차이 사유 코드를 생성합니다.
     * @param requestDto 생성할 차이 사유 코드 정보
     * @return 생성된 차이 사유 코드 정보
     */
    @PostMapping("/reason-codes")
    public ResponseEntity<DifferenceReasonCodeDto> createDifferenceReasonCode(@Valid @RequestBody DifferenceReasonCodeRequestDto requestDto) {
        DifferenceReasonCode reasonCode = requestDto.toEntity();
        DifferenceReasonCode createdCode = reconciliationService.createDifferenceReasonCode(reasonCode);
        return new ResponseEntity<>(DifferenceReasonCodeDto.fromEntity(createdCode), HttpStatus.CREATED);
    }

    /**
     * 모든 차이 사유 코드를 조회합니다.
     * @return 모든 차이 사유 코드 목록
     */
    @GetMapping("/reason-codes")
    public ResponseEntity<List<DifferenceReasonCodeDto>> getAllDifferenceReasonCodes() {
        List<DifferenceReasonCode> reasonCodes = reconciliationService.findAllDifferenceReasonCodes();
        List<DifferenceReasonCodeDto> dtoList = reasonCodes.stream()
                .map(DifferenceReasonCodeDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    /**
     * ID로 차이 사유 코드를 조회합니다.
     * @param id 차이 사유 코드 ID
     * @return 조회된 차이 사유 코드 정보
     */
    @GetMapping("/reason-codes/{id}")
    public ResponseEntity<DifferenceReasonCodeDto> getDifferenceReasonCodeById(@PathVariable("id") Long id) {
        DifferenceReasonCode reasonCode = reconciliationService.findDifferenceReasonCodeById(id);
        return ResponseEntity.ok(DifferenceReasonCodeDto.fromEntity(reasonCode));
    }

    /**
     * 차이 사유 코드를 업데이트합니다.
     * @param id 업데이트할 차이 사유 코드 ID
     * @param requestDto 업데이트할 차이 사유 코드 정보
     * @return 업데이트된 차이 사유 코드 정보
     */
    @PutMapping("/reason-codes/{id}")
    public ResponseEntity<DifferenceReasonCodeDto> updateDifferenceReasonCode(@PathVariable("id") Long id, @Valid @RequestBody DifferenceReasonCodeRequestDto requestDto) {
        DifferenceReasonCode updatedCodeEntity = requestDto.toEntity();
        DifferenceReasonCode updatedCode = reconciliationService.updateDifferenceReasonCode(id, updatedCodeEntity);
        return ResponseEntity.ok(DifferenceReasonCodeDto.fromEntity(updatedCode));
    }

    /**
     * 차이 사유 코드를 삭제합니다.
     * @param id 삭제할 차이 사유 코드 ID
     * @return 응답 없음
     */
    @DeleteMapping("/reason-codes/{id}")
    public ResponseEntity<Void> deleteDifferenceReasonCode(@PathVariable("id") Long id) {
        reconciliationService.deleteDifferenceReasonCode(id);
        return ResponseEntity.noContent().build();
    }

    // --- Reconciliation Run API ---

    /**
     * 특정 대사 단위를 기준으로 대사를 실행합니다.
     * @param requestDto 대사 실행 요청 정보 (대사 단위 ID, 기준일)
     * @return 대사 실행 결과 정보
     */
    @PostMapping("/run")
    public ResponseEntity<ReconciliationRunResponseDto> runReconciliation(@Valid @RequestBody ReconciliationRunRequestDto requestDto,
                                                                          @RequestHeader(value = "X-Audit-User", defaultValue = "SYSTEM") String auditUser) {
        ReconciliationRun run = reconciliationService.performReconciliation(requestDto.getReconciliationUnitId(), requestDto.getReconciliationDate(), auditUser);
        return new ResponseEntity<>(ReconciliationRunResponseDto.fromEntity(run), HttpStatus.CREATED);
    }

    // --- Reconciliation Difference API ---

    /**
     * 특정 대사 차이를 사용자에게 할당하고 SLA 기한을 설정합니다.
     * @param requestDto 대사 차이 할당 요청 정보
     * @return 업데이트된 대사 차이 정보
     */
    @PostMapping("/differences/assign")
    public ResponseEntity<ReconciliationDifferenceDto> assignDifference(@Valid @RequestBody ReconciliationDifferenceAssignmentRequestDto requestDto) {
        ReconciliationDifference assignedDifference = reconciliationService.assignDifference(
                requestDto.getDifferenceId(),
                requestDto.getAssignedToUser(),
                requestDto.getSlaDueDate()
        );
        return ResponseEntity.ok(ReconciliationDifferenceDto.fromEntity(assignedDifference));
    }

    /**
     * 특정 대사 차이를 해결 또는 무시 처리합니다.
     * DoD에 따라 사유 코드와 필요 시 조정 전표 링크를 함께 확정합니다.
     *
     * @param requestDto 대사 차이 해결 요청 정보
     * @return 갱신된 대사 차이 정보
     */
    @PostMapping("/differences/resolve")
    public ResponseEntity<ReconciliationDifferenceDto> resolveDifference(@Valid @RequestBody ReconciliationDifferenceResolutionRequestDto requestDto) {
        ReconciliationDifference resolvedDifference = reconciliationService.resolveDifference(
                requestDto.getDifferenceId(),
                requestDto.getReasonCodeId(),
                requestDto.getAdjustmentJournalEntryId(),
                requestDto.getStatus(),
                requestDto.getResolvedBy()
        );
        return ResponseEntity.ok(ReconciliationDifferenceDto.fromEntity(resolvedDifference));
    }

    /**
     * 특정 ReconciliationRun에 속한 모든 ReconciliationDifference를 조회합니다.
     * @param runId ReconciliationRun의 ID
     * @return ReconciliationDifference 목록
     */
    @GetMapping("/runs/{runId}/differences")
    public ResponseEntity<List<ReconciliationDifferenceDto>> getDifferencesByRunId(@PathVariable("runId") Long runId) {
        List<ReconciliationDifference> differences = reconciliationService.findDifferencesByReconciliationRunId(runId);
        List<ReconciliationDifferenceDto> dtoList = differences.stream()
                .map(ReconciliationDifferenceDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
}
