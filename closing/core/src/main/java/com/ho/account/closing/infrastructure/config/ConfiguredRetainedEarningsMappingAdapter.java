package com.ho.account.closing.infrastructure.config;

import com.ho.account.closing.application.port.out.RetainedEarningsMappingPort;
import com.ho.account.closing.domain.ApprovedRetainedEarningsMapping;
import com.ho.account.closing.infrastructure.config.AnnualClosingConfigurationProperties.FiscalYearRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredRetainedEarningsMappingAdapter implements RetainedEarningsMappingPort {

    private static final String PREFIX = "account.closing.annual";
    private static final int MIN_FISCAL_YEAR = 1900;
    private static final int MAX_FISCAL_YEAR = 9999;

    private final String legalEntityCode;
    private final List<FiscalYearRule> mappings;

    public ConfiguredRetainedEarningsMappingAdapter(AnnualClosingConfigurationProperties properties) {
        Objects.requireNonNull(properties, "annual closing configuration properties must not be null");
        this.legalEntityCode = properties.getLegalEntityCode();
        this.mappings = Collections.unmodifiableList(new ArrayList<>(properties.getMappings()));
    }

    @Override
    public ApprovedRetainedEarningsMapping requireForYear(int fiscalYear) {
        requireSupportedRequestedYear(fiscalYear);
        requireText(legalEntityCode, PREFIX + ".legal-entity-code");
        if (mappings.isEmpty()) {
            throw invalid(PREFIX + ".mappings", "must contain an exact fiscal-year rule");
        }

        Map<Integer, ApprovedRetainedEarningsMapping> approvedByYear = new HashMap<>();
        for (int index = 0; index < mappings.size(); index++) {
            FiscalYearRule rule = mappings.get(index);
            String rulePath = PREFIX + ".mappings[" + index + "]";
            ApprovedRetainedEarningsMapping approved = toApprovedMapping(rule, rulePath);
            if (approvedByYear.putIfAbsent(approved.fiscalYear(), approved) != null) {
                throw invalid(rulePath + ".fiscal-year", "duplicates another configured fiscal year");
            }
        }

        ApprovedRetainedEarningsMapping mapping = approvedByYear.get(fiscalYear);
        if (mapping == null) {
            throw invalid(PREFIX + ".mappings[].fiscal-year", "has no exact rule for the requested fiscal year");
        }
        return mapping;
    }

    private ApprovedRetainedEarningsMapping toApprovedMapping(FiscalYearRule rule, String rulePath) {
        if (rule == null) {
            throw invalid(rulePath, "must be configured");
        }
        if (rule.fiscalYear() == null) {
            throw invalid(rulePath + ".fiscal-year", "must be configured");
        }
        if (rule.fiscalYear() < MIN_FISCAL_YEAR || rule.fiscalYear() > MAX_FISCAL_YEAR) {
            throw invalid(rulePath + ".fiscal-year", "must be between 1900 and 9999");
        }
        requireText(rule.accountCode(), rulePath + ".account-code");
        if (!Boolean.TRUE.equals(rule.postable())) {
            throw invalid(rulePath + ".postable", "must be explicitly true");
        }

        // Approval evidence makes every destination change traceable to control-plane review.
        requireText(rule.approvedBy(), rulePath + ".approved-by");
        requireText(rule.changeReference(), rulePath + ".change-reference");

        try {
            // Journal contracts carry no entity dimension, so one runtime can safely bind only one entity.
            // The domain constructor alone normalizes configured identifiers and audit metadata.
            return new ApprovedRetainedEarningsMapping(
                    legalEntityCode,
                    rule.fiscalYear(),
                    rule.accountCode(),
                    rule.postable(),
                    rule.approvedBy(),
                    rule.changeReference());
        } catch (IllegalArgumentException exception) {
            throw invalid(rulePath, "is not a valid approved retained-earnings rule");
        }
    }

    private void requireSupportedRequestedYear(int fiscalYear) {
        if (fiscalYear < MIN_FISCAL_YEAR || fiscalYear > MAX_FISCAL_YEAR) {
            throw invalid(PREFIX + ".mappings[].fiscal-year", "supports fiscal years from 1900 through 9999");
        }
    }

    private void requireText(String value, String path) {
        if (value == null || value.isBlank()) {
            throw invalid(path, "must be configured with nonblank text");
        }
    }

    private IllegalStateException invalid(String path, String reason) {
        return new IllegalStateException("Invalid annual closing configuration at " + path + ": " + reason);
    }
}
