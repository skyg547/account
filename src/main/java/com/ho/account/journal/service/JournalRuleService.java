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

    // JournalRule CRUD 작업

    public JournalRule createJournalRule(JournalRule journalRule) {
        // 저장 전에 conditions와 ruleDetails가 규칙에 연결되었는지 확인
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
        existingRule.setCreatedBy(updatedRule.getCreatedBy()); // createdBy가 수정 시 갱신될 수 있다고 가정

        // 조건 처리: 기존 항목 제거 후 신규 항목 추가
        existingRule.getConditions().clear();
        updatedRule.getConditions().forEach(condition -> {
            existingRule.addCondition(condition);
        });

        // 규칙 상세 처리: 기존 항목 제거 후 신규 항목 추가
        existingRule.getRuleDetails().clear();
        updatedRule.getRuleDetails().forEach(ruleDetail -> {
            existingRule.addRuleDetail(ruleDetail);
        });

        return journalRuleRepository.save(existingRule);
    }

    public void deleteJournalRule(Long id) {
        journalRuleRepository.deleteById(id);
    }

    // 지정 일자 기준 활성 규칙을 우선순위와 버전 순으로 조회하는 메서드
    @Transactional(readOnly = true)
    public List<JournalRule> findActiveRules(LocalDate date) {
        return journalRuleRepository.findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(date, date);
    }
}
