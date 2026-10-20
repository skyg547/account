package com.ho.account.closing.config;

import com.ho.account.closing.application.port.out.AnnualJournalReadPort;
import com.ho.account.closing.infrastructure.source.ClosingReadOnlySources;
import com.ho.account.closing.infrastructure.source.JdbcAnnualJournalReadAdapter;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnnualJournalReadConfigurationTest {
    private final DataSource primary = mock(DataSource.class);
    private final ClosingReadOnlySources sources = mock(ClosingReadOnlySources.class);
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AnnualJournalReadConfiguration.class,
                    JdbcAnnualJournalReadAdapter.Wiring.class)
            .withBean(DataSource.class, () -> primary)
            .withBean(ClosingReadOnlySources.class, () -> sources);

    @Test
    void nonDevEmbeddedUsesPrimaryWhenSourceFlagIsAbsent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AnnualJournalReadPort.class);
            assertThat(context.getBean(AnnualJournalReadPort.class))
                    .isInstanceOf(JdbcAnnualJournalReadAdapter.class);
        });
    }

    @Test
    void nonDevIgnoresSeparateSourceFlagAndKeepsPrimaryProvider() {
        when(sources.journalDataSource()).thenReturn(primary);
        runner.withPropertyValues("closing.sources.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(AnnualJournalReadPort.class);
            assertThat(context.getBeanNamesForType(AnnualJournalReadPort.class))
                    .containsExactly("embeddedAnnualJournalReadPort");
        });
    }

    @Test
    void devEnabledUsesOnlySeparateSourceProvider() {
        when(sources.journalDataSource()).thenReturn(primary);
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
                .withPropertyValues("closing.sources.enabled=true")
                .run(context -> assertThat(context.getBeanNamesForType(AnnualJournalReadPort.class))
                        .containsExactly("annualJournalReadPort"));
    }

    @Test
    void devWithoutApprovedSourceDoesNotOfferAnnualProvider() {
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
                .run(context -> assertThat(context).doesNotHaveBean(AnnualJournalReadPort.class));
    }

    @Test
    void localH2FixtureDoesNotOfferPostgresqlAnnualProvider() {
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
                .run(context -> assertThat(context).doesNotHaveBean(AnnualJournalReadPort.class));
    }
}
