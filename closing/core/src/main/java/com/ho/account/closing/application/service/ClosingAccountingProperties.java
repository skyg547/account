package com.ho.account.closing.application.service;

import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.fx.FxValuationPolicy;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "account.closing.accounting")
public class ClosingAccountingProperties {

    private Map<ProvisionBatch.ProvisionType, EclAccountMapping> provisionRules = new LinkedHashMap<>();

    private String fxTranslationGainAccountCode = "72000"; // Default: 외화환산이익
    private String fxTranslationLossAccountCode = "92000"; // Default: 외화환산손실
    private String fxValuationReportingCurrencyCode = "KRW";
    private boolean autoPostAdjustments;
    private int apiFinancialRunMaxEvidenceRows = 10_000;
    private int apiFinancialRunMaxJournalCommands = 1_000;
    private List<FxValuationPolicy.Rule> fxValuationPolicies = List.of();
    private FxValuationPolicy fxValuationPolicy = new FxValuationPolicy(List.of());

    public List<FxValuationPolicy.Rule> getFxValuationPolicies() {
        return fxValuationPolicies;
    }

    public void setFxValuationPolicies(List<FxValuationPolicy.Rule> policies) {
        List<FxValuationPolicy.Rule> configured = policies == null ? List.of() : List.copyOf(policies);
        // Validate the whole dated configuration once; a missing rule still fails at the input gate.
        FxValuationPolicy validated = new FxValuationPolicy(configured);
        fxValuationPolicies = configured;
        fxValuationPolicy = validated;
    }

    public FxValuationPolicy fxValuationPolicy() {
        return fxValuationPolicy;
    }

    public String requireFxValuationReportingCurrencyCode() {
        if (!hasText(fxValuationReportingCurrencyCode)) {
            throw new IllegalStateException("FX valuation reporting currency is not configured: account.closing.accounting.fx-valuation-reporting-currency-code");
        }
        return fxValuationReportingCurrencyCode.trim().toUpperCase(Locale.ROOT);
    }

    public String getFxValuationReportingCurrencyCode() {
        return fxValuationReportingCurrencyCode;
    }

    public void setFxValuationReportingCurrencyCode(String fxValuationReportingCurrencyCode) {
        this.fxValuationReportingCurrencyCode = fxValuationReportingCurrencyCode;
    }

    public String getFxTranslationGainAccountCode() {
        return fxTranslationGainAccountCode;
    }

    public void setFxTranslationGainAccountCode(String fxTranslationGainAccountCode) {
        this.fxTranslationGainAccountCode = fxTranslationGainAccountCode;
    }

    public String getFxTranslationLossAccountCode() {
        return fxTranslationLossAccountCode;
    }

    public void setFxTranslationLossAccountCode(String fxTranslationLossAccountCode) {
        this.fxTranslationLossAccountCode = fxTranslationLossAccountCode;
    }

    /**
     * 결산 자동분개는 기본적으로 DRAFT로 남겨 검토받습니다.
     * 운영 통제가 별도로 승인한 환경에서만 true로 설정해 자동 승인/전기를 허용합니다.
     */
    public boolean isAutoPostAdjustments() {
        return autoPostAdjustments;
    }

    public void setAutoPostAdjustments(boolean autoPostAdjustments) {
        this.autoPostAdjustments = autoPostAdjustments;
    }

    public EclAccountMapping requireEclAccountMapping() {
        return requireAccountMapping(
                "provision-rules." + ProvisionBatch.ProvisionType.ECL.name(),
                provisionRules.get(ProvisionBatch.ProvisionType.ECL));
    }

    public Map<ProvisionBatch.ProvisionType, EclAccountMapping> getProvisionRules() {
        return provisionRules;
    }

    public void setProvisionRules(
            Map<ProvisionBatch.ProvisionType, EclAccountMapping> provisionRules) {
        this.provisionRules = provisionRules != null
                ? new LinkedHashMap<>(provisionRules)
                : new LinkedHashMap<>();
    }

    public int getApiFinancialRunMaxEvidenceRows() {
        return requirePositive(apiFinancialRunMaxEvidenceRows, "api-financial-run-max-evidence-rows");
    }

    public void setApiFinancialRunMaxEvidenceRows(int apiFinancialRunMaxEvidenceRows) {
        this.apiFinancialRunMaxEvidenceRows = requirePositive(
                apiFinancialRunMaxEvidenceRows, "api-financial-run-max-evidence-rows");
    }

    /** Also bounds ECL summary groups conservatively before per-group source lookups. */
    public int getApiFinancialRunMaxJournalCommands() {
        return requirePositive(apiFinancialRunMaxJournalCommands, "api-financial-run-max-journal-commands");
    }

    public void setApiFinancialRunMaxJournalCommands(int apiFinancialRunMaxJournalCommands) {
        this.apiFinancialRunMaxJournalCommands = requirePositive(
                apiFinancialRunMaxJournalCommands, "api-financial-run-max-journal-commands");
    }

    private EclAccountMapping requireAccountMapping(String mappingPath, EclAccountMapping mapping) {
        if (mapping == null) {
            throw new IllegalStateException(
                    "Closing account mapping is not configured: account.closing.accounting." + mappingPath);
        }
        mapping.validate(mappingPath);
        return mapping;
    }

    private int requirePositive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "account.closing.accounting." + propertyName + " must be positive");
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Fallback ECL account mapping when an allowance summary omits an account code. */
    public static class EclAccountMapping {
        private String debitAccountCode;
        private String creditAccountCode;

        public String getDebitAccountCode() {
            return debitAccountCode;
        }

        public void setDebitAccountCode(String debitAccountCode) {
            this.debitAccountCode = debitAccountCode;
        }

        public String getCreditAccountCode() {
            return creditAccountCode;
        }

        public void setCreditAccountCode(String creditAccountCode) {
            this.creditAccountCode = creditAccountCode;
        }

        private void validate(String rulePath) {
            if (!hasText(debitAccountCode)) {
                throw new IllegalStateException("Closing debit account is not configured: account.closing.accounting." + rulePath);
            }
            if (!hasText(creditAccountCode)) {
                throw new IllegalStateException("Closing credit account is not configured: account.closing.accounting." + rulePath);
            }
        }

        private boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
