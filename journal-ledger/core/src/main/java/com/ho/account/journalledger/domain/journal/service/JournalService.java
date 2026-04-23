package com.ho.account.journalledger.domain.journal.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.audit.domain.AuditLoggable;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.expenditure.BudgetControlPort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.application.service.journal.JournalRuleService;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.unsettled.UnsettledService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;

/**
 * ?ÑÌëú Í¥ÄÎ¶??úÎπÑ??(Journal Service)
 */
@Service
@Transactional
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final accountSubjectPersistencePort accountSubjectPersistencePort;
    private final departmentPersistencePort departmentPersistencePort;
    private final businessPartnerPersistencePort businessPartnerPersistencePort;
    private final AccountingPeriodStatusPort accountingPeriodStatusPort;
    private final UnsettledService unsettledService;
    private final JournalRuleService journalRuleService;
    private final BudgetControlPort budgetControlPort;
    private final PostingService postingService;
    private final JournalRuleEngine journalRuleEngine;

    @Autowired
    public JournalService(JournalEntryRepository journalEntryRepository,
            accountSubjectPersistencePort accountSubjectPersistencePort,
            departmentPersistencePort departmentPersistencePort,
            businessPartnerPersistencePort businessPartnerPersistencePort,
            AccountingPeriodStatusPort accountingPeriodStatusPort,
            UnsettledService unsettledService,
            JournalRuleService journalRuleService,
            BudgetControlPort budgetControlPort,
            PostingService postingService,
            JournalRuleEngine journalRuleEngine) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.accountingPeriodStatusPort = accountingPeriodStatusPort;
        this.unsettledService = unsettledService;
        this.journalRuleService = journalRuleService;
        this.budgetControlPort = budgetControlPort;
        this.postingService = postingService;
        this.journalRuleEngine = journalRuleEngine;
    }

    // ?ÑÌëú ?ùÏÑ±
    @AuditLoggable(eventType = "JOURNAL", eventName = "CREATE")
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        if (journalEntry.getAccountingDate() == null) {
            journalEntry.setAccountingDate(journalEntry.getSlipDate());
        }
        if (accountingPeriodStatusPort.isClosed(journalEntry.getAccountingDate())) {
            throw new IllegalStateException("?¥Îãπ ?îÏ? ?¥Î? ÎßàÍ∞ê?òÏóà?µÎãà??");
        }
        validateJournalEntry(journalEntry);
        journalEntry.setSlipNo(generateSlipNo(journalEntry.getAccountingDate()));
        journalEntry.setStatus(JournalEntryStatus.DRAFT);
        journalEntry.getDetails().forEach(d -> d.setJournalEntry(journalEntry));
        return journalEntryRepository.save(journalEntry);
    }

    // ?ÑÌëú ?πÏù∏
    @AuditLoggable(eventType = "JOURNAL", eventName = "APPROVE")
    public void approveJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id).orElseThrow();
        if (accountingPeriodStatusPort.isClosed(entry.getAccountingDate())) throw new IllegalStateException("Closed");
        if (entry.getStatus() != JournalEntryStatus.REQUESTED) throw new IllegalStateException("Not Requested");
        entry.setStatus(JournalEntryStatus.APPROVED);
        journalEntryRepository.save(entry);
        entry.getDetails().stream()
            .filter(d -> Boolean.TRUE.equals(d.getAccountSubject().isUnsettled()))
            .forEach(unsettledService::createUnsettledItem);
    }

    // ?ÑÌëú ?ÑÍ∏∞ (Post) - ?µÏã¨ Î≥ÄÍ≤?ÏßÄ??
    @AuditLoggable(eventType = "JOURNAL", eventName = "POST")
    public void postJournalEntry(Long id) {
        // ÏßÅÏ†ë Ï≤òÎ¶¨?òÏ? ?äÍ≥† ?πÌôî??PostingService???ÑÏûÑ?òÏó¨ GlEntry ?ùÏÑ± Î∞?Balance ?ÖÎç∞?¥Ìä∏ ?òÌñâ
        postingService.postJournalEntry(id);
    }

    // ?ÑÌëú ?êÎèô ?ùÏÑ±
    public Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> transactionEvent, LocalDate accountingDate) {
        List<JournalRule> activeRules = journalRuleService.findActiveRules(accountingDate);
        for (JournalRule rule : activeRules) {
            if (journalRuleEngine.matches(rule, transactionEvent)) {
                JournalEntry generatedEntry = generateJournalEntry(rule, transactionEvent, accountingDate);
                return Optional.of(createJournalEntry(generatedEntry));
            }
        }
        return Optional.empty();
    }

    private JournalEntry generateJournalEntry(JournalRule rule, Map<String, Object> transactionEvent, LocalDate accountingDate) {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setSlipDate(LocalDate.now());
        journalEntry.setAccountingDate(accountingDate);
        journalEntry.setDescription(evaluateString(rule.getDescription() != null ? rule.getDescription() : rule.getRuleName(), transactionEvent));
        journalEntry.setStatus(JournalEntryStatus.DRAFT);
        
        for (JournalRuleDetail ruleDetail : rule.getRuleDetails()) {
            JournalDetail detail = new JournalDetail();
            detail.setDrcrType(ruleDetail.getDrcrType());
            String accountCode = evaluateString(ruleDetail.getAccountSubjectCodeExpression(), transactionEvent);
            detail.setAccountSubject(accountSubjectPersistencePort.findById(accountCode).orElseThrow());
            BigDecimal amount = evaluateBigDecimal(ruleDetail.getAmountExpression(), transactionEvent);
            detail.setAmount(amount);
            detail.setBaseAmount(amount);
            detail.setDetailDescription(evaluateString(ruleDetail.getDescriptionExpression(), transactionEvent));
            journalEntry.addDetail(detail);
        }
        return journalEntry;
    }

    private String evaluateString(String exp, Map<String, Object> event) {
        if (exp == null) return null;
        if (exp.startsWith("${") && exp.endsWith("}")) return String.valueOf(event.get(exp.substring(2, exp.length()-1)));
        return exp;
    }

    private BigDecimal evaluateBigDecimal(String exp, Map<String, Object> event) {
        if (exp == null) return BigDecimal.ZERO;
        if (exp.startsWith("${") && exp.endsWith("}")) {
            Object val = event.get(exp.substring(2, exp.length()-1));
            return val instanceof BigDecimal ? (BigDecimal) val : new BigDecimal(String.valueOf(val));
        }
        return new BigDecimal(exp);
    }

    private void validateJournalEntry(JournalEntry entry) {
        BigDecimal diff = entry.getDetails().stream()
            .map(d -> "DEBIT".equals(d.getDrcrType()) ? d.getAmount() : d.getAmount().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (diff.compareTo(BigDecimal.ZERO) != 0) throw new IllegalArgumentException("Not Balanced");
    }

    private String generateSlipNo(LocalDate date) {
        return date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis() % 1000;
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesByDate(LocalDate start, LocalDate end) {
        return journalEntryRepository.findByAccountingDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo) {
        return journalEntryRepository.findBySlipNo(slipNo);
    }
}
