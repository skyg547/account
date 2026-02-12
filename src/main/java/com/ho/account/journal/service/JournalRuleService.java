package com.ho.account.journal.service;

import com.ho.account.journal.domain.JournalRule;
import com.ho.account.journal.domain.JournalRuleCondition;
import com.ho.account.journal.domain.JournalRuleDetail;
import com.ho.account.journal.repository.JournalRuleRepository;
import com.ho.account.journal.repository.JournalRuleConditionRepository;
import com.ho.account.journal.repository.JournalRuleDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JournalRuleService {

    private final JournalRuleRepository journalRuleRepository;
    private final JournalRuleConditionRepository journalRuleConditionRepository;
    private final JournalRuleDetailRepository journalRuleDetailRepository;

    @Autowired
    public JournalRuleService(JournalRuleRepository journalRuleRepository,
                              JournalRuleConditionRepository journalRuleConditionRepository,
                              JournalRuleDetailRepository journalRuleDetailRepository) {
        this.journalRuleRepository = journalRuleRepository;
        this.journalRuleConditionRepository = journalRuleConditionRepository;
        this.journalRuleDetailRepository = journalRuleDetailRepository;
    }

    // JournalRule CRUD Operations

    public JournalRule createJournalRule(JournalRule journalRule) {
        // Ensure that conditions and ruleDetails are linked to the rule before saving
        journalRule.getConditions().forEach(condition -> condition.setJournalRule(journalRule));
        journalRule.getRuleDetails().forEach(ruleDetail -> ruleDetail.setJournalRule(journalRule));
        return journalRuleRepository.save(journalRule);
    }

    @Transactional(readOnly = true)
    public Optional<JournalRule> getJournalRuleById(Long id) {
        return journalRuleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<JournalRule> getJournalRuleByCode(String ruleCode) {
        return journalRuleRepository.findByRuleCode(ruleCode);
    }

    @Transactional(readOnly = true)
    public List<JournalRule> getAllJournalRules() {
        return journalRuleRepository.findAll();
    }

    public JournalRule updateJournalRule(Long id, JournalRule updatedRule) {
        JournalRule existingRule = journalRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal Rule not found with ID: " + id));

        existingRule.setRuleName(updatedRule.getRuleName());
        existingRule.setDescription(updatedRule.getDescription());
        existingRule.setValidFrom(updatedRule.getValidFrom());
        existingRule.setValidTo(updatedRule.getValidTo());
        existingRule.setVersion(updatedRule.getVersion());
        existingRule.setActive(updatedRule.isActive());
        existingRule.setPriority(updatedRule.getPriority());
        existingRule.setCreatedBy(updatedRule.getCreatedBy()); // Assuming createdBy can be updated or set on update

        // Handle conditions: clear existing and add new ones
        existingRule.getConditions().clear();
        updatedRule.getConditions().forEach(condition -> {
            existingRule.addCondition(condition);
        });

        // Handle rule details: clear existing and add new ones
        existingRule.getRuleDetails().clear();
        updatedRule.getRuleDetails().forEach(ruleDetail -> {
            existingRule.addRuleDetail(ruleDetail);
        });

        return journalRuleRepository.save(existingRule);
    }

    public void deleteJournalRule(Long id) {
        journalRuleRepository.deleteById(id);
    }

    // Method to find active rules for a given date, ordered by priority and version
    @Transactional(readOnly = true)
    public List<JournalRule> findActiveRules(LocalDate date) {
        return journalRuleRepository.findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(date, date);
    }
}
