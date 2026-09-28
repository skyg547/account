package com.ho.account.closing.infrastructure.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Deployment-controlled annual-closing destinations.
 *
 * <p>Journal commands have no legal-entity dimension, so each runtime accepts one required
 * {@code legalEntityCode}. Missing values intentionally have no usable fallback.</p>
 */
@Component
@ConfigurationProperties(prefix = "account.closing.annual")
public class AnnualClosingConfigurationProperties {

    private String legalEntityCode;
    private List<FiscalYearRule> mappings = List.of();

    public AnnualClosingConfigurationProperties() {
    }

    public AnnualClosingConfigurationProperties(String legalEntityCode, List<FiscalYearRule> mappings) {
        this.legalEntityCode = legalEntityCode;
        setMappings(mappings);
    }

    public String getLegalEntityCode() {
        return legalEntityCode;
    }

    public void setLegalEntityCode(String legalEntityCode) {
        this.legalEntityCode = legalEntityCode;
    }

    public List<FiscalYearRule> getMappings() {
        return mappings;
    }

    public void setMappings(List<FiscalYearRule> mappings) {
        this.mappings = mappings == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(mappings));
    }

    /** Approval evidence is stored beside each exact-year destination instead of inferred at runtime. */
    public record FiscalYearRule(
            Integer fiscalYear,
            String accountCode,
            Boolean postable,
            String approvedBy,
            String changeReference) {
    }
}
