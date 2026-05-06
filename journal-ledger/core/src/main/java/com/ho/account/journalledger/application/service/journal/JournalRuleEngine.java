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
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.domain.model.Department;
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
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 자동분개란 비즈니스 이벤트(예: 매입, 지출결의, 리스 납부)가 발생하면
 * 사람이 직접 전표를 입력하지 않아도 시스템이 자동으로 적절한 전표를 생성하는 기능입니다.
 *
 * 예시:
 *   이벤트: 매입 청구서 등록 (금액: 100,000원, 거래처: BP001)
 *   → 자동 생성 전표:
 *     차변: 매입비용(51000) 100,000원
 *     대변: 매입채무(21100) 100,000원
 *
 * 자동분개 규칙(JournalRule)은 DB에 등록되어 있으며,
 * 이벤트 유형(eventType), 금액 범위, 거래처 조건 등에 따라 적용 규칙이 결정됩니다.
 * 규칙 우선순위(priority 필드)가 낮을수록 먼저 적용됩니다.
 *
 * 주요 사용처:
 *   - 매입채무 모듈: 매입 청구서 등록 시 자동 전표
 *   - 지출결의 모듈: 지출결의 승인 시 자동 전표
 *   - 리스 모듈: 월별 리스료 납부 자동 전표
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * JournalEntryService.createJournalEntryFromEvent()에서 호출됩니다.
 *
 * 동작 흐름:
 *   1. 이벤트 데이터(Map)에서 eventType 추출
 *   2. DB에서 해당 eventType에 매칭되는 JournalRule 목록 조회
 *   3. 각 규칙의 조건(JournalRuleCondition) 평가 (금액, 거래처 등 조건 검사)
 *   4. 조건에 맞는 규칙의 분개 라인(JournalRuleDetail) 기반으로 JournalEntry + JournalDetail 생성
 *   5. 생성된 전표 반환 (규칙 미매칭 시 Optional.empty())
 *
 * [현재 구현 상태]
 * 현재 이 클래스는 스켈레톤(뼈대) 구현으로, 실제 규칙 매칭 로직은
 * JournalRuleRepository, JournalRuleCondition 평가 로직과 함께 추후 완성이 필요합니다.
 * 지금은 빈 DRAFT 전표만 반환합니다.
 *
 * 추후 구현 시 참고:
 *   - JournalRule.priority: 낮을수록 먼저 적용 (priority=1이 가장 먼저)
 *   - JournalRuleCondition: 금액 범위, 거래처, 계정과목 조건 평가
 *   - JournalRuleDetail: 차변/대변 계정과목 및 금액 표현식 (${}  문법)
 *   - JournalRule.validFrom/validTo: 유효 기간 필터링 필요
 * ─────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class JournalRuleEngine {

    private static final Pattern BRACED_PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern HASH_PLACEHOLDER = Pattern.compile("#([A-Za-z0-9_.]+)");

    private final JournalRuleRepository journalRuleRepository;
    private final JournalRuleConditionRepository journalRuleConditionRepository;
    private final JournalRuleDetailRepository journalRuleDetailRepository;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final CurrencyPersistencePort currencyPersistencePort;

    /**
     * 이벤트 데이터를 기반으로 자동 전표를 생성합니다.
     *
     * [업무 설명]
     * 외부 모듈(매입, 지출결의, 리스 등)에서 이벤트가 발생하면,
     * 이 메서드가 이벤트 데이터를 분석하여 적절한 전표를 자동으로 생성합니다.
     * 규칙이 매칭되지 않으면 빈 Optional을 반환하므로, 호출자가 예외 처리를 해야 합니다.
     *
     * [개발 설명]
     * eventData 키 예시:
     *   - "eventType"    : 이벤트 유형 (예: "PURCHASE", "EXPENDITURE", "LEASE_PAYMENT")
     *   - "amount"       : 거래 금액 (BigDecimal 또는 Number 타입)
     *   - "vendorId"     : 거래처 ID (String)
     *   - "departmentId" : 귀속 부서 ID (String)
     *   - "currencyCode" : 거래 통화 (예: "KRW", "USD")
     *
     * @param eventData      이벤트 데이터 (키-값 쌍의 Map)
     * @param accountingDate 회계 반영일 (전표의 accountingDate와 slipDate에 사용)
     * @return 생성된 전표 Optional (규칙 미매칭 시 empty)
     */
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
        resolveCurrency(eventData).ifPresent(entry::setCurrency);

        List<JournalRuleDetail> sortedRuleDetails = new ArrayList<>(ruleDetails);
        sortedRuleDetails.sort(Comparator.comparing(JournalRuleDetail::getId, Comparator.nullsLast(Long::compareTo)));

        for (JournalRuleDetail ruleDetail : sortedRuleDetails) {
            JournalDetail detail = new JournalDetail();

            String accountCode = resolveStringExpression(ruleDetail.getAccountSubjectCodeExpression(), eventData, true);
            AccountSubject accountSubject = accountSubjectPersistencePort.findByCode(accountCode)
                    .orElseThrow(() -> new IllegalArgumentException("Account subject not found: " + accountCode));

            BigDecimal amount = evaluateAmountExpression(ruleDetail.getAmountExpression(), eventData);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Rule detail amount must be positive: " + ruleDetail.getId());
            }

            detail.setSide(parseSide(ruleDetail.getDrcrType()));
            detail.setAccountSubject(accountSubject);
            detail.setAmount(amount);
            detail.setBaseAmount(amount);
            detail.setDetailDescription(resolveLineDescription(ruleDetail, entry, eventData));
            detail.setAuditUser("SYSTEM");

            String departmentCode = resolveStringExpression(ruleDetail.getDepartmentCodeExpression(), eventData, false);
            if (departmentCode != null && !departmentCode.isBlank()) {
                Department department = departmentPersistencePort.findActiveByCode(departmentCode)
                        .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentCode));
                detail.setDepartment(department);
            }

            String businessPartnerCode = resolveStringExpression(ruleDetail.getBusinessPartnerCodeExpression(), eventData, false);
            if (businessPartnerCode != null && !businessPartnerCode.isBlank()) {
                BusinessPartner businessPartner = businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode)
                        .orElseThrow(() -> new IllegalArgumentException("Business partner not found: " + businessPartnerCode));
                detail.setBusinessPartner(businessPartner);
            }

            entry.addDetail(detail);
        }

        return entry;
    }

    private Optional<Currency> resolveCurrency(Map<String, Object> eventData) {
        Optional<String> requestedCode = firstNonBlankValue(eventData, "currencyCode", "currency");
        if (requestedCode.isPresent()) {
            return currencyPersistencePort.findByCode(requestedCode.get());
        }
        return currencyPersistencePort.findByCode("KRW");
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
