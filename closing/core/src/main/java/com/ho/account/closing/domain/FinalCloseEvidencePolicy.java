package com.ho.account.closing.domain;

import com.ho.account.closing.domain.FinalCloseEvidenceControl.Outcome;
import com.ho.account.closing.domain.FinalCloseEvidenceControl.Type;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Financial eligibility of an immutable snapshot for a monthly or annual final close. */
public final class FinalCloseEvidencePolicy {

    private static final Set<Type> MONTHLY_TYPES = Set.of(
            Type.AP_SUBLEDGER,
            Type.AR_SUBLEDGER,
            Type.LEASE_SUBLEDGER,
            Type.LOAN_SUBLEDGER,
            Type.JOURNAL_ADJUSTMENTS,
            Type.ECL_RECONCILIATION);

    private FinalCloseEvidencePolicy() { }

    public static void requireReconciled(String fiscalPeriod, List<FinalCloseEvidenceControl> controls) {
        Set<Type> required = EnumSet.copyOf(MONTHLY_TYPES);
        if ("YEAR".equals(fiscalPeriod)) {
            required.add(Type.ANNUAL_TRANSFER);
        }

        Map<Type, Integer> counts = new EnumMap<>(Type.class);
        for (FinalCloseEvidenceControl control : controls) {
            counts.merge(control.type(), 1, Integer::sum);
        }
        if (!counts.keySet().equals(required)
                || required.stream().anyMatch(type -> counts.getOrDefault(type, 0) != 1)) {
            throw new Violation("required controls must each appear exactly once: " + required);
        }
        for (FinalCloseEvidenceControl control : controls) {
            requireControlReconciled(control);
        }
    }

    private static void requireControlReconciled(FinalCloseEvidenceControl control) {
        if (!control.type().expectedSourceSystem().equals(control.sourceSystem())) {
            throw new Violation(control.type() + " must come from " + control.type().expectedSourceSystem());
        }
        if (control.outcome() != Outcome.PASS || control.blockingItemCount() != 0) {
            throw new Violation(control.type() + " did not pass with zero blocking items");
        }
        if (control.totals().isEmpty()) {
            throw new Violation(control.type() + " must include at least one dimensioned total");
        }

        Set<String> dimensions = new HashSet<>();
        for (FinalCloseEvidenceTotal total : control.totals()) {
            if (!dimensions.add(total.dimensionKey())) {
                throw new Violation(control.type() + " contains duplicate account/currency dimensions");
            }
            // An explicit zero/zero dimension passes; a missing dimension was rejected above.
            if (total.sourceTotal().compareTo(total.postedTotal()) != 0) {
                throw new Violation(control.type() + " source and posted totals differ for "
                        + total.accountCode() + "/" + total.currencyCode());
            }
        }
    }

    public static final class Violation extends IllegalStateException {
        public Violation(String message) {
            super(message);
        }
    }
}
