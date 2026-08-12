package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.reconciliation.application.port.in.AssignDifferenceCommand;
import com.ho.account.reconciliation.application.port.in.DifferenceReasonCodeCommand;
import com.ho.account.reconciliation.application.port.in.ReconciliationRuleCommand;
import com.ho.account.reconciliation.application.port.in.ReconciliationUnitCommand;
import com.ho.account.reconciliation.application.port.in.ResolveDifferenceCommand;
import com.ho.account.reconciliation.application.port.in.RunReconciliationCommand;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotPort;
import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationAdjustmentPolicy;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.DifferenceReasonCodeRepository;
import com.ho.account.reconciliation.repository.ReconciliationDifferenceRepository;
import com.ho.account.reconciliation.repository.ReconciliationRuleRepository;
import com.ho.account.reconciliation.repository.ReconciliationRunRepository;
import com.ho.account.reconciliation.repository.ReconciliationUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [파사드 패턴 (Facade Pattern) & 단일 책임 원칙 (SRP) 기반 유즈케이스 진입점]
 * 대사(Reconciliation) 시스템의 세부 전용 서비스들을 조율하는 파사드(Facade) 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명: God Class 리팩토링 및 파사드 패턴(Facade Pattern)]
 * 
 * 1. **과거 문제점 (God Class 악취)**
 *    기존 {@link ReconciliationService}는 680행 이상의 거대 클래스(God Class)로서
 *    - 대사 단위(Unit) CRUD
 *    - 대사 규칙(Rule) 및 사유 코드(ReasonCode) CRUD
 *    - 대사 실행(Run) 오케스트레이션 및 항목 매칭
 *    - 차액(Difference) 배정 및 해결
 *    등 서로 다른 변경 사유를 가지는 책임들이 한 클래스에 비대하게 뭉쳐 있었습니다.
 *    이로 인해 코드 읽기/테스트가 어렵고, 한 부분의 변경이 다른 기능에 부작용(Side-effect)을 일으킬 수 있었습니다.
 * 
 * 2. **해결책: SRP 기반 전용 서비스 분리 및 파사드 패턴 도입**
 *    - 대사 단위 관리 책임 ➔ {@link ReconciliationUnitService}
 *    - 대사 규칙/사유 코드 관리 책임 ➔ {@link ReconciliationRuleService}
 *    - 대사 실행 및 매칭/차액 오케스트레이션 ➔ {@link ReconciliationExecutionService}
 *    
 *    그리고 본 클래스({@link ReconciliationService})는 클라이언트(Controller, Batch 등)가 여러 하위 서비스 구조를
 *    직접 알 필요 없이 하나의 통일된 진입점으로 호출할 수 있도록 **파사드(Facade) 역할**만 담당합니다.
 * 
 * 3. **파사드 패턴의 우수성**
 *    - **하위 호환성 유지**: 기존 REST Controller나 Batch 서비스 등 외부 호출자와의 API 계약을 100% 유지합니다.
 *    - **복잡성 은닉 (Encapsulation)**: 세부 비즈니스 로직 및 분리된 서비스들의 관계를 클라이언트로부터 숨깁니다.
 *    - **테스트 용이성 및 유연한 확장**: 개별 비즈니스 로직 변경 및 단위 테스트 생성이 용이해집니다.
 */
@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationUnitService reconciliationUnitService;
    private final ReconciliationRuleService reconciliationRuleService;
    private final ReconciliationExecutionService reconciliationExecutionService;

    /**
     * Spring DI 환경에서 분리된 3개의 전용 서비스를 주입받는 표준 파사드 생성자.
     */
    @Autowired
    public ReconciliationService(ReconciliationUnitService reconciliationUnitService,
                                 ReconciliationRuleService reconciliationRuleService,
                                 ReconciliationExecutionService reconciliationExecutionService) {
        this.reconciliationUnitService = reconciliationUnitService;
        this.reconciliationRuleService = reconciliationRuleService;
        this.reconciliationExecutionService = reconciliationExecutionService;
    }

    /**
     * [하위 호환성 지원 레거시 생성자]
     * 단위 테스트 코드 및 레거시 생성자 호출 방식을 지원하기 위해 개별 레포지토리 및 포트를 받아
     * 내부적으로 하위 서비스들을 자동 구성하는 편의 생성자입니다.
     */
    public ReconciliationService(ReconciliationUnitRepository reconciliationUnitRepository,
                                 ReconciliationRuleRepository reconciliationRuleRepository,
                                 DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                 ReconciliationRunRepository reconciliationRunRepository,
                                 ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                 JournalQueryPort journalQueryPort,
                                 JournalPostingPort journalPostingPort,
                                 ObjectMapper objectMapper,
                                 ReconciliationAdjustmentPolicy adjustmentPolicy,
                                 ExternalReconSnapshotPort externalReconSnapshotPort,
                                 ReconciliationMatchingEngine matchingEngine) {
        ReconciliationUnitService unitService = new ReconciliationUnitService(reconciliationUnitRepository);
        ReconciliationRuleService ruleService = new ReconciliationRuleService(reconciliationRuleRepository, differenceReasonCodeRepository, unitService);
        ReconciliationExecutionService execService = new ReconciliationExecutionService(
                reconciliationUnitRepository, reconciliationRuleRepository, differenceReasonCodeRepository,
                reconciliationRunRepository, reconciliationDifferenceRepository, journalQueryPort,
                journalPostingPort, objectMapper, adjustmentPolicy, externalReconSnapshotPort, matchingEngine);

        this.reconciliationUnitService = unitService;
        this.reconciliationRuleService = ruleService;
        this.reconciliationExecutionService = execService;
    }

    public ReconciliationService(ReconciliationUnitRepository reconciliationUnitRepository,
                                 ReconciliationRuleRepository reconciliationRuleRepository,
                                 DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                 ReconciliationRunRepository reconciliationRunRepository,
                                 ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                 JournalQueryPort journalQueryPort,
                                 JournalPostingPort journalPostingPort,
                                 ObjectMapper objectMapper,
                                 ReconciliationAdjustmentPolicy adjustmentPolicy,
                                 ExternalReconSnapshotPort externalReconSnapshotPort) {
        this(reconciliationUnitRepository, reconciliationRuleRepository, differenceReasonCodeRepository,
             reconciliationRunRepository, reconciliationDifferenceRepository, journalQueryPort,
             journalPostingPort, objectMapper, adjustmentPolicy, externalReconSnapshotPort, null);
    }

    // --- ReconciliationUnit Delegation ---

    public ReconciliationUnit createReconciliationUnit(ReconciliationUnitCommand command) {
        return reconciliationUnitService.createReconciliationUnit(command);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationUnit> findAllReconciliationUnits() {
        return reconciliationUnitService.findAllReconciliationUnits();
    }

    @Transactional(readOnly = true)
    public ReconciliationUnit findReconciliationUnitById(Long id) {
        return reconciliationUnitService.findReconciliationUnitById(id);
    }

    public ReconciliationUnit updateReconciliationUnit(Long id, ReconciliationUnitCommand command) {
        return reconciliationUnitService.updateReconciliationUnit(id, command);
    }

    public void deleteReconciliationUnit(Long id) {
        reconciliationUnitService.deleteReconciliationUnit(id);
    }

    // --- ReconciliationRule & DifferenceReasonCode Delegation ---

    public ReconciliationRule createReconciliationRule(ReconciliationRuleCommand command) {
        return reconciliationRuleService.createReconciliationRule(command);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRule> findRulesByReconciliationUnit(Long unitId) {
        return reconciliationRuleService.findRulesByReconciliationUnit(unitId);
    }

    public ReconciliationRule updateReconciliationRule(Long id, ReconciliationRuleCommand command) {
        return reconciliationRuleService.updateReconciliationRule(id, command);
    }

    public void deleteReconciliationRule(Long id) {
        reconciliationRuleService.deleteReconciliationRule(id);
    }

    public DifferenceReasonCode createDifferenceReasonCode(DifferenceReasonCodeCommand command) {
        return reconciliationRuleService.createDifferenceReasonCode(command);
    }

    @Transactional(readOnly = true)
    public List<DifferenceReasonCode> findAllDifferenceReasonCodes() {
        return reconciliationRuleService.findAllDifferenceReasonCodes();
    }

    @Transactional(readOnly = true)
    public DifferenceReasonCode findDifferenceReasonCodeById(Long id) {
        return reconciliationRuleService.findDifferenceReasonCodeById(id);
    }

    public DifferenceReasonCode updateDifferenceReasonCode(Long id, DifferenceReasonCodeCommand command) {
        return reconciliationRuleService.updateDifferenceReasonCode(id, command);
    }

    public void deleteDifferenceReasonCode(Long id) {
        reconciliationRuleService.deleteDifferenceReasonCode(id);
    }

    // --- Reconciliation Execution & Difference Delegation ---

    public ReconciliationDifference assignDifference(AssignDifferenceCommand command) {
        return reconciliationExecutionService.assignDifference(command);
    }

    public ReconciliationDifference resolveDifference(ResolveDifferenceCommand command) {
        return reconciliationExecutionService.resolveDifference(command);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationDifference> findDifferencesByReconciliationRunId(Long reconciliationRunId) {
        return reconciliationExecutionService.findDifferencesByReconciliationRunId(reconciliationRunId);
    }

    public ReconciliationRun performReconciliation(RunReconciliationCommand command) {
        return reconciliationExecutionService.performReconciliation(command);
    }
}
