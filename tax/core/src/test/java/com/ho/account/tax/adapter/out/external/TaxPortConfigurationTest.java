package com.ho.account.tax.adapter.out.external;

import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.tax.infrastructure.local.TaxLocalExternalPortConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TaxPortConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    TaxLocalExternalPortConfiguration.class,
                    HttpTaxMasterDataAdapter.class
            ));

    @Test
    void localProfileWithoutRemotePropertyEnablesLocalStubsOnly() {
        runner.withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(TaxLocalExternalPortConfiguration.class);
                    assertThat(context).hasSingleBean(MasterDataQueryPort.class);
                    assertThat(context).doesNotHaveBean(HttpTaxMasterDataAdapter.class);
                });
    }

    @Test
    void remoteEnabledActivatesHttpAdapterAndDisablesLocalStubs() {
        runner.withPropertyValues(
                        "spring.profiles.active=local",
                        "tax.master-data.remote.enabled=true",
                        "tax.master-data.base-url=http://master-data.test"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TaxLocalExternalPortConfiguration.class);
                    assertThat(context).hasSingleBean(HttpTaxMasterDataAdapter.class);
                    assertThat(context).hasSingleBean(MasterDataQueryPort.class);
                });
    }
}
