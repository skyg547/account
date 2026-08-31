package com.ho.account.reconciliation.application.port.in;

import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconciliationCommandContractTest {

    @Nested
    @DisplayName("DifferenceReasonCodeCommand 계약 검증")
    class DifferenceReasonCodeCommandTests {

        @Test
        void createsValidCommandWithTrimmedValues() {
            DifferenceReasonCodeCommand command = new DifferenceReasonCodeCommand(
                    "  BANK_FEE  ", "  Bank Fee  ", "  Monthly bank fee  ", true, true
            );
            assertThat(command.code()).isEqualTo("BANK_FEE");
            assertThat(command.name()).isEqualTo("Bank Fee");
            assertThat(command.description()).isEqualTo("Monthly bank fee");
            assertThat(command.adjustable()).isTrue();
            assertThat(command.active()).isTrue();
        }

        @Test
        void defaultsOptionalDescriptionToEmptyStringWhenNullOrBlank() {
            DifferenceReasonCodeCommand command1 = new DifferenceReasonCodeCommand(
                    "BANK_FEE", "Bank Fee", null, true, true
            );
            assertThat(command1.description()).isEqualTo("");

            DifferenceReasonCodeCommand command2 = new DifferenceReasonCodeCommand(
                    "BANK_FEE", "Bank Fee", "   ", true, true
            );
            assertThat(command2.description()).isEqualTo("");
        }

        @Test
        void failsFastWhenRequiredFieldsAreMissing() {
            assertThatThrownBy(() -> new DifferenceReasonCodeCommand(null, "Name", "Desc", true, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");

            assertThatThrownBy(() -> new DifferenceReasonCodeCommand("   ", "Name", "Desc", true, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");

            assertThatThrownBy(() -> new DifferenceReasonCodeCommand("CODE", null, "Desc", true, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            assertThatThrownBy(() -> new DifferenceReasonCodeCommand("CODE", "  ", "Desc", true, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }
    }

    @Nested
    @DisplayName("ReconciliationRuleCommand 계약 검증")
    class ReconciliationRuleCommandTests {

        @Test
        void createsValidCommandWithTrimmedValuesAndDefaults() {
            ReconciliationRuleCommand command = new ReconciliationRuleCommand(
                    1L, "  Tolerance Rule  ", "  {}  ", ReconciliationRule.ToleranceType.ABSOLUTE,
                    new BigDecimal("10.00"), 1, true
            );
            assertThat(command.reconciliationUnitId()).isEqualTo(1L);
            assertThat(command.name()).isEqualTo("Tolerance Rule");
            assertThat(command.ruleDefinitionJson()).isEqualTo("{}");
            assertThat(command.toleranceType()).isEqualTo(ReconciliationRule.ToleranceType.ABSOLUTE);
            assertThat(command.toleranceValue()).isEqualByComparingTo("10.00");
            assertThat(command.priority()).isEqualTo(1);
            assertThat(command.active()).isTrue();
        }

        @Test
        void defaultsOptionalFieldsToNonNullValues() {
            ReconciliationRuleCommand command = new ReconciliationRuleCommand(
                    1L, "Tolerance Rule", null, ReconciliationRule.ToleranceType.PERCENTAGE,
                    null, 0, false
            );
            assertThat(command.ruleDefinitionJson()).isEqualTo("");
            assertThat(command.toleranceValue()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        void failsFastWhenRequiredFieldsAreInvalid() {
            assertThatThrownBy(() -> new ReconciliationRuleCommand(null, "Rule", "{}", ReconciliationRule.ToleranceType.ABSOLUTE, BigDecimal.ZERO, 1, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reconciliationUnitId must be greater than zero");

            assertThatThrownBy(() -> new ReconciliationRuleCommand(0L, "Rule", "{}", ReconciliationRule.ToleranceType.ABSOLUTE, BigDecimal.ZERO, 1, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reconciliationUnitId must be greater than zero");

            assertThatThrownBy(() -> new ReconciliationRuleCommand(-1L, "Rule", "{}", ReconciliationRule.ToleranceType.ABSOLUTE, BigDecimal.ZERO, 1, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reconciliationUnitId must be greater than zero");

            assertThatThrownBy(() -> new ReconciliationRuleCommand(1L, "  ", "{}", ReconciliationRule.ToleranceType.ABSOLUTE, BigDecimal.ZERO, 1, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            assertThatThrownBy(() -> new ReconciliationRuleCommand(1L, "Rule", "{}", null, BigDecimal.ZERO, 1, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("toleranceType is required");

            assertThatThrownBy(() -> new ReconciliationRuleCommand(1L, "Rule", "{}", ReconciliationRule.ToleranceType.ABSOLUTE, BigDecimal.ZERO, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priority is required");

            assertThatThrownBy(() -> new ReconciliationRuleCommand(1L, "Rule", "{}", ReconciliationRule.ToleranceType.ABSOLUTE, BigDecimal.ZERO, -1, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priority must be zero or greater");
        }
    }

    @Nested
    @DisplayName("ReconciliationUnitCommand 계약 검증")
    class ReconciliationUnitCommandTests {

        @Test
        void createsValidCommandWithTrimmedValues() {
            ReconciliationUnitCommand command = new ReconciliationUnitCommand(
                    "  Unit Name  ", "  Description  ", ReconciliationUnit.ReconciliationFrequency.DAILY,
                    ReconciliationUnit.ReconciliationType.BANK_BOOK, "  {}  ", true
            );
            assertThat(command.name()).isEqualTo("Unit Name");
            assertThat(command.description()).isEqualTo("Description");
            assertThat(command.frequency()).isEqualTo(ReconciliationUnit.ReconciliationFrequency.DAILY);
            assertThat(command.reconciliationType()).isEqualTo(ReconciliationUnit.ReconciliationType.BANK_BOOK);
            assertThat(command.criteriaJson()).isEqualTo("{}");
            assertThat(command.active()).isTrue();
        }

        @Test
        void defaultsOptionalFieldsToEmptyString() {
            ReconciliationUnitCommand command = new ReconciliationUnitCommand(
                    "Unit Name", null, ReconciliationUnit.ReconciliationFrequency.MONTHLY,
                    ReconciliationUnit.ReconciliationType.BANK_BOOK, null, false
            );
            assertThat(command.description()).isEqualTo("");
            assertThat(command.criteriaJson()).isEqualTo("");
        }

        @Test
        void failsFastWhenRequiredFieldsAreMissing() {
            assertThatThrownBy(() -> new ReconciliationUnitCommand(null, "Desc", ReconciliationUnit.ReconciliationFrequency.DAILY, ReconciliationUnit.ReconciliationType.BANK_BOOK, "{}", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            assertThatThrownBy(() -> new ReconciliationUnitCommand("  ", "Desc", ReconciliationUnit.ReconciliationFrequency.DAILY, ReconciliationUnit.ReconciliationType.BANK_BOOK, "{}", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            assertThatThrownBy(() -> new ReconciliationUnitCommand("Name", "Desc", null, ReconciliationUnit.ReconciliationType.BANK_BOOK, "{}", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("frequency is required");

            assertThatThrownBy(() -> new ReconciliationUnitCommand("Name", "Desc", ReconciliationUnit.ReconciliationFrequency.DAILY, null, "{}", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reconciliationType is required");
        }
    }

    @Nested
    @DisplayName("ExternalReconSnapshotRequest 계약 검증")
    class ExternalReconSnapshotRequestTests {

        @Test
        void createsValidRequestWithUppercaseStage() {
            LocalDate date = LocalDate.of(2026, 5, 12);
            ExternalReconSnapshotRequest request = ExternalReconSnapshotRequest.of(
                    "  UNIT-01  ", "source", date, " loan ", " krw ", " ho "
            );
            assertThat(request.unitId()).isEqualTo("UNIT-01");
            assertThat(request.stageCode()).isEqualTo("SOURCE");
            assertThat(request.reconciliationDate()).isEqualTo(date);
            assertThat(request.productCode()).isEqualTo("loan");
            assertThat(request.currencyCode()).isEqualTo("krw");
            assertThat(request.legalEntityCode()).isEqualTo("ho");
        }

        @Test
        void defaultsOptionalParametersToEmptyStrings() {
            LocalDate date = LocalDate.of(2026, 5, 12);
            ExternalReconSnapshotRequest request = ExternalReconSnapshotRequest.of(
                    "UNIT-01", "INTERFACE", date, null, "   ", null
            );
            assertThat(request.productCode()).isEqualTo("");
            assertThat(request.currencyCode()).isEqualTo("");
            assertThat(request.legalEntityCode()).isEqualTo("");
        }

        @Test
        void failsFastOnInvalidRequiredParameters() {
            LocalDate date = LocalDate.of(2026, 5, 12);

            assertThatThrownBy(() -> ExternalReconSnapshotRequest.of(null, "SOURCE", date, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unitId must not be blank");

            assertThatThrownBy(() -> ExternalReconSnapshotRequest.of("   ", "SOURCE", date, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unitId must not be blank");

            assertThatThrownBy(() -> ExternalReconSnapshotRequest.of("UNIT-01", null, date, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("stageCode must not be blank");

            assertThatThrownBy(() -> ExternalReconSnapshotRequest.of("UNIT-01", "   ", date, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("stageCode must not be blank");

            assertThatThrownBy(() -> ExternalReconSnapshotRequest.of("UNIT-01", "SOURCE", null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reconciliationDate must not be null");
        }
    }
}
