package com.ho.account.report.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
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
 * ?¬ë¬´?œí‘œ(?¬ë¬´?íƒœ?? ?ìµê³„ì‚°?? ?ì„±???´ë‹¹?˜ëŠ” ?œë¹„???´ë˜?¤ì…?ˆë‹¤.
 * ë³´ê³  ?¼ì¸ ë§¤í•‘(ReportLineMapping)??ê¸°ë°˜?¼ë¡œ ê¸ˆì•¡??ì§‘ê³„?˜ë©°,
 * ?¹ì • ë³´ê³  ?¼ì¸?ì„œ ?ì²œ ?„í‘œê¹Œì? ì¶”ì ?????ˆëŠ” Drill-through ê¸°ëŠ¥???œê³µ?©ë‹ˆ??
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
     * ë³´ê³  ?¼ì¸ ë§¤í•‘ ê¸°ë°˜???¬ë¬´?íƒœ??Balance Sheet) ?ì„±
     *
     * @param asOfDate ê¸°ì???
     * @return ?¬ë¬´?íƒœ??ë³´ê³  ?¼ì¸ë³?ê¸ˆì•¡ ë¦¬ìŠ¤??
     */
    public List<FinancialStatementDTO> generateBalanceSheet(LocalDate asOfDate) {
        // 1. ?œì„±?”ëœ ë³´ê³  ?¼ì¸ ë§¤í•‘ ì¡°íšŒ
        List<ReportLineMapping> mappings = reportLineMappingRepository.findActiveByReportTypeAsOfDate("BS", asOfDate);

        // 2. ê°??¼ì¸ë³?ê¸ˆì•¡ ê³„ì‚°
        List<FinancialStatementDTO> result = new ArrayList<>();
        for (ReportLineMapping mapping : mappings) {
            BigDecimal lineAmount = BigDecimal.ZERO;
            
            if ("SUM".equals(mapping.getAggregationType()) && mapping.getAccountCode() != null) {
                // ?¹ì • ê³„ì •???”ì•¡ ?©ì‚°
                lineAmount = calculateAccountBalance(mapping.getAccountCode(), asOfDate);
            } else if ("FORMULA".equals(mapping.getAggregationType())) {
                // TODO: ?˜ì‹ ê³„ì‚° ë¡œì§ (?˜ìœ„ ?¼ì¸ ?©ì‚° ??
            }

            result.add(new FinancialStatementDTO(mapping.getLineCode(), mapping.getLineName(), lineAmount));
        }
        return result;
    }

    /**
     * ë³´ê³  ?¼ì¸ ë§¤í•‘ ê¸°ë°˜???ìµê³„ì‚°??Income Statement) ?ì„±
     *
     * @param startDate ?œì‘??
     * @param endDate   ì¢…ë£Œ??
     * @return ?ìµê³„ì‚°??ë³´ê³  ?¼ì¸ë³?ê¸ˆì•¡ ë¦¬ìŠ¤??
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
     * [Drill-through] ?¹ì • ë³´ê³  ?¼ì¸??ê¸ˆì•¡??êµ¬ì„±?˜ëŠ” ?„í‘œ ?ì„¸ ?´ì—­ ì¡°íšŒ
     * ë³´ê³  ?¼ì¸?ì„œ ?ì²œ ?„í‘œê¹Œì? 1ë¶???ì¶”ì  ê°€??(DoD ?µì‹¬)
     *
     * @param lineCode ë³´ê³  ?¼ì¸ ì½”ë“œ
     * @param startDate ?œì‘??
     * @param endDate   ì¢…ë£Œ??
     * @return ?„í‘œ ?ì„¸ ?´ì—­ ë¦¬ìŠ¤??
     */
    public List<JournalDetail> getJournalDetailsByReportLine(String lineCode, LocalDate startDate, LocalDate endDate) {
        // 1. ?´ë‹¹ ?¼ì¸??ë§¤í•‘??ê³„ì •??ì¡°íšŒ
        List<ReportLineMapping> mappings = reportLineMappingRepository.findByLineCode(lineCode);
        List<String> accountCodes = mappings.stream()
                .map(ReportLineMapping::getAccountCode)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // 2. ?´ë‹¹ ê³„ì •?¤ì˜ ê¸°ê°„ ???„í‘œ ?ì„¸ ?´ì—­ ë°˜í™˜
        return journalDetailRepository.findAll().stream()
                .filter(d -> accountCodes.contains(d.getAccountSubject().getCode()))
                .filter(d -> d.getJournalEntry().getAccountingDate().compareTo(startDate) >= 0)
                .filter(d -> d.getJournalEntry().getAccountingDate().compareTo(endDate) <= 0)
                .filter(d -> JournalEntryStatus.POSTED.equals(d.getJournalEntry().getStatus())) // ?„ê¸° ?„ë£Œ???„í‘œë§?
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
        // ?ì‚°, ë¹„ìš©?€ ì°¨ë?(+) ?€ë³€(-), ë¶€ì±? ?ë³¸, ?˜ìµ?€ ?€ë³€(+) ì°¨ë?(-)
        AccountSubject.AccountCategory category = detail.getAccountSubject().getCategory();
        boolean isDebitNormal = category == AccountSubject.AccountCategory.ASSETS || category == AccountSubject.AccountCategory.EXPENSES;
        
        if ("DEBIT".equals(detail.getDrcrType())) {
            return isDebitNormal ? detail.getAmount() : detail.getAmount().negate();
        } else {
            return isDebitNormal ? detail.getAmount().negate() : detail.getAmount();
        }
    }
}
