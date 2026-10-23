package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.exception.MasterDataIdempotencyConflictException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Local adapter regression; the real PostgreSQL test separately proves database conflict behavior. */
@SpringBootTest(classes = MasterDataChangeRequestPostgresqlIdempotencyTest.TestApplication.class,
        properties = {
                "spring.config.name=change-request-h2-concurrent-test",
                "spring.datasource.url=jdbc:h2:mem:change-request-idempotency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.hikari.maximum-pool-size=5",
                "spring.flyway.enabled=true",
                "spring.flyway.target=8.1",
                "spring.flyway.locations=classpath:db/migration,classpath:db/local-migration,classpath:db/migration-h2",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:db/local/schema-business-partner-screening.sql,classpath:db/local/schema-fiscal-period-transitions.sql",
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        })
@Timeout(30)
class MasterDataChangeRequestH2ConcurrentIdempotencyTest {

    @Autowired private MasterDataChangeRequestUseCase requests;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void sourceReferenceLockSerializesIdenticalAndMismatchedFirstSubmissions() throws Exception {
        contend(false);
        contend(true);
    }

    private void contend(boolean mismatch) throws Exception {
        String source = "h2-source-" + key();
        MasterDataChangeRequestCommand firstCommand = command(key(), source);
        MasterDataChangeRequestCommand secondCommand = mismatch ? command(key(), source) : firstCommand;
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch contenderStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MasterDataChangeRequest> first = executor.submit(() ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        MasterDataChangeRequest request = requests.requestChange(firstCommand);
                        inserted.countDown();
                        await(release);
                        return request;
                    }));
            await(inserted);
            Future<MasterDataChangeRequest> second = executor.submit(() ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        contenderStarted.countDown();
                        return requests.requestChange(secondCommand);
                    }));
            await(contenderStarted);
            // Give the contender time to reach the lock while the first transaction is held.
            // PostgreSQL's separate test verifies the actual backend blocking graph.
            Thread.sleep(150);
            assertThat(second.isDone()).isFalse();
            release.countDown();
            MasterDataChangeRequest winner = first.get(15, TimeUnit.SECONDS);
            if (mismatch) {
                assertThatThrownBy(() -> second.get(15, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(MasterDataIdempotencyConflictException.class);
            } else {
                MasterDataChangeRequest replay = second.get(15, TimeUnit.SECONDS);
                assertThat(replay.getId()).isEqualTo(winner.getId());
            }
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM master_data_change_requests WHERE source_reference = ?",
                    Integer.class, source)).isEqualTo(1);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static MasterDataChangeRequestCommand command(String key, String source) {
        return new MasterDataChangeRequestCommand(MasterDataType.DEPARTMENT, key,
                ChangeType.CREATE, LocalDate.now().minusDays(1), 1,
                "requester", "create", "{\"code\":\"" + key + "\",\"name\":\"Department\"}", source);
    }

    private static String key() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) throw new AssertionError("Concurrency barrier timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Concurrency barrier interrupted", exception);
        }
    }

}
