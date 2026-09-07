package com.ho.account.reconciliation.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import com.ho.account.reconciliation.infrastructure.local.ReconciliationLocalExternalPortConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationPortConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    ReconciliationLocalExternalPortConfiguration.class,
                    HttpReconciliationJournalAdapter.class,
                    HttpReconciliationLedgerAdapter.class
            ));

    @Test
    void localProfileWithoutRemotePropertyEnablesLocalStubsOnly() {
        runner.withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(ReconciliationLocalExternalPortConfiguration.class);
                    assertThat(context).hasSingleBean(JournalQueryPort.class);
                    assertThat(context).hasSingleBean(JournalPostingPort.class);
                    assertThat(context).hasSingleBean(LedgerQueryPort.class);
                    assertThat(context).doesNotHaveBean(HttpReconciliationJournalAdapter.class);
                    assertThat(context).doesNotHaveBean(HttpReconciliationLedgerAdapter.class);
                });
    }

    @Test
    void remoteEnabledActivatesHttpAdaptersAndDisablesLocalStubs() {
        runner.withPropertyValues(
                        "spring.profiles.active=local",
                        "reconciliation.journal-ledger.remote.enabled=true",
                        "reconciliation.journal-ledger.base-url=http://journal-ledger.test"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ReconciliationLocalExternalPortConfiguration.class);
                    assertThat(context).hasSingleBean(HttpReconciliationJournalAdapter.class);
                    assertThat(context).hasSingleBean(HttpReconciliationLedgerAdapter.class);
                    assertThat(context).hasSingleBean(JournalQueryPort.class);
                    assertThat(context).hasSingleBean(JournalPostingPort.class);
                    assertThat(context).hasSingleBean(LedgerQueryPort.class);
                });
    }
}
