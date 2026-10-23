package com.ho.account.closing.infrastructure.config;

import com.ho.account.closing.application.port.out.RetainedEarningsMappingPort;
import com.ho.account.closing.domain.ApprovedRetainedEarningsMapping;
import com.ho.account.closing.infrastructure.config.AnnualClosingConfigurationProperties.FiscalYearRule;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguredRetainedEarningsMappingAdapterTest {

    private static final int YEAR = 2026;

    @Test
    void resolvesExactApprovedRuleAndNormalizesItsControlIdentityFields() {
        ConfiguredRetainedEarningsMappingAdapter adapter = adapter(
                " ENTITY-01 ", rule(YEAR, " 35000 ", true, " controller ", " CHG-776 "));

        ApprovedRetainedEarningsMapping mapping = adapter.requireForYear(YEAR);

        assertThat(mapping.legalEntityCode()).isEqualTo("ENTITY-01");
        assertThat(mapping.fiscalYear()).isEqualTo(YEAR);
        assertThat(mapping.accountCode()).isEqualTo("35000");
        assertThat(mapping.postable()).isTrue();
        assertThat(mapping.approvedBy()).isEqualTo("controller");
        assertThat(mapping.changeReference()).isEqualTo("CHG-776");
        assertThat(mapping.controlIdentity()).contains("ENTITY-01", "35000", "controller", "CHG-776");
    }

    @Test
    void rejectsMissingExactFiscalYearRule() {
        ConfiguredRetainedEarningsMappingAdapter adapter = adapter(
                "ENTITY-01", rule(2025, "35000", true, "controller", "CHG-775"));

        assertThatThrownBy(() -> adapter.requireForYear(YEAR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no exact rule");
    }

    @Test
    void rejectsDuplicateFiscalYearRules() {
        ConfiguredRetainedEarningsMappingAdapter adapter = adapter(
                "ENTITY-01",
                rule(YEAR, "35000", true, "controller-a", "CHG-776-A"),
                rule(YEAR, "35001", true, "controller-b", "CHG-776-B"));

        assertThatThrownBy(() -> adapter.requireForYear(YEAR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicates another configured fiscal year");
    }

    @Test
    void rejectsBlankLegalEntity() {
        ConfiguredRetainedEarningsMappingAdapter adapter = adapter(
                " ", rule(YEAR, "35000", true, "controller", "CHG-776"));

        assertThatThrownBy(() -> adapter.requireForYear(YEAR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("legal-entity-code");
    }

    @Test
    void rejectsOverlengthLegalEntityThroughTheDomainBoundary() {
        ConfiguredRetainedEarningsMappingAdapter adapter = adapter(
                "E".repeat(21), rule(YEAR, "35000", true, "controller", "CHG-776"));

        assertThatThrownBy(() -> adapter.requireForYear(YEAR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mappings[0]");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRules")
    void rejectsIncompleteOrUnapprovedRules(String scenario, FiscalYearRule invalidRule, String path) {
        ConfiguredRetainedEarningsMappingAdapter adapter = adapter("ENTITY-01", invalidRule);

        assertThatThrownBy(() -> adapter.requireForYear(YEAR))
                .as(scenario)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(path);
    }

    @Test
    void rejectsUnsupportedRequestedFiscalYearBeforeUsingMappings() {
        ConfiguredRetainedEarningsMappingAdapter adapter = adapter(
                "ENTITY-01", rule(YEAR, "35000", true, "controller", "CHG-776"));

        assertThatThrownBy(() -> adapter.requireForYear(1899))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1900 through 9999");
    }

    @Test
    void kebabCasePropertiesBindOneMappingPortWithoutAUsableDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(BindingConfiguration.class)
                .withPropertyValues(
                        "account.closing.annual.legal-entity-code=ENTITY-TEST",
                        "account.closing.annual.mappings[0].fiscal-year=2026",
                        "account.closing.annual.mappings[0].account-code=35000",
                        "account.closing.annual.mappings[0].postable=true",
                        "account.closing.annual.mappings[0].approved-by=test-controller",
                        "account.closing.annual.mappings[0].change-reference=TEST-776")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(AnnualClosingConfigurationProperties.class))
                            .hasSize(1);
                    assertThat(context.getBeansOfType(RetainedEarningsMappingPort.class).values())
                            .singleElement()
                            .isInstanceOf(ConfiguredRetainedEarningsMappingAdapter.class);
                    assertThat(context.getBean(RetainedEarningsMappingPort.class).requireForYear(YEAR))
                            .extracting(
                                    ApprovedRetainedEarningsMapping::legalEntityCode,
                                    ApprovedRetainedEarningsMapping::accountCode,
                                    ApprovedRetainedEarningsMapping::approvedBy,
                                    ApprovedRetainedEarningsMapping::changeReference)
                            .containsExactly("ENTITY-TEST", "35000", "test-controller", "TEST-776");
                });

        ConfiguredRetainedEarningsMappingAdapter defaultAdapter =
                new ConfiguredRetainedEarningsMappingAdapter(new AnnualClosingConfigurationProperties());
        assertThatThrownBy(() -> defaultAdapter.requireForYear(YEAR))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Stream<Arguments> invalidRules() {
        return Stream.of(
                Arguments.of(
                        "missing fiscal year",
                        rule(null, "35000", true, "controller", "CHG-776"),
                        "fiscal-year"),
                Arguments.of(
                        "configured year below supported range",
                        rule(1899, "35000", true, "controller", "CHG-776"),
                        "fiscal-year"),
                Arguments.of(
                        "blank account",
                        rule(YEAR, " ", true, "controller", "CHG-776"),
                        "account-code"),
                Arguments.of(
                        "postability not approved",
                        rule(YEAR, "35000", false, "controller", "CHG-776"),
                        "postable"),
                Arguments.of(
                        "blank approver",
                        rule(YEAR, "35000", true, " ", "CHG-776"),
                        "approved-by"),
                Arguments.of(
                        "blank change reference",
                        rule(YEAR, "35000", true, "controller", " "),
                        "change-reference"),
                Arguments.of(
                        "overlength account",
                        rule(YEAR, "3".repeat(51), true, "controller", "CHG-776"),
                        "mappings[0]"),
                Arguments.of(
                        "overlength approver",
                        rule(YEAR, "35000", true, "A".repeat(81), "CHG-776"),
                        "mappings[0]"),
                Arguments.of(
                        "overlength change reference",
                        rule(YEAR, "35000", true, "controller", "R".repeat(201)),
                        "mappings[0]"));
    }

    private static ConfiguredRetainedEarningsMappingAdapter adapter(
            String legalEntityCode,
            FiscalYearRule... rules) {
        return new ConfiguredRetainedEarningsMappingAdapter(
                new AnnualClosingConfigurationProperties(legalEntityCode, Arrays.asList(rules)));
    }

    private static FiscalYearRule rule(
            Integer fiscalYear,
            String accountCode,
            Boolean postable,
            String approvedBy,
            String changeReference) {
        return new FiscalYearRule(
                fiscalYear, accountCode, postable, approvedBy, changeReference);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AnnualClosingConfigurationProperties.class)
    @Import(ConfiguredRetainedEarningsMappingAdapter.class)
    static class BindingConfiguration {
    }
}
