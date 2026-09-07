package com.ho.account.expenditure.resolution.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.contracts.asset.AssetRegistrationPort;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.expenditure.resolution.infrastructure.adapter.HttpExpenditureAssetAdapter;
import com.ho.account.expenditure.resolution.infrastructure.adapter.HttpExpenditureJournalAdapter;
import com.ho.account.expenditure.resolution.infrastructure.adapter.HttpExpenditureMasterDataAdapter;
import com.ho.account.expenditure.resolution.infrastructure.adapter.HttpExpenditureTaxAdapter;
import com.ho.account.expenditure.resolution.infrastructure.local.ExpenditureResolutionLocalExternalPortConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class ExpenditurePortConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ExpenditureResolutionLocalExternalPortConfiguration.class,
                    HttpExpenditureMasterDataAdapter.class,
                    HttpExpenditureJournalAdapter.class,
                    HttpExpenditureTaxAdapter.class,
                    HttpExpenditureAssetAdapter.class
            )
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void registersLocalStubsUnderLocalProfileByDefault() {
        contextRunner
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(MasterDataQueryPort.class);
                    assertThat(context).hasBean("expenditureLocalMasterDataQueryPort");
                    assertThat(context).doesNotHaveBean(HttpExpenditureMasterDataAdapter.class);

                    assertThat(context).hasSingleBean(JournalPostingPort.class);
                    assertThat(context).hasBean("expenditureLocalJournalPostingPort");
                    assertThat(context).doesNotHaveBean(HttpExpenditureJournalAdapter.class);

                    assertThat(context).hasSingleBean(TaxInvoiceQueryPort.class);
                    assertThat(context).hasBean("expenditureLocalTaxInvoiceQueryPort");
                    assertThat(context).doesNotHaveBean(HttpExpenditureTaxAdapter.class);

                    assertThat(context).hasSingleBean(AssetRegistrationPort.class);
                    assertThat(context).hasBean("expenditureLocalAssetRegistrationPort");
                    assertThat(context).doesNotHaveBean(HttpExpenditureAssetAdapter.class);
                });
    }

    @Test
    void registersRemoteAdaptersWhenPropertiesEnabledAndLocalStubsBackOff() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "expenditure.master-data.remote.enabled=true",
                        "expenditure.master-data.base-url=http://master-data-api:8083",
                        "expenditure.journal-ledger.remote.enabled=true",
                        "expenditure.journal-ledger.base-url=http://journal-ledger-api:8084",
                        "expenditure.tax.remote.enabled=true",
                        "expenditure.tax.base-url=http://tax-api:8087",
                        "expenditure.asset.remote.enabled=true",
                        "expenditure.asset.base-url=http://asset-lease-api:8089"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MasterDataQueryPort.class);
                    assertThat(context).hasSingleBean(HttpExpenditureMasterDataAdapter.class);
                    assertThat(context).doesNotHaveBean("expenditureLocalMasterDataQueryPort");

                    assertThat(context).hasSingleBean(JournalPostingPort.class);
                    assertThat(context).hasSingleBean(HttpExpenditureJournalAdapter.class);
                    assertThat(context).doesNotHaveBean("expenditureLocalJournalPostingPort");

                    assertThat(context).hasSingleBean(TaxInvoiceQueryPort.class);
                    assertThat(context).hasSingleBean(HttpExpenditureTaxAdapter.class);
                    assertThat(context).doesNotHaveBean("expenditureLocalTaxInvoiceQueryPort");

                    assertThat(context).hasSingleBean(AssetRegistrationPort.class);
                    assertThat(context).hasSingleBean(HttpExpenditureAssetAdapter.class);
                    assertThat(context).doesNotHaveBean("expenditureLocalAssetRegistrationPort");
                });
    }

    @Test
    void localConfigurationBacksOffWhenRemoteUmbrellaPropertyEnabled() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=local",
                        "expenditure.remote.enabled=true"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean("expenditureLocalMasterDataQueryPort");
                    assertThat(context).doesNotHaveBean("expenditureLocalJournalPostingPort");
                    assertThat(context).doesNotHaveBean("expenditureLocalTaxInvoiceQueryPort");
                    assertThat(context).doesNotHaveBean("expenditureLocalAssetRegistrationPort");
                });
    }
}
