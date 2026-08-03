package com.ho.account.deposit.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DepositAccountStateMachine 단위 테스트")
class DepositAccountStateMachineTest {

    private final DepositAccountStateMachine stateMachine = new DepositAccountStateMachine();

    @Test
    @DisplayName("정상 상태 전이(ACTIVE -> SUSPENDED, SUSPENDED -> ACTIVE, ACTIVE -> CLOSED)가 허용되는지 검증한다")
    void validateStateTransition_Valid() {
        assertThatCode(() -> stateMachine.validateStateTransition(DepositStatus.ACTIVE, DepositStatus.SUSPENDED))
                .doesNotThrowAnyException();

        assertThatCode(() -> stateMachine.validateStateTransition(DepositStatus.SUSPENDED, DepositStatus.ACTIVE))
                .doesNotThrowAnyException();

        assertThatCode(() -> stateMachine.validateStateTransition(DepositStatus.ACTIVE, DepositStatus.CLOSED))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("유효하지 않은 상태 전이(CLOSED -> ACTIVE, CLOSED -> SUSPENDED) 시 예외가 발생한다")
    void validateStateTransition_Invalid() {
        assertThatThrownBy(() -> stateMachine.validateStateTransition(DepositStatus.CLOSED, DepositStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid deposit account state transition");

        assertThatThrownBy(() -> stateMachine.validateStateTransition(DepositStatus.CLOSED, DepositStatus.SUSPENDED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid deposit account state transition");
    }
}
