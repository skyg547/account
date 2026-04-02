package com.ho.account.report.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.report.domain.ReportLineMapping;
import com.ho.account.report.dto.FinancialStatementDTO;
import com.ho.account.report.repository.ReportLineMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 재무제표(재무상태표, 손익계산서) 생성을 담당하는 서비스 클래스입니다.
 * 보고 라인 매핑(ReportLineMapping)을 기반으로 금액을 집계하며,
 * 특정 보고 라인에서 원천 전표까지 추적할 수 있는 Drill-through 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
public class FinancialStatementService {

    private final JournalDetailRepository journalDetailRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final ReportLineMappingRepository reportLineMappingRepository;

    @Autowired
    public FinancialStatementService(JournalDetailRepository journalDetailRepository,
            AccountSubjectRepository accountSubjectRepository,
            ReportLineMappingRepository reportLineMappingRepository) {
        this.journalDetailRepository = journalDetailRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.reportLineMappingRepository = reportLineMappingRepository;
    }

    /**
     * 보고 라인 매핑 기반의 재무상태표(Balance Sheet) 생성
     *
     * @param asOfDate 기준일
     * @return 재무상태표 보고 라인별 금액 리스트
     */
    public List<FinancialStatementDTO> generateBalanceSheet(LocalDate asOfDate) {
        // 1. 활성화된 보고 라인 매핑 조회
        List<ReportLineMapping> mappings = reportLineMappingRepository.findActiveByReportTypeAsOfDate("BS", asOfDate);

        // 2. 각 라인별 금액 계산
        List<FinancialStatementDTO> result = new ArrayList<>();
        for (ReportLineMapping mapping : mappings) {
            BigDecimal lineAmount = BigDecimal.ZERO;
            
            if ("SUM".equals(mapping.getAggregationType()) && mapping.getAccountCode() != null) {
                // 특정 계정의 잔액 합산
                lineAmount = calculateAccountBalance(mapping.getAccountCode(), asOfDate);
            } else if ("FORMULA".equals(mapping.getAggregationType())) {
                // TODO: 수식 계산 로직 (하위 라인 합산 등)
            }

            result.add(new FinancialStatementDTO(mapping.getLineCode(), mapping.getLineName(), lineAmount));
        }
        return result;
    }

    /**
     * 보고 라인 매핑 기반의 손익계산서(Income Statement) 생성
     *
     * @param startDate 시작일
     * @param endDate   종료일
     * @return 손익계산서 보고 라인별 금액 리스트
     */
    public List<FinancialStatementDTO> generateIncomeStatement(LocalDate startDate, LocalDate endDate) {
        List<ReportLineMapping> mappings = reportLineMappingRepository.findActiveByReportTypeAsOfDate("IS", endDate);

        List<FinancialStatementDTO> result = new ArrayList<>();
        for (ReportLineMapping mapping : mappings) {
            BigDecimal lineAmount = BigDecimal.ZERO;
            
            if ("SUM".equals(mapping.getAggregationType()) && mapping.getAccountCode() != null) {
                lineAmount = calculateAccountActivity(mapping.getAccountCode(), startDate, endDate);
            }

            result.add(new FinancialStatementDTO(mapping.getLineCode(), mapping.getLineName(), lineAmount));
        }
        return result;
    }

    /**
     * [Drill-through] 특정 보고 라인의 금액을 구성하는 전표 상세 내역 조회
     * 보고 라인에서 원천 전표까지 1분 내 추적 가능 (DoD 핵심)
     *
     * @param lineCode 보고 라인 코드
     * @param startDate 시작일
     * @param endDate   종료일
     * @return 전표 상세 내역 리스트
     */
    public List<JournalDetail> getJournalDetailsByReportLine(String lineCode, LocalDate startDate, LocalDate endDate) {
        // 1. 해당 라인에 매핑된 계정들 조회
        List<ReportLineMapping> mappings = reportLineMappingRepository.findByLineCode(lineCode);
        List<String> accountCodes = mappings.stream()
                .map(ReportLineMapping::getAccountCode)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // 2. 해당 계정들의 기간 내 전표 상세 내역 반환
        return journalDetailRepository.findAll().stream()
                .filter(d -> accountCodes.contains(d.getAccountSubject().getCode()))
                .filter(d -> d.getJournalEntry().getAccountingDate().compareTo(startDate) >= 0)
                .filter(d -> d.getJournalEntry().getAccountingDate().compareTo(endDate) <= 0)
                .filter(d -> JournalEntryStatus.POSTED.equals(d.getJournalEntry().getStatus())) // 전기 완료된 전표만
                .collect(Collectors.toList());
    }

    // --- Helper Methods ---

    private BigDecimal calculateAccountBalance(String accountCode, LocalDate asOfDate) {
        return journalDetailRepository.findAll().stream()
                .filter(d -> d.getAccountSubject().getCode().equals(accountCode))
                .filter(d -> d.getJournalEntry().getAccountingDate().compareTo(asOfDate) <= 0)
                .filter(d -> JournalEntryStatus.POSTED.equals(d.getJournalEntry().getStatus()))
                .map(this::calculateSignedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAccountActivity(String accountCode, LocalDate startDate, LocalDate endDate) {
        return journalDetailRepository.findAll().stream()
                .filter(d -> d.getAccountSubject().getCode().equals(accountCode))
                .filter(d -> d.getJournalEntry().getAccountingDate().compareTo(startDate) >= 0)
                .filter(d -> d.getJournalEntry().getAccountingDate().compareTo(endDate) <= 0)
                .filter(d -> JournalEntryStatus.POSTED.equals(d.getJournalEntry().getStatus()))
                .map(JournalDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSignedAmount(JournalDetail detail) {
        // 자산, 비용은 차변(+) 대변(-), 부채, 자본, 수익은 대변(+) 차변(-)
        AccountSubject.AccountCategory category = detail.getAccountSubject().getCategory();
        boolean isDebitNormal = category == AccountSubject.AccountCategory.ASSETS || category == AccountSubject.AccountCategory.EXPENSES;
        
        if ("DEBIT".equals(detail.getDrcrType())) {
            return isDebitNormal ? detail.getAmount() : detail.getAmount().negate();
        } else {
            return isDebitNormal ? detail.getAmount().negate() : detail.getAmount();
        }
    }
}
