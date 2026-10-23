package com.ho.account.contracts.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalContractValidationTest {

    @Test
    void copiesJournalLinesSoCallerMutationCannotChangeTheCommand() {
        JournalLineCommand line = line("DEBIT", "101000");
        List<JournalLineCommand> mutableLines = new ArrayList<>(List.of(line));

        JournalEntryCommand command = entry(mutableLines);
        mutableLines.clear();

        assertThat(command.lines()).containsExactly(line);
        assertThatThrownBy(() -> command.lines().add(line))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void normalizesAndValidatesJournalSideAndAccountCode() {
        JournalLineCommand line = line(" debit ", " 101000 ");

        assertThat(line.drcrType()).isEqualTo("DEBIT");
        assertThat(line.accountCode()).isEqualTo("101000");

        assertThatThrownBy(() -> line("BALANCE", "101000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DEBIT or CREDIT");
        assertThatThrownBy(() -> new JournalLineCommand(
                "DEBIT", "101000", null, null, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void requiresAtLeastOneJournalLine() {
        assertThatThrownBy(() -> entry(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lines");
    }

    @Test
    void keepsOperationIdentitySeparateFromSlipNumber() {
        JournalEntryCommand command = entryWithIdentity(" SLIP-1 ", " payment:v2 ");

        assertThat(command.slipNo()).isEqualTo("SLIP-1");
        assertThat(command.operationKey()).isEqualTo("payment:v2");
    }

    @Test
    void rejectsInvalidSuppliedOperationKey() {
        assertThatThrownBy(() -> entryWithIdentity(null, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operationKey must not be blank");
        assertThatThrownBy(() -> entryWithIdentity(null, "x".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operationKey must not exceed 100");
        assertThat(entryWithIdentity(null, "x".repeat(100)).operationKey()).hasSize(100);
    }

    @Test
    void legacyConstructorsLeaveOperationIdentityAbsent() {
        assertThat(entry(List.of(line("DEBIT", "101000"))).operationKey()).isNull();
        assertThat(entryWithLegacySlipNo("SLIP-1").operationKey()).isNull();
    }

    @Test
    void acceptsLegacyJsonWithoutOperationKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ObjectNode legacyPayload = (ObjectNode) mapper.valueToTree(entryWithIdentity(null, "PAYMENT:v1"));
        legacyPayload.remove("operationKey");

        JournalEntryCommand command = mapper.treeToValue(legacyPayload, JournalEntryCommand.class);

        assertThat(command.operationKey()).isNull();
        assertThat(command.lineageSourceId()).isEqualTo("TEST-1");
        assertThat(command.lines()).hasSize(1);
    }

    @Test
    void validatesAccountNormalBalanceDirection() {
        AccountSubjectRef creditAccount =
                new AccountSubjectRef("221000", "Borrowing", false, false, " credit ");

        assertThat(creditAccount.normalBalanceSide()).isEqualTo("CREDIT");
        assertThat(creditAccount.creditNormalBalance()).isTrue();
        assertThat(new AccountSubjectRef("101000", "Cash", false, false).debitNormalBalance())
                .isTrue();
        assertThatThrownBy(() ->
                new AccountSubjectRef("101000", "Cash", false, false, "UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DEBIT or CREDIT");
        assertThatThrownBy(() ->
                new AccountSubjectRef("101000", "Cash", false, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("normalBalanceSide");
    }

    private JournalLineCommand line(String side, String accountCode) {
        return new JournalLineCommand(
                side,
                accountCode,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                null,
                null,
                "contract test");
    }

    private JournalEntryCommand entry(List<JournalLineCommand> lines) {
        return new JournalEntryCommand(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 22),
                "Contract test",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "tester",
                "tester",
                "TEST",
                "TEST-1",
                lines);
    }

    private JournalEntryCommand entryWithLegacySlipNo(String slipNo) {
        return new JournalEntryCommand(
                LocalDate.of(2026, 7, 22), LocalDate.of(2026, 7, 22),
                "Contract test", "GENERAL", "KRW", BigDecimal.ONE,
                "tester", "tester", "TEST", "TEST-1", slipNo,
                List.of(line("DEBIT", "101000")));
    }

    private JournalEntryCommand entryWithIdentity(String slipNo, String operationKey) {
        return new JournalEntryCommand(
                LocalDate.of(2026, 7, 22), LocalDate.of(2026, 7, 22),
                "Contract test", "GENERAL", "KRW", BigDecimal.ONE,
                "tester", "tester", "TEST", "TEST-1", slipNo, operationKey,
                List.of(line("DEBIT", "101000")));
    }
}
