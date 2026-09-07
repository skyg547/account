package com.ho.account.receivable.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.infrastructure.adapter.HttpReceivableJournalAdapter;
import com.ho.account.receivable.infrastructure.adapter.HttpReceivableMasterDataAdapter;
import com.ho.account.receivable.infrastructure.adapter.ReceivableAccountingPeriodStatusAdapter;
import com.ho.account.receivable.infrastructure.local.ReceivableLocalExternalPortConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class ReceivablePortConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ReceivableLocalExternalPortConfiguration.class,
                    HttpReceivableMasterDataAdapter.class,
                    HttpReceivableJournalAdapter.class,
                    ReceivableAccountingPeriodStatusAdapter.class
            )
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void registersLocalStubsUnderLocalProfileByDefault() {
        contextRunner
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(MasterDataQueryPort.class);
                    assertThat(context).hasBean("receivableLocalMasterDataQueryPort");
                    assertThat(context).doesNotHaveBean(HttpReceivableMasterDataAdapter.class);

                    assertThat(context).hasSingleBean(JournalPostingPort.class);
                    assertThat(context).hasBean("receivableLocalJournalPostingPort");
                    assertThat(context).doesNotHaveBean(HttpReceivableJournalAdapter.class);

                    assertThat(context).hasSingleBean(AccountingPeriodStatusPort.class);
                    assertThat(context).hasBean("receivableLocalAccountingPeriodStatusPort");
                    assertThat(context).doesNotHaveBean(ReceivableAccountingPeriodStatusAdapter.class);
                });
    }

    @Test
    void registersRemoteAdaptersWhenPropertiesEnabledAndLocalStubsBackOff() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "receivable.master-data.remote.enabled=true",
                        "receivable.master-data.base-url=http://master-data-api:8083",
                        "receivable.journal-ledger.remote.enabled=true",
                        "receivable.journal-ledger.base-url=http://journal-ledger-api:8084"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MasterDataQueryPort.class);
                    assertThat(context).hasSingleBean(FiscalPeriodControlPort.class);
                    assertThat(context).hasSingleBean(HttpReceivableMasterDataAdapter.class);
                    assertThat(context).doesNotHaveBean("receivableLocalMasterDataQueryPort");

                    assertThat(context).hasSingleBean(JournalPostingPort.class);
                    assertThat(context).hasSingleBean(HttpReceivableJournalAdapter.class);
                    assertThat(context).doesNotHaveBean("receivableLocalJournalPostingPort");

                    assertThat(context).hasSingleBean(AccountingPeriodStatusPort.class);
                    assertThat(context).hasSingleBean(ReceivableAccountingPeriodStatusAdapter.class);
                    assertThat(context).doesNotHaveBean("receivableLocalAccountingPeriodStatusPort");
                });
    }

    @Test
    void localConfigurationBacksOffWhenRemoteUmbrellaPropertyEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "receivable.remote.enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean("receivableLocalMasterDataQueryPort");
                    assertThat(context).doesNotHaveBean("receivableLocalJournalPostingPort");
                    assertThat(context).doesNotHaveBean("receivableLocalAccountingPeriodStatusPort");
                });
    }
}
