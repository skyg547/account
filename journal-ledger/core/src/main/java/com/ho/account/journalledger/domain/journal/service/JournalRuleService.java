package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.JournalRule;
import com.ho.account.journalledger.domain.journal.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.JournalRuleDetail;
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
        // ????꾩뿉 conditions? ruleDetails媛 洹쒖튃???곌껐?섏뿀?붿? ?뺤씤
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
        existingRule.setCreatedBy(updatedRule.getCreatedBy()); // createdBy媛 ?섏젙 ??媛깆떊?????덈떎怨?媛??

        // 議곌굔 泥섎━: 湲곗〈 ??ぉ ?쒓굅 ???좉퇋 ??ぉ 異붽?
        existingRule.getConditions().clear();
        updatedRule.getConditions().forEach(condition -> {
            existingRule.addCondition(condition);
        });

        // 洹쒖튃 ?곸꽭 泥섎━: 湲곗〈 ??ぉ ?쒓굅 ???좉퇋 ??ぉ 異붽?
        existingRule.getRuleDetails().clear();
        updatedRule.getRuleDetails().forEach(ruleDetail -> {
            existingRule.addRuleDetail(ruleDetail);
        });

        return journalRuleRepository.save(existingRule);
    }

    public void deleteJournalRule(Long id) {
        journalRuleRepository.deleteById(id);
    }

    // 吏???쇱옄 湲곗? ?쒖꽦 洹쒖튃???곗꽑?쒖쐞? 踰꾩쟾 ?쒖쑝濡?議고쉶?섎뒗 硫붿꽌??
    @Transactional(readOnly = true)
    public List<JournalRule> findActiveRules(LocalDate date) {
        return journalRuleRepository.findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(date, date);
    }
}
