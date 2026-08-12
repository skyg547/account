package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.application.port.in.DifferenceReasonCodeCommand;
import com.ho.account.reconciliation.application.port.in.ReconciliationRuleCommand;
import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.DifferenceReasonCodeRepository;
import com.ho.account.reconciliation.repository.ReconciliationRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [단일 책임 원칙 (SRP) - 대사 규칙 및 사유 코드 관리 서비스]
 * 대사 실행 시 적용할 대사 규칙({@link ReconciliationRule}) 및 대사 차액의 원인을 분류하는 사유 코드({@link DifferenceReasonCode}) 관리를 전담합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 대사 규칙 및 사유 코드에 대한 '정책 관리자'입니다.
 * 대사 단위별로 적용할 조건(허용 오차, 우선순위 등)을 정의하는 대사 규칙(Rule)과
 * 발생한 금액 차이의 원인을 기록하는 차액 사유 코드(Reason Code)의 생명주기를 전담하여 관리합니다.
 * 
 * [설계 특징 및 이점]
 * 1. **관련 도메인 규칙의 응집력 향상**: 대사 기준/규칙 및 사유 코드 설정이라는 연관된 관리 책임을 집중시킵니다.
 * 2. **이력 보존을 위한 Soft Delete**: 대사 규칙 및 사유 코드 삭제 시 물리 삭제를 수행하면
 *    과거에 실행된 대사 결과(Run/Difference)의 판정 기준 추적에 문제가 발생하므로, 비활성화(active = false) 처리하여 추적성을 보장합니다.
 */
@Service
@Transactional
public class ReconciliationRuleService {

    private final ReconciliationRuleRepository reconciliationRuleRepository;
    private final DifferenceReasonCodeRepository differenceReasonCodeRepository;
    private final ReconciliationUnitService reconciliationUnitService;

    public ReconciliationRuleService(ReconciliationRuleRepository reconciliationRuleRepository,
                                     DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                     ReconciliationUnitService reconciliationUnitService) {
        this.reconciliationRuleRepository = reconciliationRuleRepository;
        this.differenceReasonCodeRepository = differenceReasonCodeRepository;
        this.reconciliationUnitService = reconciliationUnitService;
    }

    // --- ReconciliationRule methods ---

    /**
     * 새로운 대사 규칙을 생성하고 저장합니다.
     *
     * @param command 대사 규칙 생성을 위한 커맨드 객체
     * @return 저장된 대사 규칙 엔티티
     */
    public ReconciliationRule createReconciliationRule(ReconciliationRuleCommand command) {
        ReconciliationRule reconciliationRule = toReconciliationRule(command);
        return reconciliationRuleRepository.save(reconciliationRule);
    }

    /**
     * 특정 대사 단위에 속한 대사 규칙 목록을 우선순위(Priority 오름차순) 순으로 조회합니다.
     *
     * @param unitId 대사 단위 ID
     * @return 우선순위 순 정렬된 대사 규칙 목록
     */
    @Transactional(readOnly = true)
    public List<ReconciliationRule> findRulesByReconciliationUnit(Long unitId) {
        ReconciliationUnit unit = reconciliationUnitService.findReconciliationUnitById(unitId);
        return reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit);
    }

    /**
     * 기존 대사 규칙의 정보를 수정합니다.
     *
     * @param id 수정할 대사 규칙 ID
     * @param command 대사 규칙 수정 커맨드
     * @return 수정 및 저장된 대사 규칙 엔티티
     */
    public ReconciliationRule updateReconciliationRule(Long id, ReconciliationRuleCommand command) {
        ReconciliationRule existingRule = reconciliationRuleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRule not found with id: " + id));
        ReconciliationUnit reconciliationUnit = reconciliationUnitService.findReconciliationUnitById(command.reconciliationUnitId());

        existingRule.setName(command.name());
        existingRule.setRuleDefinitionJson(command.ruleDefinitionJson());
        existingRule.setToleranceType(command.toleranceType());
        existingRule.setToleranceValue(command.toleranceValue());
        existingRule.setPriority(command.priority());
        existingRule.setActive(command.active());
        existingRule.setReconciliationUnit(reconciliationUnit);
        return reconciliationRuleRepository.save(existingRule);
    }

    /**
     * 대사 규칙을 비활성화(Soft Delete) 처리합니다.
     * 과거 대사 실행 이력이 어떤 규칙으로 판단됐는지 추적할 수 있도록 물리 삭제하지 않습니다.
     *
     * @param id 비활성화할 대사 규칙 ID
     */
    public void deleteReconciliationRule(Long id) {
        ReconciliationRule existingRule = reconciliationRuleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRule not found with id: " + id));
        existingRule.setActive(false);
        reconciliationRuleRepository.save(existingRule);
    }

    private ReconciliationRule toReconciliationRule(ReconciliationRuleCommand command) {
        ReconciliationUnit reconciliationUnit = reconciliationUnitService.findReconciliationUnitById(command.reconciliationUnitId());
        ReconciliationRule rule = new ReconciliationRule();
        rule.setReconciliationUnit(reconciliationUnit);
        rule.setName(command.name());
        rule.setRuleDefinitionJson(command.ruleDefinitionJson());
        rule.setToleranceType(command.toleranceType());
        rule.setToleranceValue(command.toleranceValue());
        rule.setPriority(command.priority());
        rule.setActive(command.active());
        return rule;
    }

    // --- DifferenceReasonCode methods ---

    /**
     * 새로운 차액 사유 코드를 생성합니다.
     *
     * @param command 차액 사유 코드 생성 커맨드
     * @return 생성된 차액 사유 코드 엔티티
     */
    public DifferenceReasonCode createDifferenceReasonCode(DifferenceReasonCodeCommand command) {
        DifferenceReasonCode reasonCode = toDifferenceReasonCode(command);
        return differenceReasonCodeRepository.save(reasonCode);
    }

    /**
     * 등록된 모든 차액 사유 코드를 조회합니다.
     *
     * @return 차액 사유 코드 전체 목록
     */
    @Transactional(readOnly = true)
    public List<DifferenceReasonCode> findAllDifferenceReasonCodes() {
        return differenceReasonCodeRepository.findAll();
    }

    /**
     * ID로 차액 사유 코드를 단건 조회합니다.
     *
     * @param id 차액 사유 코드 ID
     * @return 조회된 차액 사유 코드 엔티티
     * @throws EntityNotFoundException 해당 ID의 차액 사유 코드가 존재하지 않는 경우 발생
     */
    @Transactional(readOnly = true)
    public DifferenceReasonCode findDifferenceReasonCodeById(Long id) {
        return differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));
    }

    /**
     * 차액 사유 코드 정보를 수정합니다.
     *
     * @param id 수정할 차액 사유 코드 ID
     * @param command 수정 속성을 담은 커맨드
     * @return 수정 및 저장된 차액 사유 코드 엔티티
     */
    public DifferenceReasonCode updateDifferenceReasonCode(Long id, DifferenceReasonCodeCommand command) {
        DifferenceReasonCode existingCode = findDifferenceReasonCodeById(id);

        existingCode.setCode(command.code());
        existingCode.setName(command.name());
        existingCode.setDescription(command.description());
        existingCode.setAdjustable(command.adjustable());
        existingCode.setActive(command.active());
        return differenceReasonCodeRepository.save(existingCode);
    }

    /**
     * 차액 사유 코드를 비활성화(Soft Delete) 처리합니다.
     * 과거 차이 내역에서 참조하는 사유 코드는 유지하고 신규 사용만 차단합니다.
     *
     * @param id 비활성화할 차액 사유 코드 ID
     */
    public void deleteDifferenceReasonCode(Long id) {
        DifferenceReasonCode reasonCode = findDifferenceReasonCodeById(id);
        reasonCode.setActive(false);
        differenceReasonCodeRepository.save(reasonCode);
    }

    private DifferenceReasonCode toDifferenceReasonCode(DifferenceReasonCodeCommand command) {
        DifferenceReasonCode reasonCode = new DifferenceReasonCode();
        reasonCode.setCode(command.code());
        reasonCode.setName(command.name());
        reasonCode.setDescription(command.description());
        reasonCode.setAdjustable(command.adjustable());
        reasonCode.setActive(command.active());
        return reasonCode;
    }
}
