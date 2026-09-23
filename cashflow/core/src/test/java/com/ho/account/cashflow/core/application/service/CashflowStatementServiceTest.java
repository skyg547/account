package com.ho.account.cashflow.core.application.service;

import com.ho.account.cashflow.core.application.port.in.GenerateStatementCommand;
import com.ho.account.cashflow.core.domain.CashflowActivity;
import com.ho.account.cashflow.core.domain.CashflowLineItem;
import com.ho.account.cashflow.core.domain.CashflowMethod;
import com.ho.account.cashflow.core.domain.CashflowStatement;
import com.ho.account.cashflow.core.infrastructure.memory.InMemoryCashflowStatementAdapter;
import com.ho.account.cashflow.core.infrastructure.memory.InMemoryLedgerCashBalanceAdapter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CashflowStatementServiceTest {

    @Test
    void loadsBeginningBalanceBuildsBalancedStatementAndPersistsIt() {
        InMemoryCashflowStatementAdapter persistence = new InMemoryCashflowStatementAdapter();
        InMemoryLedgerCashBalanceAdapter ledger = new InMemoryLedgerCashBalanceAdapter();
        ledger.setBeginningCash(2026, 9, "KRW", new BigDecimal("1200"));
        CashflowStatementService service = new CashflowStatementService(persistence, ledger);

        CashflowStatement generated = service.generate(new GenerateStatementCommand(
                "stmt-service",
                2026,
                9,
                CashflowMethod.DIRECT,
                "KRW",
                LocalDateTime.of(2026, 9, 23, 10, 0),
                List.of(new CashflowLineItem(
                        "OP-01", "collection", CashflowActivity.OPERATING,
                        new BigDecimal("300"), "KRW", "customer receipt"))));

        assertThat(generated.getEndingCash()).isEqualByComparingTo("1500.00");
        assertThat(service.findById("stmt-service")).containsSame(generated);
    }
}
