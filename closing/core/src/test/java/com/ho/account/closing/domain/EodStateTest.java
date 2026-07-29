package com.ho.account.closing.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EodStateTest {

    @Test
    void permitsOnlyDeclaredSameRowTransitions() {
        Set<String> allowed = Set.of(
                key(EodState.OPEN, EodState.PRE_CLOSING),
                key(EodState.PRE_CLOSING, EodState.OPEN),
                key(EodState.PRE_CLOSING, EodState.CLOSING_IN_PROGRESS),
                key(EodState.CLOSING_IN_PROGRESS, EodState.CLOSED),
                key(EodState.BOD_IN_PROGRESS, EodState.OPEN));

        for (EodState current : EodState.values()) {
            for (EodState target : EodState.values()) {
                assertThat(current.canTransitionTo(target))
                        .as("%s -> %s", current, target)
                        .isEqualTo(allowed.contains(key(current, target)));
            }
        }

        assertThat(EodState.CLOSED.canTransitionTo(EodState.BOD_IN_PROGRESS)).isFalse();
        assertThat(EodState.OPEN.isTransactionAllowed()).isTrue();
        assertThat(EodState.values())
                .filteredOn(state -> state != EodState.OPEN)
                .allMatch(state -> !state.isTransactionAllowed());
    }

    private static String key(EodState current, EodState target) {
        return current + "->" + target;
    }
}
