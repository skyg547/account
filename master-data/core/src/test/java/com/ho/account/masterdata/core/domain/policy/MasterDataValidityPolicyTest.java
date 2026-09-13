package com.ho.account.masterdata.core.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MasterDataValidityPolicyTest {
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = START.plusDays(20);

    @ParameterizedTest
    @MethodSource("containedWindows")
    void acceptsOnlyContainedInclusiveSplits(LocalDate from, LocalDate to) {
        assertThatCode(() -> MasterDataValidityPolicy.requireVersionSplit(START, END, from, to))
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> containedWindows() {
        return Stream.of(
                Arguments.of(START.plusDays(1), END),
                Arguments.of(END, END),
                Arguments.of(START.plusDays(5), START.plusDays(10)));
    }

    @ParameterizedTest
    @MethodSource("invalidWindows")
    void rejectsInvalidOrExtendingWindowsBeforeDateArithmetic(
            LocalDate currentFrom, LocalDate currentTo, LocalDate from, LocalDate to) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                MasterDataValidityPolicy.requireVersionSplit(currentFrom, currentTo, from, to));
    }

    static Stream<Arguments> invalidWindows() {
        return Stream.of(
                Arguments.of(null, END, START.plusDays(1), END),
                Arguments.of(START, null, START.plusDays(1), END),
                Arguments.of(END, START, START.plusDays(1), END),
                Arguments.of(START, END, null, END),
                Arguments.of(START, END, START.plusDays(1), null),
                Arguments.of(START, END, START.plusDays(2), START.plusDays(1)),
                Arguments.of(START, END, START, END),
                Arguments.of(START, END, START.minusDays(1), END),
                Arguments.of(START, END, START.plusDays(1), END.plusDays(1)),
                Arguments.of(START, END, END.plusDays(1), END.plusDays(2)),
                Arguments.of(START, END, LocalDate.MIN, END),
                Arguments.of(START, START, START, START));
    }
}
