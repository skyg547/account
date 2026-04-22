package com.ho.account.journalledger.application.service.journal;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.audit.domain.AuditLoggable;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.expenditure.BudgetControlPort;
import com.ho.account.journalledger.domain.journal.JournalDetail;
import com.ho.account.journalledger.domain.journal.JournalEntry;
import com.ho.account.journalledger.domain.journal.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
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
import com.ho.account.journalledger.domain.journal.JournalRule;
import com.ho.account.journalledger.domain.journal.JournalRuleDetail;

/**
 * 전표 관리 서비스 (Journal Service)
 * 전표의 생성, 수정, 승인, 확정(Posting), 역분개(Reversal) 등 전표 라이프사이클을 관리함.
 */
@Service
@Transactional
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final AccountingPeriodStatusPort accountingPeriodStatusPort;
    private final UnsettledService unsettledService;
    private final JournalRuleService journalRuleService;
    private final BudgetControlPort budgetControlPort;
    private final LedgerService ledgerService;
    private final JournalRuleEngine journalRuleEngine;

    @Autowired
    public JournalService(JournalEntryRepository journalEntryRepository,
            AccountSubjectRepository accountSubjectRepository,
            DepartmentRepository departmentRepository,
            BusinessPartnerRepository businessPartnerRepository,
            AccountingPeriodStatusPort accountingPeriodStatusPort,
            UnsettledService unsettledService,
            JournalRuleService journalRuleService,
            BudgetControlPort budgetControlPort,
            LedgerService ledgerService,
            JournalRuleEngine journalRuleEngine) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.accountingPeriodStatusPort = accountingPeriodStatusPort;
        this.unsettledService = unsettledService;
        this.journalRuleService = journalRuleService;
        this.budgetControlPort = budgetControlPort;
        this.ledgerService = ledgerService;
        this.journalRuleEngine = journalRuleEngine;
    }

    // 전표 생성
    @AuditLoggable(eventType = "JOURNAL", eventName = "CREATE")
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        if (journalEntry.getAccountingDate() == null) {
            journalEntry.setAccountingDate(journalEntry.getSlipDate());
        }

        if (accountingPeriodStatusPort.isClosed(journalEntry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 생성할 수 없습니다.");
        }

        validateJournalEntry(journalEntry);

        String slipNo = generateSlipNo(journalEntry.getAccountingDate());
        journalEntry.setSlipNo(slipNo);
        journalEntry.setStatus(JournalEntryStatus.DRAFT);

        if (journalEntry.getAuditUser() == null) {
            journalEntry.setAuditUser(journalEntry.getCreatedBy() != null ? journalEntry.getCreatedBy() : "SYSTEM");
        }

        for (JournalDetail detail : journalEntry.getDetails()) {
            detail.setJournalEntry(journalEntry);
            if (detail.getAuditUser() == null) {
                detail.setAuditUser(journalEntry.getAuditUser());
            }
        }

        return journalEntryRepository.save(journalEntry);
    }

    // 전표 승인
    @AuditLoggable(eventType = "JOURNAL", eventName = "APPROVE")
    public void approveJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (accountingPeriodStatusPort.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다.");
        }

        if (entry.getStatus() != JournalEntryStatus.REQUESTED) {
            throw new IllegalStateException("승인 요청된 전표만 승인할 수 있습니다.");
        }

        // SOD Check: Maker != Checker
        String currentApprover = entry.getAuditUser(); // Simplified for now
        if (entry.getCreatedBy() != null && entry.getCreatedBy().equals(currentApprover)) {
            throw new IllegalStateException("전표 작성자는 직접 승인할 수 없습니다. (직무 분리 원칙)");
        }

        entry.setStatus(JournalEntryStatus.APPROVED);
        entry.setRejectionReason(null);
        journalEntryRepository.save(entry);

        for (JournalDetail detail : entry.getDetails()) {
            if (Boolean.TRUE.equals(detail.getAccountSubject().isUnsettled())) {
                unsettledService.createUnsettledItem(detail);
            }
        }
    }

    // 전표 수정
    @AuditLoggable(eventType = "JOURNAL", eventName = "UPDATE")
    public JournalEntry updateJournalEntry(Long id, JournalEntry journalEntryDetails) {
        JournalEntry existingEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (accountingPeriodStatusPort.isClosed(existingEntry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 수정할 수 없습니다.");
        }
        if (journalEntryDetails.getAccountingDate() != null
                && accountingPeriodStatusPort.isClosed(journalEntryDetails.getAccountingDate())) {
            throw new IllegalStateException("변경하려는 날짜의 월은 이미 마감되었습니다.");
        }

        if (existingEntry.getStatus() != JournalEntryStatus.DRAFT
                && existingEntry.getStatus() != JournalEntryStatus.REJECTED) {
            throw new IllegalStateException("작성중이거나 반려된 전표만 수정할 수 있습니다.");
        }

        existingEntry.setSlipDate(journalEntryDetails.getSlipDate());
        existingEntry.setAccountingDate(
                journalEntryDetails.getAccountingDate() != null ? journalEntryDetails.getAccountingDate()
                        : journalEntryDetails.getSlipDate());
        existingEntry.setDescription(journalEntryDetails.getDescription());

        existingEntry.clearDetails();
        for (JournalDetail detail : journalEntryDetails.getDetails()) {
            if (detail.getAuditUser() == null) {
                detail.setAuditUser(
                        journalEntryDetails.getAuditUser() != null ? journalEntryDetails.getAuditUser() : "SYSTEM");
            }
            existingEntry.addDetail(detail);
        }

        existingEntry.setAuditUser(
                journalEntryDetails.getAuditUser() != null ? journalEntryDetails.getAuditUser() : "SYSTEM");

        validateJournalEntry(existingEntry);

        return journalEntryRepository.save(existingEntry);
    }

    // 전표 삭제
    @AuditLoggable(eventType = "JOURNAL", eventName = "DELETE")
    public void deleteJournalEntry(Long id) {
        JournalEntry existingEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (accountingPeriodStatusPort.isClosed(existingEntry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 삭제할 수 없습니다.");
        }

        if (existingEntry.getStatus() != JournalEntryStatus.DRAFT) {
            throw new IllegalStateException("작성중인 전표만 삭제할 수 있습니다.");
        }
        journalEntryRepository.delete(existingEntry);
    }

    // 승인 요청
    public void requestApproval(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (accountingPeriodStatusPort.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다.");
        }

        if (entry.getStatus() != JournalEntryStatus.DRAFT && entry.getStatus() != JournalEntryStatus.REJECTED) {
            throw new IllegalStateException("작성중이거나 반려된 전표만 승인 요청할 수 있습니다.");
        }

        entry.setStatus(JournalEntryStatus.REQUESTED);
        journalEntryRepository.save(entry);
    }

    // 전표 반려
    public void rejectJournalEntry(Long id, String reason) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (accountingPeriodStatusPort.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다.");
        }

        if (entry.getStatus() != JournalEntryStatus.REQUESTED) {
            throw new IllegalStateException("승인 요청된 전표만 반려할 수 있습니다.");
        }
        entry.setStatus(JournalEntryStatus.REJECTED);
        entry.setRejectionReason(reason);
        journalEntryRepository.save(entry);
    }

    // 전표 전기
    @AuditLoggable(eventType = "JOURNAL", eventName = "POST")
    public void postJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (accountingPeriodStatusPort.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 전기할 수 없습니다.");
        }

        if (entry.getStatus() != JournalEntryStatus.APPROVED) {
            throw new IllegalStateException("승인된 전표만 전기할 수 있습니다.");
        }

        entry.setStatus(JournalEntryStatus.POSTED);
        journalEntryRepository.save(entry);

        for (JournalDetail detail : entry.getDetails()) {
            ledgerService.updateLedgerBalances(detail, entry.getAccountingDate());
        }

        System.out.println("Journal Entry " + entry.getSlipNo() + " has been posted to GL.");
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate) {
        return journalEntryRepository.findByAccountingDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo) {
        return journalEntryRepository.findBySlipNo(slipNo);
    }

    @Transactional(readOnly = true)
    public Optional<JournalEntry> getJournalEntryWithDetails(Long id) {
        return journalEntryRepository.findByIdWithDetails(id);
    }

    private void validateJournalEntry(JournalEntry entry) {
        BigDecimal debitSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;

        for (JournalDetail detail : entry.getDetails()) {
            if (detail.getAccountSubject() == null || detail.getAccountSubject().getCode() == null) {
                throw new IllegalArgumentException("계정과목 코드는 필수입니다.");
            }
            AccountSubject account = accountSubjectRepository.findById(detail.getAccountSubject().getCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 계정과목입니다: " + detail.getAccountSubject().getCode()));
            detail.setAccountSubject(account);

            if (detail.getDepartment() != null && detail.getDepartment().getCode() != null) {
                Department dept = departmentRepository.findByCode(detail.getDepartment().getCode())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "존재하지 않는 부서입니다: " + detail.getDepartment().getCode()));
                detail.setDepartment(dept);
            }

            if (detail.getBusinessPartner() != null && detail.getBusinessPartner().getBusinessPartnerCode() != null) {
                BusinessPartner businessPartner = businessPartnerRepository
                        .findByBusinessPartnerCode(detail.getBusinessPartner().getBusinessPartnerCode())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "존재하지 않는 거래처입니다: " + detail.getBusinessPartner().getBusinessPartnerCode()));
                if (!businessPartner.getUseYn()) {
                    throw new IllegalArgumentException("사용 중지된 거래처입니다: " + businessPartner.getBusinessPartnerName());
                }
                detail.setBusinessPartner(businessPartner);
            }

            if ("DEBIT".equals(detail.getDrcrType())) {
                debitSum = debitSum.add(detail.getAmount());
                if (detail.getDepartment() == null || detail.getAccountSubject() == null) {
                    throw new IllegalArgumentException("예산 검사를 위해 부서와 계정과목은 필수입니다.");
                }
                String yearMonth = entry.getAccountingDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
                budgetControlPort.checkBudgetAvailability(
                        yearMonth,
                        detail.getDepartment().getCode(),
                        detail.getAccountSubject().getCode(),
                        detail.getAmount());
            } else if ("CREDIT".equals(detail.getDrcrType())) {
                creditSum = creditSum.add(detail.getAmount());
            } else {
                throw new IllegalArgumentException("차대 구분은 DEBIT 또는 CREDIT이어야 합니다.");
            }
        }

        if (debitSum.compareTo(creditSum) != 0) {
            throw new IllegalArgumentException("차변과 대변의 합계가 일치하지 않습니다. 차변: " + debitSum + ", 대변: " + creditSum);
        }
    }

    private String generateSlipNo(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        List<JournalEntry> entries = journalEntryRepository.findByAccountingDate(date);
        int seq = entries.size() + 1;
        return String.format("%s-%03d", dateStr, seq);
    }

    // 전표 역분개 (Reverse Journal Entry)
    public JournalEntry reverseJournalEntry(Long id, LocalDate reversalDate) {
        JournalEntry originalEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("원 전표를 찾을 수 없습니다. ID: " + id));

        if (accountingPeriodStatusPort.isClosed(reversalDate)) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 역분개 전표를 생성할 수 없습니다.");
        }

        if (originalEntry.getStatus() != JournalEntryStatus.POSTED) {
            throw new IllegalStateException("전기된 전표만 역분개할 수 있습니다.");
        }

        JournalEntry reversalEntry = new JournalEntry();
        reversalEntry.setSlipDate(LocalDate.now());
        reversalEntry.setAccountingDate(reversalDate);
        reversalEntry.setDescription(
                "역분개: " + originalEntry.getDescription() + " (원 전표 No: " + originalEntry.getSlipNo() + ")");
        reversalEntry.setStatus(JournalEntryStatus.DRAFT);
        reversalEntry.setEntryType("REVERSAL");

        for (JournalDetail originalDetail : originalEntry.getDetails()) {
            JournalDetail reversedDetail = new JournalDetail();
            reversedDetail.setAccountSubject(originalDetail.getAccountSubject());
            reversedDetail.setAmount(originalDetail.getAmount());
            reversedDetail.setDepartment(originalDetail.getDepartment());
            reversedDetail.setBusinessPartner(originalDetail.getBusinessPartner());
            reversedDetail.setDetailDescription("역분개: " + originalDetail.getDetailDescription());

            if ("DEBIT".equals(originalDetail.getDrcrType())) {
                reversedDetail.setDrcrType("CREDIT");
            } else {
                reversedDetail.setDrcrType("DEBIT");
            }
            reversalEntry.addDetail(reversedDetail);
        }

        validateJournalEntry(reversalEntry);
        originalEntry.setStatus(JournalEntryStatus.REVERSED);
        journalEntryRepository.save(originalEntry);

        return journalEntryRepository.save(reversalEntry);
    }

    /**
     * 트랜잭션 이벤트에 룰을 적용하여 전표를 자동 생성합니다.
     */
    public Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> transactionEvent,
            LocalDate accountingDate) {
        List<JournalRule> activeRules = journalRuleService.findActiveRules(accountingDate);

        for (JournalRule rule : activeRules) {
            if (journalRuleEngine.matches(rule, transactionEvent)) {
                JournalEntry generatedEntry = generateJournalEntry(rule, transactionEvent, accountingDate);
                return Optional.of(createJournalEntry(generatedEntry));
            }
        }
        return Optional.empty();
    }

    private JournalEntry generateJournalEntry(JournalRule rule, Map<String, Object> transactionEvent,
            LocalDate accountingDate) {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setSlipDate(LocalDate.now());
        journalEntry.setAccountingDate(accountingDate);
        
        String description = evaluateString(rule.getDescription() != null ? rule.getDescription() : rule.getRuleName(), transactionEvent);
        journalEntry.setDescription(description);
        journalEntry.setStatus(JournalEntryStatus.DRAFT);
        journalEntry.setEntryType("AUTO");

        for (JournalRuleDetail ruleDetail : rule.getRuleDetails()) {
            JournalDetail detail = new JournalDetail();
            detail.setDrcrType(ruleDetail.getDrcrType());

            String accountCode = evaluateString(ruleDetail.getAccountSubjectCodeExpression(), transactionEvent);
            detail.setAccountSubject(accountSubjectRepository.findById(accountCode)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Account Subject Code from rule: " + accountCode)));

            BigDecimal amount = evaluateBigDecimal(ruleDetail.getAmountExpression(), transactionEvent);
            detail.setAmount(amount);
            detail.setBaseAmount(amount);

            String bpCode = evaluateString(ruleDetail.getBusinessPartnerCodeExpression(), transactionEvent);
            if (bpCode != null) {
                detail.setBusinessPartner(businessPartnerRepository.findByBusinessPartnerCode(bpCode).orElse(null));
            }

            String deptCode = evaluateString(ruleDetail.getDepartmentCodeExpression(), transactionEvent);
            if (deptCode != null) {
                detail.setDepartment(departmentRepository.findByCode(deptCode).orElse(null));
            }

            detail.setDetailDescription(evaluateString(ruleDetail.getDescriptionExpression(), transactionEvent));

            journalEntry.addDetail(detail);
        }

        return journalEntry;
    }

    private String evaluateString(String expression, Map<String, Object> event) {
        if (expression == null || expression.isEmpty()) return null;
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String key = expression.substring(2, expression.length() - 1);
            Object val = event.get(key);
            return val != null ? String.valueOf(val) : null;
        }
        return expression;
    }

    private BigDecimal evaluateBigDecimal(String expression, Map<String, Object> event) {
        if (expression == null || expression.isEmpty()) return BigDecimal.ZERO;
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String key = expression.substring(2, expression.length() - 1);
            Object val = event.get(key);
            if (val instanceof BigDecimal) return (BigDecimal) val;
            if (val instanceof Number) return new BigDecimal(val.toString());
            if (val instanceof String) return new BigDecimal((String) val);
        }
        try {
            return new BigDecimal(expression);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
