package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
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

    // JournalRule CRUD ?묒뾽

    public JournalRule createJournalRule(JournalRule journalRule) {
        // ?????꾩뿉 conditions?? ruleDetails媛 洹쒖????곌껐??��??�? ?뺤씤
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
        existingRule.setCreatedBy(updatedRule.getCreatedBy()); // createdBy媛 ??�젙 ??媛깆???????�떎??媛??

        // 議곌�?泥섎?? 湲곗????????�굅 ???좉퇋 ?????�붽?
        existingRule.getConditions().clear();
        updatedRule.getConditions().forEach(condition -> {
            existingRule.addCondition(condition);
        });

        // 洹쒖???곸꽭 泥섎?? 湲곗????????�굅 ???좉퇋 ?????�붽?
        existingRule.getRuleDetails().clear();
        updatedRule.getRuleDetails().forEach(ruleDetail -> {
            existingRule.addRuleDetail(ruleDetail);
        });

        return journalRuleRepository.save(existingRule);
    }

    public void deleteJournalRule(Long id) {
        journalRuleRepository.deleteById(id);
    }

    // 吏????�옄 湲곗? ??�꽦 洹쒖????곗꽑??�쐞?? 踰꾩????�쑝�?議고???�뒗 硫붿�??
    @Transactional(readOnly = true)
    public List<JournalRule> findActiveRules(LocalDate date) {
        return journalRuleRepository.findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(date, date);
    }
}
