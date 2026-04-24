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

    // JournalRule CRUD ?臾믩씜

    public JournalRule createJournalRule(JournalRule journalRule) {
        // ?????袁⑸퓠 conditions?? ruleDetails揶쎛 域뱀뮇????怨뚭퍙??뤿??遺? ?類ㅼ뵥
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
        existingRule.setCreatedBy(updatedRule.getCreatedBy()); // createdBy揶쎛 ??륁젟 ??揶쏄퉮???????덈뼄??揶쎛??

        // 鈺곌퀗援?筌ｌ꼶?? 疫꿸퀣????????볤탢 ???醫됲뇣 ?????곕떽?
        existingRule.getConditions().clear();
        updatedRule.getConditions().forEach(condition -> {
            existingRule.addCondition(condition);
        });

        // 域뱀뮇???怨멸쉭 筌ｌ꼶?? 疫꿸퀣????????볤탢 ???醫됲뇣 ?????곕떽?
        existingRule.getRuleDetails().clear();
        updatedRule.getRuleDetails().forEach(ruleDetail -> {
            existingRule.addRuleDetail(ruleDetail);
        });

        return journalRuleRepository.save(existingRule);
    }

    public void deleteJournalRule(Long id) {
        journalRuleRepository.deleteById(id);
    }

    // 筌왖????깆쁽 疫꿸퀣? ??뽮쉐 域뱀뮇????怨쀪퐨??뽰맄?? 甕곌쑴????뽰몵嚥?鈺곌퀬???롫뮉 筌롫뗄苑??
    @Transactional(readOnly = true)
    public List<JournalRule> findActiveRules(LocalDate date) {
        return journalRuleRepository.findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(date, date);
    }
}
