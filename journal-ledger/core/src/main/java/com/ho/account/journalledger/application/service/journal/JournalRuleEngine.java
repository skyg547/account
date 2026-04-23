package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * <h3>?„í‘œ ë£??”ì§„ (Journal Rule Engine)</h3>
 * <p>
 * ???”ì§„?€ ?¸ë??ì„œ ? ì…??ê²½ì œ???¬ê±´(?´ë²¤????ë¶„ì„?˜ì—¬, ë¯¸ë¦¬ ?•ì˜???Œê³„ ì²˜ë¦¬ ê·œì¹™(Rule)???°ë¼
 * ?ë™?¼ë¡œ ë³µì‹ë¶€ê¸??„í‘œë¥??ì„±?˜ëŠ” ??• ???©ë‹ˆ??
 * </p>
 *
 * <b>ì£¼ìš” ê¸°ëŠ¥:</b>
 * <ul>
 *   <li><b>SpEL ì§€??</b> Spring Expression Languageë¥??¬ìš©?˜ì—¬ "${amount} * 0.1"ê³?ê°™ì? ?™ì  ?˜ì‹ ê³„ì‚°??ì§€?í•©?ˆë‹¤.</li>
 *   <li><b>ì¡°ê±´ ë§¤ì¹­:</b> ?´ë²¤???°ì´?°ì˜ ?¹ì • ?„ë“œê°’ê³¼ ë£°ì— ?•ì˜??ì¡°ê±´??ë¹„êµ?˜ì—¬ ?ìš© ?¬ë?ë¥??ë‹¨?©ë‹ˆ??</li>
 *   <li><b>?°ì´??ë³€??</b> ë¹„ì¦ˆ?ˆìŠ¤ ?°ì´?°ë? ?Œê³„ ?°ì´??ê³„ì •ê³¼ëª©, ì°¨ë?ë³€)ë¡?ë³€?˜í•©?ˆë‹¤.</li>
 * </ul>
 *
 * @author ëª¨ë˜ ?¬ë¬´ ?œìŠ¤??ê°œë°œ?€
 */
@Component
@RequiredArgsConstructor
public class JournalRuleEngine {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * ë£°ê³¼ ?´ë²¤?¸ë? ê²°í•©?˜ì—¬ ?„í‘œ ?”í‹°?°ë? ?ì„±?©ë‹ˆ??
     *
     * @param rule ?ìš©???Œê³„ ê·œì¹™
     * @param event ?¸ë? ?¸ëœ??…˜ ?°ì´??(Map ?•íƒœ)
     * @param accountingDate ?„í‘œ??ê¸°ë¡???Œê³„ ?¼ì
     * @return ?ì„±???„í‘œ ?”í‹°??(DRAFT ?íƒœ)
     */
    public JournalEntry generateJournal(JournalRule rule, Map<String, Object> event, LocalDate accountingDate) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        // ë£??´ë¦„?´ë‚˜ ?¤ëª…???œí˜„?ìœ¼ë¡??‰ê??˜ì—¬ ?„í‘œ ?ìš”ë¡??¤ì •
        entry.setDescription(evaluateExpression(rule.getDescription() != null ? rule.getDescription() : rule.getRuleName(), event, String.class));
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("AUTO");

        for (JournalRuleDetail ruleDetail : rule.getRuleDetails()) {
            JournalDetail detail = new JournalDetail();
            detail.setDrcrType(ruleDetail.getDrcrType());
            
            // ê¸ˆì•¡ ?‰ê?: SpEL???µí•´ ?˜ì‹ ê³„ì‚° ì§€??(?? ë¶€ê°€??10% ?ë™ ê³„ì‚°)
            BigDecimal amount = evaluateExpression(ruleDetail.getAmountExpression(), event, BigDecimal.class);
            detail.setAmount(amount);
            detail.setBaseAmount(amount);
            
            // ?ìš” ?‰ê?: ?´ë²¤???°ì´?°ë? ?ìš”???¬í•¨ (?? "${vendorName} ?€ê¸?ì§€ê¸?)
            detail.setDetailDescription(evaluateExpression(ruleDetail.getDescriptionExpression(), event, String.class));
            
            entry.addDetail(detail);
        }

        return entry;
    }

    /**
     * ?´ë²¤?¸ê? ?´ë‹¹ ë£°ì˜ ?ìš© ?€?ì¸ì§€ ì¡°ê±´??ê²€?¬í•©?ˆë‹¤.
     *
     * @param rule ê²€?¬í•  ê·œì¹™
     * @param event ë°œìƒ???´ë²¤???°ì´??
     * @return ëª¨ë“  ì¡°ê±´???¼ì¹˜?˜ë©´ true
     */
    public boolean matches(JournalRule rule, Map<String, Object> event) {
        for (JournalRuleCondition condition : rule.getConditions()) {
            Object eventValue = event.get(condition.getField());
            if (eventValue == null) return false;

            String stringValue = String.valueOf(eventValue);
            switch (condition.getOperator()) {
                case EQUALS:
                    if (!stringValue.equals(condition.getValue())) return false;
                    break;
                case NOT_EQUALS:
                    if (stringValue.equals(condition.getValue())) return false;
                    break;
                case CONTAINS:
                    if (!stringValue.contains(condition.getValue())) return false;
                    break;
                case STARTS_WITH:
                    if (!stringValue.startsWith(condition.getValue())) return false;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    /**
     * SpEL ?œí˜„?ì„ ?‰ê??˜ì—¬ ?¤ì œ ê°’ì„ ë°˜í™˜?©ë‹ˆ??
     */
    private <T> T evaluateExpression(String expression, Map<String, Object> event, Class<T> targetClass) {
        if (expression == null) return null;
        
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            // ?´ë²¤??ë§µì˜ ëª¨ë“  ??ê°’ì„ SpEL ë³€?˜ë¡œ ?±ë¡?˜ì—¬ #variable ?•íƒœë¡??‘ê·¼ ê°€?¥í•˜ê²???
            context.setVariables(event);
            return parser.parseExpression(expression).getValue(context, targetClass);
        } catch (Exception e) {
            // ?œí˜„?ì´ ?¨ìˆœ ë¬¸ì?´ì¼ ê²½ìš° ?ˆì™¸ë¥?ë¬´ì‹œ?˜ê³  ?ë³¸ ë°˜í™˜ ?œë„
            if (targetClass == String.class) return targetClass.cast(expression);
            throw new IllegalArgumentException("?œí˜„???‰ê? ?¤íŒ¨: " + expression, e);
        }
    }
}
