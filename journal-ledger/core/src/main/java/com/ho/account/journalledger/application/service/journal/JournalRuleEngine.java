package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.repository.JournalRuleConditionRepository;
import com.ho.account.journalledger.domain.journal.repository.JournalRuleDetailRepository;
import com.ho.account.journalledger.domain.journal.repository.JournalRuleRepository;
import com.ho.account.journalledger.domain.journal.domain.ConditionOperator;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 자동분개 규칙 엔진 (Journal Rule Engine).
 * 타 모듈(ERP/Banking)에서 발생한 비즈니스 이벤트 데이터를 기반으로,
 * 룰 기반 분개(Journalizing)를 수행하여 회계 전표(JournalEntry)를 생성하는 핵심 도메인 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 엔진은 회계 시스템의 '자동 번역기' 역할을 합니다.
 * 영업팀, 구매팀, 은행 시스템 등에서 발생한 날것의 거래 데이터(JSON 형태)가 들어오면,
 * 미리 등록된 분개 규칙(JournalRule)들을 쭉 훑어보고 조건이 맞는 것을 찾습니다.
 * 조건이 맞으면, "차변엔 무슨 계정, 대변엔 무슨 계정, 금액은 어떻게 계산해서 넣어라"는 
 * 규칙에 따라 완벽한 회계 전표를 뚝딱 만들어냅니다.
 */
@Service
@RequiredArgsConstructor
public class JournalRuleEngine {

    private static final Pattern BRACED_PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern HASH_PLACEHOLDER = Pattern.compile("#([A-Za-z0-9_.]+)");

    private final JournalRuleRepository journalRuleRepository;
    private final JournalRuleConditionRepository journalRuleConditionRepository;
    private final JournalRuleDetailRepository journalRuleDetailRepository;

    @Transactional(readOnly = true)
    public Optional<JournalEntry> generateJournalEntry(Map<String, Object> eventData, LocalDate accountingDate) {
        if (eventData == null || eventData.isEmpty()) {
            return Optional.empty();
        }

        List<JournalRule> candidates = loadCandidateRules(eventData, accountingDate);
        for (JournalRule rule : candidates) {
            if (!matchesRuleConditions(rule, eventData)) {
                continue;
            }

            List<JournalRuleDetail> ruleDetails = journalRuleDetailRepository.findByJournalRuleId(rule.getId());
            if (ruleDetails.isEmpty()) {
                continue;
            }

            JournalEntry entry = buildEntry(rule, ruleDetails, eventData, accountingDate);
            if (!entry.getDetails().isEmpty()) {
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    private List<JournalRule> loadCandidateRules(Map<String, Object> eventData, LocalDate accountingDate) {
        Optional<String> requestedRuleCode = firstNonBlankValue(eventData, "ruleCode", "journalRuleCode");

        return journalRuleRepository.findByIsActiveTrueOrderByPriorityAscVersionDesc().stream()
                .filter(JournalRule::isActive)
                .filter(rule -> isValidAt(rule, accountingDate))
                .filter(rule -> requestedRuleCode
                        .map(code -> code.equalsIgnoreCase(rule.getRuleCode()))
                        .orElse(true))
                .sorted(Comparator.comparingInt(JournalRule::getPriority))
                .toList();
    }

    private boolean isValidAt(JournalRule rule, LocalDate accountingDate) {
        LocalDate validFrom = rule.getValidFrom();
        LocalDate validTo = rule.getValidTo();

        boolean afterStart = validFrom == null || !accountingDate.isBefore(validFrom);
        boolean beforeEnd = validTo == null || !accountingDate.isAfter(validTo);
        return afterStart && beforeEnd;
    }

    private boolean matchesRuleConditions(JournalRule rule, Map<String, Object> eventData) {
        List<JournalRuleCondition> conditions = journalRuleConditionRepository.findByJournalRuleId(rule.getId());
        if (conditions.isEmpty()) {
            return true;
        }

        return conditions.stream().allMatch(condition -> matchesCondition(condition, eventData));
    }

    private boolean matchesCondition(JournalRuleCondition condition, Map<String, Object> eventData) {
        Object actual = resolveEventValue(eventData, condition.getField());
        String expected = condition.getValue();

        if (condition.getOperator() == ConditionOperator.NOT_EQUALS && actual == null) {
            return expected != null && !expected.isBlank();
        }

        if (actual == null) {
            return false;
        }

        return switch (condition.getOperator()) {
            case EQUALS -> compareAsNumber(actual, expected)
                    .map(result -> result == 0)
                    .orElseGet(() -> String.valueOf(actual).equals(expected));
            case NOT_EQUALS -> compareAsNumber(actual, expected)
                    .map(result -> result != 0)
                    .orElseGet(() -> !String.valueOf(actual).equals(expected));
            case STARTS_WITH -> String.valueOf(actual).startsWith(expected);
            case ENDS_WITH -> String.valueOf(actual).endsWith(expected);
            case CONTAINS -> String.valueOf(actual).contains(expected);
            case GREATER_THAN -> compareAsNumber(actual, expected).map(result -> result > 0).orElse(false);
            case GREATER_THAN_OR_EQUAL -> compareAsNumber(actual, expected).map(result -> result >= 0).orElse(false);
            case LESS_THAN -> compareAsNumber(actual, expected).map(result -> result < 0).orElse(false);
            case LESS_THAN_OR_EQUAL -> compareAsNumber(actual, expected).map(result -> result <= 0).orElse(false);
        };
    }

    private Optional<Integer> compareAsNumber(Object actual, String expected) {
        try {
            BigDecimal left = toBigDecimal(actual);
            BigDecimal right = new BigDecimal(expected);
            return Optional.of(left.compareTo(right));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(String.valueOf(value).trim());
    }

    private JournalEntry buildEntry(
            JournalRule rule,
            List<JournalRuleDetail> ruleDetails,
            Map<String, Object> eventData,
            LocalDate accountingDate) {

        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(accountingDate);
        entry.setAccountingDate(accountingDate);
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setDescription(resolveEntryDescription(rule, eventData));
        entry.setCreatedBy("SYSTEM");
        entry.setAuditUser("SYSTEM");
        entry.setLineageSourceType(firstNonBlankValue(eventData, "lineageSourceType").orElse(rule.getRuleCode()));
        entry.setLineageSourceId(firstNonBlankValue(eventData, "lineageSourceId", "sourceId", "documentId", "assetCode", "contractNo").orElse(null));
        
        String currencyCode = resolveCurrencyCode(eventData);
        entry.setCurrencyCode(currencyCode);

        List<JournalRuleDetail> sortedRuleDetails = new ArrayList<>(ruleDetails);
        sortedRuleDetails.sort(Comparator.comparing(JournalRuleDetail::getId, Comparator.nullsLast(Long::compareTo)));

        for (JournalRuleDetail ruleDetail : sortedRuleDetails) {
            JournalDetail detail = new JournalDetail();

            String accountCode = resolveStringExpression(ruleDetail.getAccountSubjectCodeExpression(), eventData, true);
            if (accountCode == null || accountCode.isBlank()) {
                throw new IllegalArgumentException("Account code resolution failed for rule detail: " + ruleDetail.getId());
            }

            BigDecimal amount = evaluateAmountExpression(ruleDetail.getAmountExpression(), eventData);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Rule detail amount must be positive: " + ruleDetail.getId());
            }

            detail.setSide(parseSide(ruleDetail.getDrcrType()));
            detail.setAccountCode(accountCode);
            detail.setAmount(amount);
            detail.setBaseAmount(amount);
            detail.setDetailDescription(resolveLineDescription(ruleDetail, entry, eventData));
            detail.setAuditUser("SYSTEM");

            String departmentCode = resolveStringExpression(ruleDetail.getDepartmentCodeExpression(), eventData, false);
            if (departmentCode != null && !departmentCode.isBlank()) {
                detail.setDepartmentCode(departmentCode);
            }

            String businessPartnerCode = resolveStringExpression(ruleDetail.getBusinessPartnerCodeExpression(), eventData, false);
            if (businessPartnerCode != null && !businessPartnerCode.isBlank()) {
                detail.setBusinessPartnerCode(businessPartnerCode);
            }

            entry.addDetail(detail);
        }

        return entry;
    }

    private String resolveCurrencyCode(Map<String, Object> eventData) {
        return firstNonBlankValue(eventData, "currencyCode", "currency").orElse("KRW");
    }

    private String resolveEntryDescription(JournalRule rule, Map<String, Object> eventData) {
        return firstNonBlankValue(eventData, "description", "memo", "title")
                .orElse(rule.getRuleName());
    }

    private String resolveLineDescription(JournalRuleDetail ruleDetail, JournalEntry entry, Map<String, Object> eventData) {
        String resolved = resolveStringExpression(ruleDetail.getDescriptionExpression(), eventData, false);
        if (resolved == null || resolved.isBlank()) {
            return entry.getDescription();
        }
        return resolved;
    }

    private JournalSide parseSide(String drcrType) {
        if (drcrType == null || drcrType.isBlank()) {
            throw new IllegalArgumentException("drcrType is required");
        }
        return JournalSide.valueOf(drcrType.trim().toUpperCase());
    }

    private BigDecimal evaluateAmountExpression(String expression, Map<String, Object> eventData) {
        String interpolated = interpolateExpression(expression, eventData, true);
        return new BigDecimalExpressionParser(interpolated).parse();
    }

    private String resolveStringExpression(String expression, Map<String, Object> eventData, boolean required) {
        if (expression == null || expression.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("Expression is required");
            }
            return null;
        }

        if (expression.contains("+")) {
            return evaluateStringConcat(expression, eventData, required);
        }

        String interpolated = interpolateExpression(expression, eventData, required).trim();
        return stripQuotes(interpolated);
    }

    private String evaluateStringConcat(String expression, Map<String, Object> eventData, boolean required) {
        StringBuilder builder = new StringBuilder();
        for (String token : expression.split("\\+")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if ((trimmed.startsWith("'") && trimmed.endsWith("'"))
                    || (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
                builder.append(stripQuotes(trimmed));
                continue;
            }

            Object value = resolveEventValue(eventData, trimmed);
            if (value == null) {
                if (required) {
                    throw new IllegalArgumentException("Missing event value: " + trimmed);
                }
                continue;
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String interpolateExpression(String expression, Map<String, Object> eventData, boolean required) {
        String resolvedBraced = replaceBracedPlaceholders(expression, eventData, required);
        return replaceHashPlaceholders(resolvedBraced, eventData, required);
    }

    private String replaceBracedPlaceholders(String expression, Map<String, Object> eventData, boolean required) {
        Matcher matcher = BRACED_PLACEHOLDER.matcher(expression);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            Object value = resolveEventValue(eventData, placeholder);
            if (value == null) {
                if (required) {
                    throw new IllegalArgumentException("Missing event value: " + placeholder);
                }
                matcher.appendReplacement(buffer, "");
                continue;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceHashPlaceholders(String expression, Map<String, Object> eventData, boolean required) {
        Matcher matcher = HASH_PLACEHOLDER.matcher(expression);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            Object value = resolveEventValue(eventData, placeholder);
            if (value == null) {
                if (required) {
                    throw new IllegalArgumentException("Missing event value: " + placeholder);
                }
                matcher.appendReplacement(buffer, "");
                continue;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private Object resolveEventValue(Map<String, Object> eventData, String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }

        String normalizedReference = normalizeReference(reference);
        Object value = lookupValue(eventData, normalizedReference);
        if (value != null) {
            return value;
        }

        if (normalizedReference.startsWith("transaction.")) {
            value = lookupValue(eventData, normalizedReference.substring("transaction.".length()));
            if (value != null) {
                return value;
            }
        }

        if (normalizedReference.startsWith("eventData.")) {
            value = lookupValue(eventData, normalizedReference.substring("eventData.".length()));
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Object lookupValue(Map<String, Object> eventData, String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }

        if (eventData.containsKey(reference)) {
            return eventData.get(reference);
        }

        String[] path = reference.split("\\.");
        Object current = eventData;
        for (String pathSegment : path) {
            if (!(current instanceof Map<?, ?> currentMap) || !currentMap.containsKey(pathSegment)) {
                return null;
            }
            current = currentMap.get(pathSegment);
        }

        return current;
    }

    private String normalizeReference(String reference) {
        String normalized = reference.trim();
        if (normalized.startsWith("${") && normalized.endsWith("}")) {
            normalized = normalized.substring(2, normalized.length() - 1).trim();
        }
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private Optional<String> firstNonBlankValue(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            String stringValue = String.valueOf(value).trim();
            if (!stringValue.isBlank()) {
                return Optional.of(stringValue);
            }
        }
        return Optional.empty();
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        if ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static final class BigDecimalExpressionParser {
        private final String expression;
        private int index;

        private BigDecimalExpressionParser(String expression) {
            this.expression = expression;
            this.index = 0;
        }

        private BigDecimal parse() {
            BigDecimal value = parseExpression();
            skipWhitespace();
            if (index != expression.length()) {
                throw new IllegalArgumentException("Invalid amount expression: " + expression);
            }
            return value;
        }

        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value = value.add(parseTerm());
                } else if (match('-')) {
                    value = value.subtract(parseTerm());
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parseFactor();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value = value.multiply(parseFactor());
                } else if (match('/')) {
                    value = value.divide(parseFactor(), 12, RoundingMode.HALF_UP);
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseFactor() {
            skipWhitespace();

            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return parseFactor().negate();
            }
            if (match('(')) {
                BigDecimal value = parseExpression();
                if (!match(')')) {
                    throw new IllegalArgumentException("Missing closing parenthesis: " + expression);
                }
                return value;
            }
            return parseNumber();
        }

        private BigDecimal parseNumber() {
            skipWhitespace();
            int start = index;
            while (index < expression.length()) {
                char ch = expression.charAt(index);
                if (Character.isDigit(ch) || ch == '.') {
                    index++;
                    continue;
                }
                break;
            }
            if (start == index) {
                throw new IllegalArgumentException("Invalid number in expression: " + expression);
            }
            return new BigDecimal(expression.substring(start, index));
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (index < expression.length() && expression.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < expression.length() && Character.isWhitespace(expression.charAt(index))) {
                index++;
            }
        }
    }
}