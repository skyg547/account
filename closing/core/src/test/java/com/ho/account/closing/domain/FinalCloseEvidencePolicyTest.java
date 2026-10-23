package com.ho.account.closing.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.closing.domain.FinalCloseEvidenceControl.Outcome;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinalCloseEvidencePolicyTest {

    @Test
    void monthlyAndAnnualRequireTheirOwnExactControlSets() {
        List<FinalCloseEvidenceControl> monthly = controls(false);
        assertThatCode(() -> FinalCloseEvidencePolicy.requireReconciled("01", monthly)).doesNotThrowAnyException();
        assertViolation("YEAR", monthly, "required controls");

        List<FinalCloseEvidenceControl> annual = controls(true);
        assertThatCode(() -> FinalCloseEvidencePolicy.requireReconciled("YEAR", annual)).doesNotThrowAnyException();
        assertViolation("01", annual, "required controls");

        List<FinalCloseEvidenceControl> duplicate = new ArrayList<>(monthly);
        duplicate.add(monthly.get(0));
        assertViolation("01", duplicate, "required controls");
    }

    @Test
    void eachRequiredControlMustHaveAuthoritativeSourcePassAndZeroBlockers() {
        for (Type type : Type.values()) {
            String period = type == Type.ANNUAL_TRANSFER ? "YEAR" : "01";
            List<FinalCloseEvidenceControl> valid = controls(type == Type.ANNUAL_TRANSFER);
            assertReplacementViolation(period, valid, type,
                    new FinalCloseEvidenceControl(type, "wrong-provider", "run", Outcome.PASS, 0, totals()),
                    "must come from");
            assertReplacementViolation(period, valid, type,
                    new FinalCloseEvidenceControl(type, type.expectedSourceSystem(), "run", Outcome.FAIL, 0, totals()),
                    "did not pass");
            assertReplacementViolation(period, valid, type,
                    new FinalCloseEvidenceControl(type, type.expectedSourceSystem(), "run", Outcome.PASS, 1, totals()),
                    "did not pass");
        }
    }

    @Test
    void explicitZeroAndEqualScaleVariantsPassButMissingDuplicateAndUnequalDimensionsFail() {
        List<FinalCloseEvidenceControl> valid = controls(false);
        Type type = Type.AP_SUBLEDGER;
        assertThatCode(() -> FinalCloseEvidencePolicy.requireReconciled("01", valid)).doesNotThrowAnyException();
        assertReplacementViolation("01", valid, type, control(type, List.of()), "at least one");
        assertReplacementViolation("01", valid, type, control(type, List.of(
                total("100", "USD", "1", "1.0"), total("100", "usd", "2", "2"))), "duplicate");
        assertReplacementViolation("01", valid, type, control(type, List.of(
                total("100", "USD", "0.000000000000000001", "0"))), "totals differ");
    }

    @Test
    void amountStorageBoundaryRejectsOverflowWithoutRounding() {
        String largest = "99999999999999999999.999999999999999999";
        assertThatCode(() -> total("100", "USD", largest, largest)).doesNotThrowAnyException();
        assertThatCode(() -> total("100", "USD", "1.123456789012345678000", "1.123456789012345678"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> total("100", "USD", "100000000000000000000", "0"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NUMERIC(38,18)");
        assertThatThrownBy(() -> total("100", "USD", "0.1234567890123456789", "0"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NUMERIC(38,18)");
    }

    private static void assertReplacementViolation(String period, List<FinalCloseEvidenceControl> source,
            Type type, FinalCloseEvidenceControl replacement, String message) {
        List<FinalCloseEvidenceControl> controls = new ArrayList<>(source);
        controls.set(Arrays.stream(Type.values()).filter(t -> t != Type.ANNUAL_TRANSFER || "YEAR".equals(period))
                .toList().indexOf(type), replacement);
        assertViolation(period, controls, message);
    }

    private static void assertViolation(String period, List<FinalCloseEvidenceControl> controls, String message) {
        assertThatThrownBy(() -> FinalCloseEvidencePolicy.requireReconciled(period, controls))
                .isInstanceOf(FinalCloseEvidencePolicy.Violation.class).hasMessageContaining(message);
    }

    private static List<FinalCloseEvidenceControl> controls(boolean annual) {
        return Arrays.stream(Type.values()).filter(type -> annual || type != Type.ANNUAL_TRANSFER)
                .map(type -> control(type, totals())).toList();
    }

    private static FinalCloseEvidenceControl control(Type type, List<FinalCloseEvidenceTotal> totals) {
        return new FinalCloseEvidenceControl(type, type.expectedSourceSystem(), "run", Outcome.PASS, 0, totals);
    }

    private static List<FinalCloseEvidenceTotal> totals() {
        return List.of(total("100", "USD", "0", "0.000"),
                total("200", "KRW", "123456789.123456789012345678", "123456789.1234567890123456780"));
    }

    private static FinalCloseEvidenceTotal total(String account, String currency, String source, String posted) {
        return new FinalCloseEvidenceTotal(account, currency, new BigDecimal(source), new BigDecimal(posted));
    }
}
