package com.ho.account.cashflow.core.infrastructure.config;

import com.ho.account.cashflow.core.application.port.out.CashflowForecastPersistencePort;
import com.ho.account.cashflow.core.application.port.out.CashflowStatementPersistencePort;
import com.ho.account.cashflow.core.application.port.out.LedgerCashBalancePort;
import com.ho.account.cashflow.core.infrastructure.memory.InMemoryCashflowForecastAdapter;
import com.ho.account.cashflow.core.infrastructure.memory.InMemoryCashflowStatementAdapter;
import com.ho.account.cashflow.core.infrastructure.memory.InMemoryLedgerCashBalanceAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Local-only adapters keep the initial domain executable without introducing a production datastore contract. */
@Configuration(proxyBeanMethods = false)
@Profile("local")
public class CashflowLocalAdapterConfiguration {

    @Bean
    CashflowStatementPersistencePort cashflowStatementPersistencePort() {
        return new InMemoryCashflowStatementAdapter();
    }

    @Bean
    CashflowForecastPersistencePort cashflowForecastPersistencePort() {
        return new InMemoryCashflowForecastAdapter();
    }

    @Bean
    LedgerCashBalancePort ledgerCashBalancePort() {
        return new InMemoryLedgerCashBalanceAdapter();
    }
}
