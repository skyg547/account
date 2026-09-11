package com.ho.account.deposit.batch;

import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.deposit.application.service.DepositService;
import com.ho.account.deposit.infrastructure.adapter.out.external.HttpDepositJournalPostingAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.external.HttpDepositMasterDataAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.local.LocalDepositJournalPostingAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.local.LocalDepositMasterDataAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks dev adapter selection at the actual batch composition root; schema behavior is outside this test. */
@ActiveProfiles("dev")
@SpringBootTest(classes = DepositBatchApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.batch.job.enabled=false", "spring.batch.jdbc.initialize-schema=always",
                "spring.datasource.url=jdbc:h2:mem:deposit_batch_dev_wiring;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
                "spring.cloud.config.enabled=false", "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false", "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false", "management.tracing.enabled=false",
                "account.deposit.remote.enabled=true",
                "account.deposit.master-data-base-url=http://reference.invalid",
                "account.deposit.journal-base-url=http://journal.invalid",
                "account.deposit.account-mapping.cash-account-code=10100",
                "account.deposit.account-mapping.deposit-liability-account-code=20200",
                "account.deposit.outbox.scheduler.enabled=false"
        })
class DepositBatchDevProfileTest {
    @Autowired ApplicationContext context;
    @Autowired JobRegistry jobRegistry;

    @Test
    void devSelectsOnlyHttpPortsAndRegistersIntegrityJob() throws Exception {
        // Do not override local-adapters in test properties: application-dev.yml must disable the default itself.
        assertThat(context.getEnvironment().getProperty("account.deposit.local-adapters.enabled", Boolean.class))
                .isFalse();
        assertThat(context.getBeansOfType(DepositService.class)).hasSize(1);
        assertThat(context.getBeansOfType(MasterDataQueryPort.class)).hasSize(1);
        assertThat(context.getBean(MasterDataQueryPort.class)).isInstanceOf(HttpDepositMasterDataAdapter.class);
        assertThat(context.getBeansOfType(JournalPostingPort.class)).hasSize(1);
        assertThat(context.getBean(JournalPostingPort.class)).isInstanceOf(HttpDepositJournalPostingAdapter.class);
        assertThat(context.getBeansOfType(LocalDepositMasterDataAdapter.class)).isEmpty();
        assertThat(context.getBeansOfType(LocalDepositJournalPostingAdapter.class)).isEmpty();
        assertThat(jobRegistry.getJob("depositAccountIntegrityJob"))
                .isSameAs(context.getBean("depositAccountIntegrityJob", Job.class));
    }
}
