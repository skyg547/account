package com.ho.account.closing.domain.fx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static com.ho.account.closing.domain.fx.FxValuationPolicy.Treatment.*;
import static org.assertj.core.api.Assertions.*;

class FxValuationPolicyTest {
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 6, 30);

    @ParameterizedTest
    @ValueSource(strings = {"ASSET", "ASSETS", "LIABILITY", "LIABILITIES"})
    void explicitMonetaryPolicyAcceptsCanonicalAndLegacyAssetLiabilityCategories(String category) {
        var policy = policy(MONETARY);
        assertThat(policy.isEligible("A", START, category, false)).isTrue();
        assertThat(policy.isEligible("A", END, category, false)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"REVENUE,false", "EXPENSE,false", "EXPENSES,false", "EQUITY,false", "ASSETS,true"})
    void historicalItemsAreExcludedAndCannotBeOverriddenByMonetaryTreatment(String category, boolean fixedAsset) {
        assertThat(policy(HISTORICAL_COST).isEligible("A", START, category, fixedAsset)).isFalse();
        assertThatThrownBy(() -> policy(MONETARY).isEligible("A", START, category, fixedAsset))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("conflicts");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN"})
    void InsufficientMetadataFailsForBothTreatments(String category) {
        for (var treatment : FxValuationPolicy.Treatment.values()) {
            assertThatThrownBy(() -> policy(treatment).isEligible("A", START, category, false))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("category");
        }
    }

    @Test
    void missingRulesDatesAndGapsFailClosed() {
        var policy = new FxValuationPolicy(List.of(rule(START, END, MONETARY),
                rule(END.plusDays(2), END.plusMonths(2), HISTORICAL_COST)));
        for (LocalDate date : List.of(START.minusDays(1), END.plusDays(1), END.plusMonths(2).plusDays(1))) {
            assertThatThrownBy(() -> policy.isEligible("A", date, "ASSETS", false))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("policy is missing");
        }
        assertThatThrownBy(() -> policy.isEligible("B", START, "ASSETS", false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("policy is missing");
        assertThat(policy.isEligible("A", END.plusDays(2), "ASSETS", false)).isFalse();
    }

    @Test
    void policyCanChangeTreatmentOnFollowingDayWithoutOverlap() {
        var policy = new FxValuationPolicy(List.of(rule(START, END, MONETARY),
                rule(END.plusDays(1), END.plusMonths(1), HISTORICAL_COST)));
        assertThat(policy.isEligible("A", END, "ASSETS", false)).isTrue();
        assertThat(policy.isEligible("A", END.plusDays(1), "ASSETS", false)).isFalse();
    }

    @Test
    void equalAndConflictingOverlapsAreRejectedIncludingInclusiveBoundary() {
        for (var treatment : FxValuationPolicy.Treatment.values()) {
            assertThatThrownBy(() -> new FxValuationPolicy(List.of(rule(END, END.plusMonths(1), treatment),
                    rule(START, END, MONETARY))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Overlapping");
        }
    }

    @Test
    void malformedPoliciesCannotBeConstructed() {
        assertThatThrownBy(() -> rule(null, END, MONETARY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rule(START, null, MONETARY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rule(END, START, MONETARY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rule(START, END, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxValuationPolicy.Rule(" ", START, END, MONETARY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private FxValuationPolicy policy(FxValuationPolicy.Treatment treatment) {
        return new FxValuationPolicy(List.of(rule(START, END, treatment)));
    }

    private FxValuationPolicy.Rule rule(LocalDate from, LocalDate to, FxValuationPolicy.Treatment treatment) {
        return new FxValuationPolicy.Rule("A", from, to, treatment);
    }
}
