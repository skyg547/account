package com.ho.account.closing.application.service;

import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.domain.ValuationBatch;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "account.closing.accounting")
public class ClosingAccountingProperties {

    private Map<ValuationBatch.ValuationType, AutomatedJournalRule> valuationRules = new LinkedHashMap<>();
    private Map<ProvisionBatch.ProvisionType, AutomatedJournalRule> provisionRules = new LinkedHashMap<>();

    private String fxTranslationGainAccountCode = "72000"; // Default: 외화환산이익
    private String fxTranslationLossAccountCode = "92000"; // Default: 외화환산손실

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

    public AutomatedJournalRule requireValuationRule(ValuationBatch.ValuationType valuationType) {
        return requireRule("valuation-rules." + valuationType.name(), valuationRules.get(valuationType));
    }

    public AutomatedJournalRule requireProvisionRule(ProvisionBatch.ProvisionType provisionType) {
        return requireRule("provision-rules." + provisionType.name(), provisionRules.get(provisionType));
    }

    public Map<ValuationBatch.ValuationType, AutomatedJournalRule> getValuationRules() {
        return valuationRules;
    }

    public void setValuationRules(Map<ValuationBatch.ValuationType, AutomatedJournalRule> valuationRules) {
        this.valuationRules = valuationRules != null ? valuationRules : new LinkedHashMap<>();
    }

    public Map<ProvisionBatch.ProvisionType, AutomatedJournalRule> getProvisionRules() {
        return provisionRules;
    }

    public void setProvisionRules(Map<ProvisionBatch.ProvisionType, AutomatedJournalRule> provisionRules) {
        this.provisionRules = provisionRules != null ? provisionRules : new LinkedHashMap<>();
    }

    private AutomatedJournalRule requireRule(String rulePath, AutomatedJournalRule rule) {
        if (rule == null) {
            throw new IllegalStateException("Closing accounting rule is not configured: account.closing.accounting." + rulePath);
        }
        rule.validate(rulePath);
        return rule;
    }

    public static class AutomatedJournalRule {
        private String debitAccountCode;
        private String creditAccountCode;
        private BigDecimal amount;

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

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        private void validate(String rulePath) {
            if (!hasText(debitAccountCode)) {
                throw new IllegalStateException("Closing debit account is not configured: account.closing.accounting." + rulePath);
            }
            if (!hasText(creditAccountCode)) {
                throw new IllegalStateException("Closing credit account is not configured: account.closing.accounting." + rulePath);
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Closing amount must be positive: account.closing.accounting." + rulePath);
            }
        }

        private boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
