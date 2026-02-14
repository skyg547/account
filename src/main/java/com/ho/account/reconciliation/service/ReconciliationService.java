package com.ho.account.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.journal.repository.JournalEntryRepository;
import java.math.BigDecimal;
import com.ho.account.reconciliation.domain.ReconciliationRun.ReconciliationRunStatus; // Added import
import java.util.Collections;

/**
 * 대사(Reconciliation) 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * 대사 단위, 규칙, 차이 사유 코드 관리 및 실제 대사 실행 로직을 포함합니다.
 */
@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationUnitRepository reconciliationUnitRepository;
    private final ReconciliationRuleRepository reconciliationRuleRepository;
    private final DifferenceReasonCodeRepository differenceReasonCodeRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationDifferenceRepository reconciliationDifferenceRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final ObjectMapper objectMapper; // JSON 파싱을 위한 ObjectMapper

    @Autowired
    public ReconciliationService(ReconciliationUnitRepository reconciliationUnitRepository,
                                 ReconciliationRuleRepository reconciliationRuleRepository,
                                 DifferenceReasonCodeRepository differenceReasonCodeRepository,
                                 ReconciliationRunRepository reconciliationRunRepository,
                                 ReconciliationDifferenceRepository reconciliationDifferenceRepository,
                                 JournalEntryRepository journalEntryRepository,
                                 JournalDetailRepository journalDetailRepository,
                                 AccountSubjectRepository accountSubjectRepository,
                                 ObjectMapper objectMapper) {
        this.reconciliationUnitRepository = reconciliationUnitRepository;
        this.reconciliationRuleRepository = reconciliationRuleRepository;
        this.differenceReasonCodeRepository = differenceReasonCodeRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.reconciliationDifferenceRepository = reconciliationDifferenceRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalDetailRepository = journalDetailRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.objectMapper = objectMapper;
    }

    // --- ReconciliationUnit (대사 단위) 관련 메서드 ---

    /**
     * 새로운 대사 단위를 생성합니다.
     * @param reconciliationUnit 생성할 대사 단위 엔티티
     * @return 생성된 대사 단위
     */
    public ReconciliationUnit createReconciliationUnit(ReconciliationUnit reconciliationUnit) {
        // 중복 이름 검증 등 추가 로직 필요 시 구현
        return reconciliationUnitRepository.save(reconciliationUnit);
    }

    /**
     * 모든 대사 단위를 조회합니다.
     * @return 모든 대사 단위 목록
     */
    @Transactional(readOnly = true)
    public List<ReconciliationUnit> findAllReconciliationUnits() {
        return reconciliationUnitRepository.findAll();
    }

    /**
     * ID로 대사 단위를 조회합니다.
     * @param id 대사 단위 ID
     * @return 조회된 대사 단위
     * @throws EntityNotFoundException 해당 ID의 대사 단위가 없을 경우
     */
    @Transactional(readOnly = true)
    public ReconciliationUnit findReconciliationUnitById(Long id) {
        return reconciliationUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + id));
    }

    /**
     * 대사 단위를 업데이트합니다.
     * @param id 업데이트할 대사 단위의 ID
     * @param updatedUnit 업데이트할 내용을 담은 대사 단위 엔티티
     * @return 업데이트된 대사 단위
     * @throws EntityNotFoundException 해당 ID의 대사 단위가 없을 경우
     */
    public ReconciliationUnit updateReconciliationUnit(Long id, ReconciliationUnit updatedUnit) {
        ReconciliationUnit existingUnit = reconciliationUnitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + id));

        existingUnit.setName(updatedUnit.getName());
        existingUnit.setDescription(updatedUnit.getDescription());
        existingUnit.setFrequency(updatedUnit.getFrequency());
        existingUnit.setReconciliationType(updatedUnit.getReconciliationType());
        existingUnit.setCriteriaJson(updatedUnit.getCriteriaJson());
        existingUnit.setActive(updatedUnit.isActive());
        // auditUser 및 updatedAt은 @PreUpdate에서 처리됨
        return reconciliationUnitRepository.save(existingUnit);
    }

    /**
     * 대사 단위를 삭제합니다. (연관된 규칙, 실행 결과, 차이도 함께 처리 필요)
     * @param id 삭제할 대사 단위의 ID
     */
    public void deleteReconciliationUnit(Long id) {
        // TODO: 연관된 ReconciliationRule, ReconciliationRun, ReconciliationDifference 처리 로직 추가 필요
        reconciliationUnitRepository.deleteById(id);
    }

    // --- ReconciliationRule (대사 규칙) 관련 메서드 ---

    /**
     * 새로운 대사 규칙을 생성합니다.
     * @param reconciliationRule 생성할 대사 규칙 엔티티
     * @return 생성된 대사 규칙
     */
    public ReconciliationRule createReconciliationRule(ReconciliationRule reconciliationRule) {
        // 중복 이름, 유효성 검증 등 추가 로직 필요 시 구현
        return reconciliationRuleRepository.save(reconciliationRule);
    }

    /**
     * 특정 대사 단위에 속한 모든 대사 규칙을 조회합니다.
     * @param unitId 대사 단위 ID
     * @return 대사 규칙 목록
     */
    @Transactional(readOnly = true)
    public List<ReconciliationRule> findRulesByReconciliationUnit(Long unitId) {
        ReconciliationUnit unit = reconciliationUnitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationUnit not found with id: " + unitId));
        return reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(unit);
    }
    // Note: findByReconciliationUnit method will need to be added to ReconciliationRuleRepository

    /**
     * 대사 규칙을 업데이트합니다.
     * @param id 업데이트할 대사 규칙의 ID
     * @param updatedRule 업데이트할 내용을 담은 대사 규칙 엔티티
     * @return 업데이트된 대사 규칙
     * @throws EntityNotFoundException 해당 ID의 대사 규칙이 없을 경우
     */
    public ReconciliationRule updateReconciliationRule(Long id, ReconciliationRule updatedRule) {
        ReconciliationRule existingRule = reconciliationRuleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRule not found with id: " + id));

        existingRule.setName(updatedRule.getName());
        existingRule.setRuleDefinitionJson(updatedRule.getRuleDefinitionJson());
        existingRule.setToleranceType(updatedRule.getToleranceType());
        existingRule.setToleranceValue(updatedRule.getToleranceValue());
        existingRule.setPriority(updatedRule.getPriority());
        existingRule.setActive(updatedRule.isActive());
        existingRule.setReconciliationUnit(updatedRule.getReconciliationUnit()); // FK 변경 가능성
        return reconciliationRuleRepository.save(existingRule);
    }

    /**
     * 대사 규칙을 삭제합니다.
     * @param id 삭제할 대사 규칙의 ID
     */
    public void deleteReconciliationRule(Long id) {
        reconciliationRuleRepository.deleteById(id);
    }

    // --- DifferenceReasonCode (차이 사유 코드) 관련 메서드 ---

    /**
     * 새로운 차이 사유 코드를 생성합니다.
     * @param reasonCode 생성할 차이 사유 코드 엔티티
     * @return 생성된 차이 사유 코드
     */
    public DifferenceReasonCode createDifferenceReasonCode(DifferenceReasonCode reasonCode) {
        // 중복 코드/이름 검증 등 추가 로직 필요 시 구현
        return differenceReasonCodeRepository.save(reasonCode);
    }

    /**
     * 모든 차이 사유 코드를 조회합니다.
     * @return 모든 차이 사유 코드 목록
     */
    @Transactional(readOnly = true)
    public List<DifferenceReasonCode> findAllDifferenceReasonCodes() {
        return differenceReasonCodeRepository.findAll();
    }

    /**
     * ID로 차이 사유 코드를 조회합니다.
     * @param id 차이 사유 코드 ID
     * @return 조회된 차이 사유 코드
     * @throws EntityNotFoundException 해당 ID의 차이 사유 코드가 없을 경우
     */
    @Transactional(readOnly = true)
    public DifferenceReasonCode findDifferenceReasonCodeById(Long id) {
        return differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));
    }

    /**
     * 차이 사유 코드를 업데이트합니다.
     * @param id 업데이트할 차이 사유 코드의 ID
     * @param updatedReasonCode 업데이트할 내용을 담은 차이 사유 코드 엔티티
     * @return 업데이트된 차이 사유 코드
     * @throws EntityNotFoundException 해당 ID의 차이 사유 코드가 없을 경우
     */
    public DifferenceReasonCode updateDifferenceReasonCode(Long id, DifferenceReasonCode updatedReasonCode) {
        DifferenceReasonCode existingCode = differenceReasonCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DifferenceReasonCode not found with id: " + id));

        existingCode.setCode(updatedReasonCode.getCode());
        existingCode.setName(updatedReasonCode.getName());
        existingCode.setDescription(updatedReasonCode.getDescription());
        existingCode.setAdjustable(updatedReasonCode.isAdjustable());
        existingCode.setActive(updatedReasonCode.isActive());
        return differenceReasonCodeRepository.save(existingCode);
    }

    /**
     * 차이 사유 코드를 삭제합니다.
     * @param id 삭제할 차이 사유 코드의 ID
     */
    public void deleteDifferenceReasonCode(Long id) {
        // TODO: 연관된 ReconciliationDifference 처리 로직 추가 필요 (FK 제약 조건)
        differenceReasonCodeRepository.deleteById(id);
    }

    /**
     * 대사 차이를 특정 사용자에게 할당하고 SLA 기한을 설정합니다.
     * @param differenceId 할당할 대사 차이의 ID
     * @param assignedToUser 할당받을 사용자 ID 또는 이름
     * @param slaDueDate SLA 기한
     * @return 업데이트된 ReconciliationDifference
     * @throws EntityNotFoundException 해당 ID의 대사 차이가 없을 경우
     */
    public ReconciliationDifference assignDifference(Long differenceId, String assignedToUser, LocalDateTime slaDueDate) {
        ReconciliationDifference difference = reconciliationDifferenceRepository.findById(differenceId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationDifference not found with id: " + differenceId));

        difference.setAssignedToUser(assignedToUser);
        difference.setSlaDueDate(slaDueDate);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED); // 상태를 ASSIGNED로 변경
        return reconciliationDifferenceRepository.save(difference);
    }

    /**
     * 특정 대사 실행에 해당하는 모든 대사 차이를 조회합니다.
     * @param reconciliationRunId 대사 실행 ID
     * @return 대사 차이 목록
     */
    @Transactional(readOnly = true)
    public List<ReconciliationDifference> findDifferencesByReconciliationRunId(Long reconciliationRunId) {
        ReconciliationRun run = reconciliationRunRepository.findById(reconciliationRunId)
                .orElseThrow(() -> new EntityNotFoundException("ReconciliationRun not found with id: " + reconciliationRunId));
        return reconciliationDifferenceRepository.findByReconciliationRun(run);
    }

    // --- Core Reconciliation Logic (대사 실행 로직) ---

    /**
     * 특정 대사 단위를 기반으로 대사를 실행합니다.
     * 이 메서드는 대사 로직의 진입점이며, 실제 매칭 및 차이 식별 로직을 호출합니다.
     * @param unitId 대사를 실행할 대사 단위의 ID
     * @param reconciliationDate 대사 기준일
     * @return 생성된 대사 실행 결과 (ReconciliationRun)
     * @throws EntityNotFoundException 대사 단위를 찾을 수 없는 경우
     */
    public ReconciliationRun performReconciliation(Long unitId, LocalDate reconciliationDate) {
        ReconciliationUnit reconciliationUnit = findReconciliationUnitById(unitId);
        List<ReconciliationRule> rules = reconciliationRuleRepository.findByReconciliationUnitOrderByPriorityAsc(reconciliationUnit);

        // ReconciliationRun 시작 기록
        ReconciliationRun run = new ReconciliationRun();
        run.setReconciliationUnit(reconciliationUnit);
        run.setReconciliationDate(reconciliationDate);
        run.setRunStartTime(LocalDateTime.now());
        run.setStatus(ReconciliationRunStatus.RUNNING);
        run.setRunBy("SYSTEM"); // 또는 현재 로그인 사용자 정보
        run = reconciliationRunRepository.save(run);

        try {
            // Declare local variables for reconciliation results
            BigDecimal sourceAmount = BigDecimal.ZERO;
            BigDecimal targetAmount = BigDecimal.ZERO;
            int sourceCount = 0;
            int targetCount = 0;
            BigDecimal unmatchedAmount = BigDecimal.ZERO;
            int unmatchedCount = 0;
            BigDecimal matchedAmount = BigDecimal.ZERO;
            int matchedCount = 0;

            // 3. 매칭된 항목/금액, 미매칭된 항목/금액 계산 (임시 로직)
            // 실제 구현에서는 각 rule의 ruleDefinitionJson을 파싱하여 복잡한 매칭 로직 수행
            // 이 예시에서는 모든 규칙을 적용하여 최종 차이를 계산한다고 가정

            // TODO: Replace with actual logic to fetch source/target data based on reconciliationUnit and rules.
            // For now, using dummy values for compilation and basic flow.
            sourceAmount = new BigDecimal("1000.00"); // Dummy value
            targetAmount = new BigDecimal("950.00");  // Dummy value
            sourceCount = 10;
            targetCount = 9;


            // 임시 매칭 로직: 단순 금액 불일치 발생 시 차이 생성
            if (sourceAmount.compareTo(targetAmount) != 0) {
                // 차이 발생
                unmatchedAmount = sourceAmount.subtract(targetAmount).abs();
                unmatchedCount = sourceCount - targetCount; // 간단한 예시로 차이 개수 설정
                matchedAmount = sourceAmount.min(targetAmount);
                matchedCount = Math.min(sourceCount, targetCount);

                ReconciliationDifference diff = new ReconciliationDifference();
                diff.setReconciliationRun(run);
                diff.setDifferenceType(ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH);
                diff.setAmountExpected(sourceAmount);
                diff.setAmountActual(targetAmount);
                diff.setDifferenceAmount(unmatchedAmount);
                diff.setDescription(reconciliationUnit.getName() + " - 금액 불일치 발생 (기준일: " + reconciliationDate + ")");
                // sourceItemRef와 targetItemRef는 실제 데이터를 반영하도록 변경 필요
                diff.setSourceItemRef("{\"type\":\"SUMMARY\",\"date\":\"" + reconciliationDate + "\",\"unit\":\"" + reconciliationUnit.getName() + "\"}");
                diff.setTargetItemRef("{\"type\":\"SUMMARY\",\"date\":\"" + reconciliationDate + "\",\"unit\":\"" + reconciliationUnit.getName() + "\"}");

                // DoD: 차이는 "원인코드+조정전표 링크"로 반드시 수렴
                // 기본 차이 사유 코드를 조회하거나, 규칙 기반으로 특정 사유 코드 할당
                DifferenceReasonCode defaultReason = differenceReasonCodeRepository.findByCode("GENERIC_MISMATCH")
                        .orElseGet(() -> differenceReasonCodeRepository.save(createDefaultReasonCode())); // 기본 사유 코드 없으면 생성 후 저장

                diff.setReasonCode(defaultReason);
                diff.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING); // 초기 상태

                // 조정 전표 생성 로직 (isAdjustable이 true인 경우)
                if (defaultReason.isAdjustable()) {
                    // 차변/대변 계정과목은 대사 단위의 유형이나 시스템 설정에 따라 달라짐
                    // 여기서는 임시로 특정 계정과목 사용
                    AccountSubject debitAccount = accountSubjectRepository.findById("121000") // 예: 미결제 계정 (임시)
                            .orElseThrow(() -> new EntityNotFoundException("Debit AccountSubject (121000) not found. Please create it."));
                    AccountSubject creditAccount = accountSubjectRepository.findById("999999") // 예: 대사차이 조정 계정 (임시)
                            .orElseThrow(() -> new EntityNotFoundException("Credit AccountSubject (999999) not found. Please create it."));

                    JournalEntry adjustmentEntry = createAdjustmentJournalEntry(
                            reconciliationDate,
                            unmatchedAmount,
                            reconciliationUnit.getName() + " 대사 차이 조정 (" + defaultReason.getName() + ")",
                            debitAccount,
                            creditAccount,
                            "SYSTEM"
                    );
                    diff.setAdjustmentJournalEntry(adjustmentEntry); // 조정 전표 링크
                }
                reconciliationDifferenceRepository.save(diff);
            }

            // run 객체 업데이트
            run.setTotalItemsSource((long)sourceCount);
            run.setTotalAmountSource(sourceAmount);
            run.setTotalItemsTarget((long)targetCount);
            run.setTotalAmountTarget(targetAmount);
            run.setMatchedItemsCount((long)matchedCount);
            run.setMatchedAmount(matchedAmount);
            run.setUnmatchedItemsCount((long)unmatchedCount);
            run.setUnmatchedAmount(unmatchedAmount);
            run.setStatus(ReconciliationRunStatus.SUCCESS);


        } catch (Exception e) {
            run.setStatus(ReconciliationRunStatus.FAILED);
            // TODO: 에러 로깅
            throw new RuntimeException("Reconciliation failed for unit " + unitId, e);
        } finally {
            run.setRunEndTime(LocalDateTime.now());
            reconciliationRunRepository.save(run);
        }

        return run;
    }

    /**
     * 조정 전표를 생성하고 저장하는 헬퍼 메서드.
     * @param accountingDate 회계일자
     * @param amount 금액
     * @param description 적요
     * @param debitAccount 차변 계정과목
     * @param creditAccount 대변 계정과목
     * @param createdBy 생성자
     * @return 생성된 JournalEntry
     */
    private JournalEntry createAdjustmentJournalEntry(LocalDate accountingDate, BigDecimal amount, String description,
                                                      AccountSubject debitAccount, AccountSubject creditAccount, String createdBy) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        entry.setDescription(description);
        entry.setStatus(JournalEntryStatus.DRAFT); // 조정 전표는 DRAFT 상태로 생성 후 승인 프로세스를 거칠 수 있음
        entry.setEntryType("ADJUSTMENT");
        entry.setCreatedBy(createdBy);
        entry.setAuditUser(createdBy);
        entry.setLineageSourceType("RECONCILIATION");
        entry.setLineageSourceId("RECON_ADJ-" + System.currentTimeMillis()); // 고유한 ID 생성

        // JournalDetail - 차변
        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAccountSubject(debitAccount);
        debitDetail.setAmount(amount);
        debitDetail.setBaseAmount(amount); // 기준 통화 금액도 동일하다고 가정
        debitDetail.setDetailDescription(description + " (차변)");
        entry.addDetail(debitDetail);

        // JournalDetail - 대변
        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setDrcrType("CREDIT");
        creditDetail.setAccountSubject(creditAccount);
        creditDetail.setAmount(amount);
        creditDetail.setBaseAmount(amount); // 기준 통화 금액도 동일하다고 가정
        creditDetail.setDetailDescription(description + " (대변)");
        entry.addDetail(creditDetail);

        // 전표번호 생성 (예시)
        entry.setSlipNo(accountingDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-ADJ-" + journalEntryRepository.count());

        return journalEntryRepository.save(entry);
    }

    // 기본 차이 사유 코드를 생성하는 헬퍼 메서드 (초기 데이터 로딩 시 사용 가능)
    private DifferenceReasonCode createDefaultReasonCode() {
        DifferenceReasonCode defaultReason = new DifferenceReasonCode();
        defaultReason.setCode("GENERIC_MISMATCH");
        defaultReason.setName("일반 불일치");
        defaultReason.setDescription("자동 매칭되지 않은 일반적인 불일치");
        defaultReason.setAdjustable(true); // 기본적으로 조정 가능하도록 설정
        defaultReason.setActive(true);
        return defaultReason;
    }
}
