package com.ho.account.contracts.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FiscalPeriodTransitionContractTest {

    @Test
    void receiptMatchesOnlyTheEntireOriginalCommand() {
        FiscalPeriodTransitionCommand command = command();
        FiscalPeriodTransitionReceipt receipt = new FiscalPeriodTransitionReceipt(
                command.operationId(), command.fiscalPeriodId(), command.expectedStatus(),
                command.targetStatus(), command.auditUser(), FiscalPeriodTransitionReceipt.Phase.APPLIED);

        assertThat(receipt.matches(command)).isTrue();
        assertThat(receipt.matches(null)).isFalse();
        assertThat(receipt.matches(new FiscalPeriodTransitionCommand(
                "other-operation", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "approver"))).isFalse();
        assertThat(receipt.matches(new FiscalPeriodTransitionCommand(
                "operation-1", 11L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "approver"))).isFalse();
        assertThat(receipt.matches(new FiscalPeriodTransitionCommand(
                "operation-1", 10L, FiscalPeriodClosingStatus.OPEN,
                FiscalPeriodClosingStatus.CLOSED, "approver"))).isFalse();
        assertThat(receipt.matches(new FiscalPeriodTransitionCommand(
                "operation-1", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "other-approver"))).isFalse();
        FiscalPeriodTransitionReceipt cancelled = new FiscalPeriodTransitionReceipt(
                command.operationId(), command.fiscalPeriodId(), command.expectedStatus(),
                command.targetStatus(), command.auditUser(), FiscalPeriodTransitionReceipt.Phase.CANCELLED);
        assertThat(cancelled.matches(command)).isTrue();
        FiscalPeriodTransitionReceipt confirmed = new FiscalPeriodTransitionReceipt(
                command.operationId(), command.fiscalPeriodId(), command.expectedStatus(),
                command.targetStatus(), command.auditUser(), FiscalPeriodTransitionReceipt.Phase.CONFIRMED);
        assertThat(confirmed.matches(command)).isTrue();
    }

    @Test
    void rejectsIncompleteOrAmbiguousTransitionIdentity() {
        assertThatThrownBy(() -> new FiscalPeriodTransitionCommand(
                " ", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "approver"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("operationId");
        assertThatThrownBy(() -> new FiscalPeriodTransitionCommand(
                " operation-1", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "approver"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("operationId");
        assertThatThrownBy(() -> new FiscalPeriodTransitionCommand(
                "operation-1\n", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "approver"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("operationId");
        assertThatThrownBy(() -> new FiscalPeriodTransitionCommand(
                "operation-1", 0L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "approver"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fiscalPeriodId");
        assertThatThrownBy(() -> new FiscalPeriodTransitionCommand(
                "operation-1", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.CLOSED, "approver"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must differ");
        assertThatThrownBy(() -> new FiscalPeriodTransitionCommand(
                "operation-1", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("auditUser");
        assertThatThrownBy(() -> new FiscalPeriodTransitionReceipt(
                "operation-1", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "approver", null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("phase");
    }

    @Test
    void legacyPortImplementationsFailClosedForUnimplementedProtocol() {
        FiscalPeriodControlPort legacy = new FiscalPeriodControlPort() {
            @Override
            public java.util.Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
                return java.util.Optional.empty();
            }

            @Override
            public FiscalPeriodRef updateClosingStatus(Long id, String status, String actor) {
                throw new UnsupportedOperationException();
            }
        };

        assertThatThrownBy(() -> legacy.prepareClosingTransition(command()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> legacy.applyClosingTransition("operation-1"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> legacy.confirmClosingTransition("operation-1"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> legacy.cancelClosingTransition("operation-1", "operator"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> legacy.findTransitionReceipt("operation-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private FiscalPeriodTransitionCommand command() {
        return new FiscalPeriodTransitionCommand(
                "operation-1", 10L, FiscalPeriodClosingStatus.CLOSED,
                FiscalPeriodClosingStatus.OPEN, "approver");
    }
}
