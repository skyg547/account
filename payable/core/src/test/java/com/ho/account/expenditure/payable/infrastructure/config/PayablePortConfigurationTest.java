package com.ho.account.expenditure.payable.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.payable.infrastructure.adapter.HttpPayableJournalAdapter;
import com.ho.account.expenditure.payable.infrastructure.adapter.HttpPayableMasterDataAdapter;
import com.ho.account.expenditure.payable.infrastructure.adapter.PayableAccountingPeriodStatusAdapter;
import com.ho.account.expenditure.payable.infrastructure.local.PayableLocalExternalPortConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class PayablePortConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    PayableLocalExternalPortConfiguration.class,
                    HttpPayableMasterDataAdapter.class,
                    HttpPayableJournalAdapter.class,
                    PayableAccountingPeriodStatusAdapter.class
            )
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void registersLocalStubsUnderLocalProfileByDefault() {
        contextRunner
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(MasterDataQueryPort.class);
                    assertThat(context).hasBean("payableLocalMasterDataQueryPort");
                    assertThat(context).doesNotHaveBean(HttpPayableMasterDataAdapter.class);

                    assertThat(context).hasSingleBean(JournalPostingPort.class);
                    assertThat(context).hasBean("payableLocalJournalPostingPort");
                    assertThat(context).doesNotHaveBean(HttpPayableJournalAdapter.class);

                    assertThat(context).hasSingleBean(AccountingPeriodStatusPort.class);
                    assertThat(context).hasBean("payableLocalAccountingPeriodStatusPort");
                    assertThat(context).doesNotHaveBean(PayableAccountingPeriodStatusAdapter.class);
                });
    }

    @Test
    void registersRemoteAdaptersWhenPropertiesEnabledAndLocalStubsBackOff() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "payable.master-data.remote.enabled=true",
                        "payable.master-data.base-url=http://master-data-api:8083",
                        "payable.journal-ledger.remote.enabled=true",
                        "payable.journal-ledger.base-url=http://journal-ledger-api:8084"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MasterDataQueryPort.class);
                    assertThat(context).hasSingleBean(FiscalPeriodControlPort.class);
                    assertThat(context).hasSingleBean(HttpPayableMasterDataAdapter.class);
                    assertThat(context).doesNotHaveBean("payableLocalMasterDataQueryPort");

                    assertThat(context).hasSingleBean(JournalPostingPort.class);
                    assertThat(context).hasSingleBean(HttpPayableJournalAdapter.class);
                    assertThat(context).doesNotHaveBean("payableLocalJournalPostingPort");

                    assertThat(context).hasSingleBean(AccountingPeriodStatusPort.class);
                    assertThat(context).hasSingleBean(PayableAccountingPeriodStatusAdapter.class);
                    assertThat(context).doesNotHaveBean("payableLocalAccountingPeriodStatusPort");
                });
    }

    @Test
    void localConfigurationBacksOffWhenRemoteUmbrellaPropertyEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "payable.remote.enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean("payableLocalMasterDataQueryPort");
                    assertThat(context).doesNotHaveBean("payableLocalJournalPostingPort");
                    assertThat(context).doesNotHaveBean("payableLocalAccountingPeriodStatusPort");
                });
    }
}
