package com.ho.account.closing.domain.fx;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Explicit, dated account treatment for closing-rate FX valuation. Account category alone cannot
 * distinguish monetary assets from prepayments/inventory, so every candidate needs an active rule.
 * Historical-cost items are excluded; fair-value or other exceptions require a separate policy.
 */
public final class FxValuationPolicy {
    private static final Set<String> KNOWN_CATEGORIES =
            Set.of("ASSET", "ASSETS", "LIABILITY", "LIABILITIES", "EQUITY", "REVENUE", "EXPENSE", "EXPENSES");
    private final Map<String, List<Rule>> rulesByAccount;

    public FxValuationPolicy(List<Rule> rules) {
        Map<String, List<Rule>> grouped = new HashMap<>();
        for (Rule rule : rules) {
            if (rule == null) {
                throw new IllegalArgumentException("FX valuation policy must not contain null rules");
            }
            grouped.computeIfAbsent(rule.accountCode(), ignored -> new ArrayList<>()).add(rule);
        }
        grouped.replaceAll((account, accountRules) -> {
            accountRules.sort(Comparator.comparing(Rule::effectiveFrom));
            for (int index = 1; index < accountRules.size(); index++) {
                // Inclusive endpoints make a shared boundary date ambiguous, even for equal treatments.
                if (!accountRules.get(index).effectiveFrom().isAfter(accountRules.get(index - 1).effectiveTo())) {
                    throw new IllegalArgumentException("Overlapping FX valuation policies for account " + account);
                }
            }
            return List.copyOf(accountRules);
        });
        rulesByAccount = Map.copyOf(grouped);
    }

    public boolean isEligible(String accountCode, LocalDate valuationDate, String category, boolean fixedAsset) {
        Rule activeRule = rulesByAccount.getOrDefault(accountCode, List.of()).stream()
                .filter(rule -> !valuationDate.isBefore(rule.effectiveFrom())
                        && !valuationDate.isAfter(rule.effectiveTo()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "FX valuation policy is missing for account " + accountCode + " on " + valuationDate));
        if (category == null || !KNOWN_CATEGORIES.contains(category)) {
            throw new IllegalStateException("FX account category is missing or unsupported for account " + accountCode);
        }
        if (activeRule.treatment() == Treatment.HISTORICAL_COST) {
            return false;
        }
        // An explicit monetary setting cannot override contradictory master-data classification.
        if (fixedAsset || !(Set.of("ASSET", "ASSETS", "LIABILITY", "LIABILITIES").contains(category))) {
            throw new IllegalStateException("MONETARY FX policy conflicts with account classification for " + accountCode);
        }
        return true;
    }

    public enum Treatment { MONETARY, HISTORICAL_COST }

    public record Rule(String accountCode, LocalDate effectiveFrom, LocalDate effectiveTo, Treatment treatment) {
        public Rule {
            if (accountCode == null || accountCode.isBlank() || !accountCode.equals(accountCode.trim())) {
                throw new IllegalArgumentException("FX policy accountCode must be nonblank without surrounding whitespace");
            }
            if (effectiveFrom == null || effectiveTo == null || effectiveFrom.isAfter(effectiveTo)) {
                throw new IllegalArgumentException("FX policy dates must define an inclusive, ordered interval");
            }
            if (treatment == null) {
                throw new IllegalArgumentException("FX policy treatment must be explicit");
            }
        }
    }
}
