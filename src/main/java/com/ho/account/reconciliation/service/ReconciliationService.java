package com.ho.account.reconciliation.service;

import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.dto.ReconciliationRequestDto;
import com.ho.account.reconciliation.dto.ReconciliationResponseDto;
import com.ho.account.reconciliation.dto.VarianceDto;
import com.ho.account.reconciliation.repository.ReconciliationResultRepository;
import com.ho.account.reconciliation.repository.ReconciliationVarianceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationResultRepository reconciliationResultRepository;
    private final ReconciliationVarianceRepository reconciliationVarianceRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final com.ho.account.reconciliation.repository.BankStatementRepository bankStatementRepository;
    private final com.ho.account.journal.repository.JournalDetailRepository journalDetailRepository;
    private final AutomatedMatchingEngine matchingEngine;

    @Autowired
    public ReconciliationService(ReconciliationResultRepository reconciliationResultRepository,
            ReconciliationVarianceRepository reconciliationVarianceRepository,
            JournalEntryRepository journalEntryRepository,
            com.ho.account.reconciliation.repository.BankStatementRepository bankStatementRepository,
            com.ho.account.journal.repository.JournalDetailRepository journalDetailRepository,
            AutomatedMatchingEngine matchingEngine) {
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.reconciliationVarianceRepository = reconciliationVarianceRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.bankStatementRepository = bankStatementRepository;
        this.journalDetailRepository = journalDetailRepository;
        this.matchingEngine = matchingEngine;
    }

    // ===== 대사 실행 =====

    /**
     * 범용 대사 실행 (ReconciliationRequestDto 기반)
     */
    public ReconciliationResponseDto executeReconciliation(ReconciliationRequestDto request) {
        switch (request.getReconciliationType()) {
            case SOURCE_STANDARD:
                return performSourceStandardReconciliation(request.getReconciliationDate(), request.getRunBy());
            case ACCOUNT_TOTALS:
                return performAccountTotalsReconciliation(request.getReconciliationDate(), request.getRunBy());
            case BANK_ACCOUNT:
                return performBankAccountReconciliation(request.getReconciliationDate(), request.getRunBy());
            default:
                throw new IllegalArgumentException("지원하지 않는 대사 유형: " + request.getReconciliationType());
        }
    }

    /**
     * 원천-표준-전표-원장 대사 (건수/금액 비교)
     */
    public ReconciliationResponseDto performSourceStandardReconciliation(LocalDate reconciliationDate, String runBy) {
        ReconciliationResult result = createBaseResult(reconciliationDate, ReconciliationType.SOURCE_STANDARD, runBy);

        // 원천 데이터 건수/금액 집계 (실제로는 원천 시스템 조회)
        long sourceCount = countSourceTransactions(reconciliationDate);
        BigDecimal sourceAmount = sumSourceTransactionAmounts(reconciliationDate);

        // 대상 데이터 건수/금액 집계 (전표/원장 조회)
        long targetCount = countJournalEntries(reconciliationDate);
        BigDecimal targetAmount = sumJournalEntryAmounts(reconciliationDate);

        result.setTotalCountSource(sourceCount);
        result.setTotalAmountSource(sourceAmount);
        result.setTotalCountTarget(targetCount);
        result.setTotalAmountTarget(targetAmount);

        long varCount = Math.abs(sourceCount - targetCount);
        BigDecimal varAmount = sourceAmount.subtract(targetAmount).abs();
        result.setVarianceCount(varCount);
        result.setVarianceAmount(varAmount);

        if (varCount == 0 && varAmount.compareTo(BigDecimal.ZERO) == 0) {
            result.setStatus(ReconciliationStatus.SUCCESS);
        } else {
            result.setStatus(ReconciliationStatus.VARIANCE_FOUND);
        }

        ReconciliationResult saved = reconciliationResultRepository.save(result);

        // 차이 발생 시 차이 내역 생성
        if (saved.getStatus() == ReconciliationStatus.VARIANCE_FOUND) {
            createVarianceEntries(saved, sourceCount, targetCount, sourceAmount, targetAmount);
        }

        return toResponseDto(saved);
    }

    /**
     * 계정합계 대사
     */
    public ReconciliationResponseDto performAccountTotalsReconciliation(LocalDate reconciliationDate, String runBy) {
        ReconciliationResult result = createBaseResult(reconciliationDate, ReconciliationType.ACCOUNT_TOTALS, runBy);

        // 전표 차변 합계
        BigDecimal totalDebit = sumJournalDebitAmounts(reconciliationDate);
        // 전표 대변 합계
        BigDecimal totalCredit = sumJournalCreditAmounts(reconciliationDate);

        long entryCount = countJournalEntries(reconciliationDate);

        result.setTotalCountSource(entryCount);
        result.setTotalAmountSource(totalDebit);
        result.setTotalCountTarget(entryCount);
        result.setTotalAmountTarget(totalCredit);

        BigDecimal difference = totalDebit.subtract(totalCredit).abs();
        result.setVarianceCount(difference.compareTo(BigDecimal.ZERO) != 0 ? 1L : 0L);
        result.setVarianceAmount(difference);

        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            result.setStatus(ReconciliationStatus.SUCCESS);
        } else {
            result.setStatus(ReconciliationStatus.VARIANCE_FOUND);
        }

        ReconciliationResult saved = reconciliationResultRepository.save(result);

        if (saved.getStatus() == ReconciliationStatus.VARIANCE_FOUND) {
            ReconciliationVariance variance = new ReconciliationVariance();
            variance.setReconciliationResult(saved);
            variance.setVarianceCode("DEBIT_CREDIT_MISMATCH");
            variance.setDescription("차변 합계(" + totalDebit + ")와 대변 합계(" + totalCredit + ") 불일치");
            variance.setAmount(difference);
            variance.setDrCrType(totalDebit.compareTo(totalCredit) > 0 ? "DR" : "CR");
            variance.setStatus(VarianceStatus.OPEN);
            reconciliationVarianceRepository.save(variance);
        }

        return toResponseDto(saved);
    }

    /**
     * 은행계좌 대사 (통장 vs 장부)
     */
    public ReconciliationResponseDto performBankAccountReconciliation(LocalDate reconciliationDate, String runBy) {
        ReconciliationResult result = createBaseResult(reconciliationDate, ReconciliationType.BANK_ACCOUNT, runBy);

        // 1. 데이터 추출
        List<BankStatement> statements = bankStatementRepository.findAll().stream()
                .filter(bs -> bs.getTransactionDate().equals(reconciliationDate))
                .collect(Collectors.toList());

        List<com.ho.account.journal.domain.JournalDetail> details = journalDetailRepository
                .findByAccountAndDateRangeForIS(reconciliationDate, reconciliationDate).stream()
                .filter(jd -> jd.getAccountSubject().getCode().startsWith("103")) // 은행계정
                .collect(Collectors.toList());

        // 2. 자동 매칭 수행
        List<AutomatedMatchingEngine.MatchResult> matchResults = matchingEngine.match(statements, details);

        // 3. 결과 집계
        long sourceCount = statements.size();
        BigDecimal sourceAmount = statements.stream()
                .map(s -> s.getDepositAmount().add(s.getWithdrawalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long targetCount = details.size();
        BigDecimal targetAmount = details.stream()
                .map(com.ho.account.journal.domain.JournalDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.setTotalCountSource(sourceCount);
        result.setTotalAmountSource(sourceAmount);
        result.setTotalCountTarget(targetCount);
        result.setTotalAmountTarget(targetAmount);

        // 4. 차이 식별 및 로그 생성
        long unmatchedCount = matchResults.stream().filter(r -> !r.isMatch()).count();
        BigDecimal unmatchedAmount = matchResults.stream()
                .filter(r -> !r.isMatch())
                .map(r -> r.getBankStatement().getDepositAmount().add(r.getBankStatement().getWithdrawalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.setVarianceCount(unmatchedCount);
        result.setVarianceAmount(unmatchedAmount);

        if (unmatchedCount == 0) {
            result.setStatus(ReconciliationStatus.SUCCESS);
        } else {
            result.setStatus(ReconciliationStatus.VARIANCE_FOUND);
        }

        ReconciliationResult saved = reconciliationResultRepository.save(result);

        // 차이 내역 상세 저장
        matchResults.stream().filter(r -> !r.isMatch()).forEach(r -> {
            ReconciliationVariance v = new ReconciliationVariance();
            v.setReconciliationResult(saved);
            v.setVarianceCode("UNMATCHED_STATEMENT");
            v.setDescription("매칭되는 장부 내역 없음: " + r.getBankStatement().getDescription());
            v.setAmount(r.getBankStatement().getDepositAmount().add(r.getBankStatement().getWithdrawalAmount()));
            v.setSourceReference(r.getBankStatement().getId().toString());
            v.setStatus(VarianceStatus.OPEN);
            reconciliationVarianceRepository.save(v);
        });

        return toResponseDto(saved);
    }

    // ===== 조회 =====

    @Transactional(readOnly = true)
    public List<ReconciliationResponseDto> getAllReconciliationResults() {
        return reconciliationResultRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReconciliationResponseDto getReconciliationResultById(Long id) {
        ReconciliationResult result = reconciliationResultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대사 결과를 찾을 수 없습니다: " + id));
        ReconciliationResponseDto dto = toResponseDto(result);
        List<VarianceDto> variances = reconciliationVarianceRepository.findByReconciliationResultId(id).stream()
                .map(VarianceDto::from)
                .collect(Collectors.toList());
        dto.setVariances(variances);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResponseDto> getReconciliationResultsByType(ReconciliationType type) {
        return reconciliationResultRepository.findByReconciliationType(type).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResponseDto> getReconciliationResultsByDate(LocalDate date) {
        return reconciliationResultRepository.findByReconciliationDate(date).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VarianceDto> getVariancesByReconciliationResult(Long reconciliationResultId) {
        return reconciliationVarianceRepository.findByReconciliationResultId(reconciliationResultId).stream()
                .map(VarianceDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VarianceDto> getOpenVariances() {
        return reconciliationVarianceRepository.findByStatus(VarianceStatus.OPEN).stream()
                .map(VarianceDto::from)
                .collect(Collectors.toList());
    }

    // ===== 차이 해소 =====

    /**
     * 조정 전표를 통한 차이 해소
     */
    public VarianceDto resolveVarianceWithAdjustment(Long varianceId, Long journalEntryId, String resolvedBy) {
        ReconciliationVariance variance = reconciliationVarianceRepository.findById(varianceId)
                .orElseThrow(() -> new IllegalArgumentException("대사 차이를 찾을 수 없습니다: " + varianceId));

        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다: " + journalEntryId));

        variance.setAdjustmentJournalEntry(journalEntry);
        variance.setStatus(VarianceStatus.ADJUSTED);
        variance.setResolvedBy(resolvedBy);
        variance.setResolvedAt(LocalDateTime.now());

        return VarianceDto.from(reconciliationVarianceRepository.save(variance));
    }

    /**
     * 차이 무시 처리
     */
    public VarianceDto ignoreVariance(Long varianceId, String resolvedBy) {
        ReconciliationVariance variance = reconciliationVarianceRepository.findById(varianceId)
                .orElseThrow(() -> new IllegalArgumentException("대사 차이를 찾을 수 없습니다: " + varianceId));

        variance.setStatus(VarianceStatus.IGNORED);
        variance.setResolvedBy(resolvedBy);
        variance.setResolvedAt(LocalDateTime.now());

        return VarianceDto.from(reconciliationVarianceRepository.save(variance));
    }

    // ===== Private Helper Methods =====

    private ReconciliationResult createBaseResult(LocalDate reconciliationDate, ReconciliationType type, String runBy) {
        ReconciliationResult result = new ReconciliationResult();
        result.setReconciliationDate(reconciliationDate);
        result.setReconciliationType(type);
        result.setRunBy(runBy);
        result.setRunAt(LocalDateTime.now());
        return result;
    }

    private void createVarianceEntries(ReconciliationResult result,
            long sourceCount, long targetCount,
            BigDecimal sourceAmount, BigDecimal targetAmount) {
        if (sourceCount != targetCount) {
            ReconciliationVariance countVariance = new ReconciliationVariance();
            countVariance.setReconciliationResult(result);
            countVariance.setVarianceCode("COUNT_MISMATCH");
            countVariance.setDescription("원천 건수(" + sourceCount + ")와 대상 건수(" + targetCount + ") 불일치");
            countVariance.setAmount(BigDecimal.valueOf(Math.abs(sourceCount - targetCount)));
            countVariance.setStatus(VarianceStatus.OPEN);
            reconciliationVarianceRepository.save(countVariance);
        }

        BigDecimal amountDiff = sourceAmount.subtract(targetAmount);
        if (amountDiff.compareTo(BigDecimal.ZERO) != 0) {
            ReconciliationVariance amountVariance = new ReconciliationVariance();
            amountVariance.setReconciliationResult(result);
            amountVariance.setVarianceCode("AMOUNT_MISMATCH");
            amountVariance.setDescription("원천 금액(" + sourceAmount + ")과 대상 금액(" + targetAmount + ") 불일치");
            amountVariance.setAmount(amountDiff.abs());
            amountVariance.setDrCrType(amountDiff.compareTo(BigDecimal.ZERO) > 0 ? "DR" : "CR");
            amountVariance.setStatus(VarianceStatus.OPEN);
            reconciliationVarianceRepository.save(amountVariance);
        }
    }

    private ReconciliationResponseDto toResponseDto(ReconciliationResult entity) {
        return ReconciliationResponseDto.from(entity);
    }

    // ===== 데이터 조회 헬퍼 (실제 구현 시 확장 필요) =====

    private long countSourceTransactions(LocalDate date) {
        // TODO: 실제 원천 시스템 데이터 건수 조회
        return 0L;
    }

    private BigDecimal sumSourceTransactionAmounts(LocalDate date) {
        // TODO: 실제 원천 시스템 데이터 금액 합계 조회
        return BigDecimal.ZERO;
    }

    private long countJournalEntries(LocalDate date) {
        return journalEntryRepository.findByAccountingDate(date).size();
    }

    private BigDecimal sumJournalEntryAmounts(LocalDate date) {
        return journalEntryRepository.findByAccountingDate(date).stream()
                .flatMap(je -> je.getDetails().stream())
                .filter(jd -> "DEBIT".equals(jd.getDrcrType())) // 차변 합계 기준
                .map(com.ho.account.journal.domain.JournalDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumJournalDebitAmounts(LocalDate date) {
        return sumJournalEntryAmounts(date);
    }

    private BigDecimal sumJournalCreditAmounts(LocalDate date) {
        return journalEntryRepository.findByAccountingDate(date).stream()
                .flatMap(je -> je.getDetails().stream())
                .filter(jd -> "CREDIT".equals(jd.getDrcrType()))
                .map(com.ho.account.journal.domain.JournalDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
