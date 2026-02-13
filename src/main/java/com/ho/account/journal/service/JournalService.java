package com.ho.account.journal.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.ledger.service.LedgerPostingService;
import com.ho.account.journal.service.JournalRuleService;
import com.ho.account.closing.service.ClosingService;
import com.ho.account.expenditure.service.BudgetService;
import com.ho.account.unsettled.service.UnsettledService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map; // Added
import java.util.Optional;
import com.ho.account.journal.domain.JournalRule;
import com.ho.account.journal.domain.JournalRuleCondition;
import com.ho.account.journal.domain.JournalRuleDetail;
import com.ho.account.journal.domain.ConditionOperator;

@Service
@Transactional
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final ClosingService closingService;
    private final UnsettledService unsettledService;
    private final JournalRuleService journalRuleService;
    private final BudgetService budgetService;
    private final LedgerPostingService ledgerPostingService;

    @Autowired
    public JournalService(JournalEntryRepository journalEntryRepository,
            AccountSubjectRepository accountSubjectRepository,
            DepartmentRepository departmentRepository,
            BusinessPartnerRepository businessPartnerRepository,
            ClosingService closingService,
            UnsettledService unsettledService,
            JournalRuleService journalRuleService,
            BudgetService budgetService,
            LedgerPostingService ledgerPostingService) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.closingService = closingService;
        this.unsettledService = unsettledService;
        this.journalRuleService = journalRuleService;
        this.budgetService = budgetService;
        this.ledgerPostingService = ledgerPostingService;
    }

    // 전표 생성
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        if (journalEntry.getAccountingDate() == null) {
            journalEntry.setAccountingDate(journalEntry.getSlipDate());
        }

        if (closingService.isClosed(journalEntry.getAccountingDate())) {
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
    public void approveJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (closingService.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다.");
        }

        if (entry.getStatus() != JournalEntryStatus.REQUESTED) {
            throw new IllegalStateException("승인 요청된 전표만 승인할 수 있습니다.");
        }
        entry.setRejectionReason(null);
        journalEntryRepository.save(entry);

        for (JournalDetail detail : entry.getDetails()) {
            if (Boolean.TRUE.equals(detail.getAccountSubject().isUnsettled())) {
                // UnsettledService의 메서드 시그니처가 변경될 예정이므로 임시 주석 처리 또는 수정 필요
                // 현재는 JournalDetail 객체를 그대로 넘기는 구조라고 가정
                unsettledService.createUnsettledItem(detail);
            }
        }
    }

    // 전표 수정
    public JournalEntry updateJournalEntry(Long id, JournalEntry journalEntryDetails) {
        JournalEntry existingEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (closingService.isClosed(existingEntry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 수정할 수 없습니다.");
        }
        if (journalEntryDetails.getAccountingDate() != null
                && closingService.isClosed(journalEntryDetails.getAccountingDate())) {
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
    public void deleteJournalEntry(Long id) {
        JournalEntry existingEntry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (closingService.isClosed(existingEntry.getAccountingDate())) {
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

        if (closingService.isClosed(entry.getAccountingDate())) {
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

        if (closingService.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다.");
        }

        if (entry.getStatus() != JournalEntryStatus.REQUESTED) {
            throw new IllegalStateException("승인 요청된 전표만 반려할 수 있습니다.");
        }
        entry.setRejectionReason(reason);
        journalEntryRepository.save(entry);
    }

    // 전표 전기 (Post Journal Entry)
    public void postJournalEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. ID: " + id));

        if (closingService.isClosed(entry.getAccountingDate())) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 전표를 전기할 수 없습니다.");
        }

        if (entry.getStatus() != JournalEntryStatus.APPROVED) {
            throw new IllegalStateException("승인된 전표만 전기할 수 있습니다.");
        }

        entry.setStatus(JournalEntryStatus.POSTED);
        journalEntryRepository.save(entry);

        // GL 잔액 업데이트 호출
        ledgerPostingService.postToLedger(entry);

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
            // Removed check for !account.getUseYn() as it's not present in AccountSubject
            // entity
            detail.setAccountSubject(account);

            if (detail.getDepartment() != null && detail.getDepartment().getCode() != null) {
                Department dept = departmentRepository.findByCode(detail.getDepartment().getCode())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "존재하지 않는 부서입니다: " + detail.getDepartment().getCode()));
                // Removed check for !dept.getUseYn() as it's not present in Department entity
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

            // TODO: Implement Tax Validation (DoD 05.3) - e.g., checking tax codes, rates,
            // and rules
            // if (detail.getTaxCode() != null) {
            // taxService.validateTaxImplications(detail, entry.getAccountingDate());
            // }

            if ("DEBIT".equals(detail.getDrcrType())) {
                debitSum = debitSum.add(detail.getAmount());
                // Budget Validation for DEBIT entries
                if (detail.getDepartment() == null || detail.getAccountSubject() == null) {
                    throw new IllegalArgumentException("예산 검사를 위해 부서와 계정과목은 필수입니다.");
                }
                String yearMonth = entry.getAccountingDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
                budgetService.checkBudgetAvailability(
                        yearMonth,
                        detail.getDepartment(),
                        detail.getAccountSubject(),
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

        if (closingService.isClosed(reversalDate)) {
            throw new IllegalStateException("해당 월은 이미 마감되었습니다. 역분개 전표를 생성할 수 없습니다.");
        }

        if (originalEntry.getStatus() != JournalEntryStatus.POSTED) {
            throw new IllegalStateException("전기된 전표만 역분개할 수 있습니다.");
        }

        // Create a new JournalEntry for the reversal
        JournalEntry reversalEntry = new JournalEntry();
        reversalEntry.setSlipDate(LocalDate.now());
        reversalEntry.setAccountingDate(reversalDate);
        reversalEntry.setDescription(
                "역분개: " + originalEntry.getDescription() + " (원 전표 No: " + originalEntry.getSlipNo() + ")");
        reversalEntry.setStatus(JournalEntryStatus.DRAFT); // Reversal entry starts as DRAFT
        reversalEntry.setEntryType("REVERSAL");

        for (JournalDetail originalDetail : originalEntry.getDetails()) {
            JournalDetail reversedDetail = new JournalDetail();
            reversedDetail.setAccountSubject(originalDetail.getAccountSubject());
            reversedDetail.setAmount(originalDetail.getAmount());
            reversedDetail.setDepartment(originalDetail.getDepartment());
            reversedDetail.setBusinessPartner(originalDetail.getBusinessPartner());
            reversedDetail.setDetailDescription("역분개: " + originalDetail.getDetailDescription());

            // Reverse DR/CR type
            if ("DEBIT".equals(originalDetail.getDrcrType())) {
                reversedDetail.setDrcrType("CREDIT");
            } else {
                reversedDetail.setDrcrType("DEBIT");
            }
            reversalEntry.addDetail(reversedDetail);
        }

        validateJournalEntry(reversalEntry); // Validate the generated reversal entry

        // Mark original entry as REVERSED
        originalEntry.setStatus(JournalEntryStatus.REVERSED);
        // TODO: Optionally, link original to reversal and vice-versa for audit trail
        journalEntryRepository.save(originalEntry);

        // Save and return the new reversal entry
        return journalEntryRepository.save(reversalEntry);
    }

    /**
     * Creates a JournalEntry by applying defined journal rules to a given
     * transaction event.
     * 
     * @param transactionEvent A map representing the transaction data (e.g.,
     *                         "transactionType": "SALE", "amount": "1000").
     * @param accountingDate   The accounting date for the journal entry.
     * @return An Optional containing the generated JournalEntry if a rule matches,
     *         otherwise empty.
     */
    public Optional<JournalEntry> createJournalEntryFromEvent(Map<String, String> transactionEvent,
            LocalDate accountingDate) {
        List<JournalRule> activeRules = journalRuleService.findActiveRules(accountingDate);

        for (JournalRule rule : activeRules) {
            if (matchesConditions(rule, transactionEvent)) {
                return Optional.of(generateJournalEntry(rule, transactionEvent, accountingDate));
            }
        }
        return Optional.empty();
    }

    private boolean matchesConditions(JournalRule rule, Map<String, String> transactionEvent) {
        for (JournalRuleCondition condition : rule.getConditions()) {
            String eventValue = transactionEvent.get(condition.getField());
            if (eventValue == null) {
                return false; // Condition field not present in event
            }

            // Simple string comparison for now. More complex evaluation needed for full
            // expression support.
            // This part would be enhanced with a proper expression engine for advanced
            // operators.
            switch (condition.getOperator()) {
                case EQUALS:
                    if (!eventValue.equals(condition.getValue()))
                        return false;
                    break;
                case NOT_EQUALS:
                    if (eventValue.equals(condition.getValue()))
                        return false;
                    break;
                case STARTS_WITH:
                    if (!eventValue.startsWith(condition.getValue()))
                        return false;
                    break;
                case ENDS_WITH:
                    if (!eventValue.endsWith(condition.getValue()))
                        return false;
                    break;
                case CONTAINS:
                    if (!eventValue.contains(condition.getValue()))
                        return false;
                    break;
                // Add more operators as needed (e.g., GREATER_THAN, LESS_THAN for numeric
                // values)
                default:
                    // For unsupported operators, assume no match or throw an error
                    return false;
            }
        }
        return true; // All conditions matched
    }

    private JournalEntry generateJournalEntry(JournalRule rule, Map<String, String> transactionEvent,
            LocalDate accountingDate) {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setSlipDate(LocalDate.now()); // Current date for slip date
        journalEntry.setAccountingDate(accountingDate);
        journalEntry.setDescription(rule.getDescription() != null ? rule.getDescription() : rule.getRuleName());
        journalEntry.setStatus(JournalEntryStatus.DRAFT); // Rules generate DRAFT entries

        for (JournalRuleDetail ruleDetail : rule.getRuleDetails()) {
            JournalDetail detail = new JournalDetail();
            detail.setDrcrType(ruleDetail.getDrcrType());

            // Evaluate expressions (simple direct lookup or static value for now)
            detail.setAccountSubject(
                    evaluateAccountSubjectExpression(ruleDetail.getAccountSubjectCodeExpression(), transactionEvent));
            detail.setAmount(evaluateAmountExpression(ruleDetail.getAmountExpression(), transactionEvent));
            detail.setBusinessPartner(
                    evaluateBusinessPartnerExpression(ruleDetail.getBusinessPartnerCodeExpression(), transactionEvent));
            detail.setDepartment(
                    evaluateDepartmentExpression(ruleDetail.getDepartmentCodeExpression(), transactionEvent));
            detail.setDetailDescription(
                    evaluateDescriptionExpression(ruleDetail.getDescriptionExpression(), transactionEvent));

            journalEntry.addDetail(detail);
        }

        validateJournalEntry(journalEntry); // Validate the generated entry
        return journalEntry;
    }

    // Helper methods to evaluate expressions.
    // For now, these will simply check if the expression is a direct value or a
    // placeholder like "${key}".
    // In a full implementation, a robust expression parser (e.g., SpEL) would be
    // used.
    private AccountSubject evaluateAccountSubjectExpression(String expression, Map<String, String> transactionEvent) {
        String value = extractValueFromExpression(expression, transactionEvent);
        if (value != null) {
            return accountSubjectRepository.findById(value)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Invalid Account Subject Code from rule: " + value));
        }
        return null; // Or handle as error
    }

    private BigDecimal evaluateAmountExpression(String expression, Map<String, String> transactionEvent) {
        String value = extractValueFromExpression(expression, transactionEvent);
        if (value != null) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid Amount expression result from rule: " + value, e);
            }
        }
        return BigDecimal.ZERO; // Or handle as error
    }

    private BusinessPartner evaluateBusinessPartnerExpression(String expression, Map<String, String> transactionEvent) {
        String value = extractValueFromExpression(expression, transactionEvent);
        if (value != null) {
            return businessPartnerRepository.findByBusinessPartnerCode(value)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Invalid Business Partner Code from rule: " + value));
        }
        return null;
    }

    private Department evaluateDepartmentExpression(String expression, Map<String, String> transactionEvent) {
        String value = extractValueFromExpression(expression, transactionEvent);
        if (value != null) {
            return departmentRepository.findByCode(value)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Department Code from rule: " + value));
        }
        return null;
    }

    private String evaluateDescriptionExpression(String expression, Map<String, String> transactionEvent) {
        return extractValueFromExpression(expression, transactionEvent);
    }

    private String extractValueFromExpression(String expression, Map<String, String> transactionEvent) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String key = expression.substring(2, expression.length() - 1);
            return transactionEvent.get(key);
        }
        return expression; // Treat as static value
    }
}
