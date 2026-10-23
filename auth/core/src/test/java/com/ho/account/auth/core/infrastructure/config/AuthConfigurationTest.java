package com.ho.account.auth.core.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.exception.DepartmentValidationUnavailableException;
import com.ho.account.auth.core.application.port.out.DepartmentValidationPort;
import com.ho.account.auth.core.infrastructure.persistence.LocalDepartmentValidationAdapter;
import com.ho.account.auth.core.infrastructure.persistence.MasterDataDepartmentValidationAdapter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class AuthConfigurationTest {

    @Test
    void defaultsAreBoundedAndLocalValidationSelectionIsUnchanged() {
        contextRunner()
                .withUserConfiguration(LocalDepartmentValidationAdapter.class,
                        MasterDataDepartmentValidationAdapter.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AuthModuleProperties.MasterData masterData = context.getBean(AuthModuleProperties.class)
                            .getMasterData();
                    assertThat(masterData.validatedConnectTimeoutMillis()).isEqualTo(1_000);
                    assertThat(masterData.validatedReadTimeoutMillis()).isEqualTo(3_000);
                    assertThat(context).hasSingleBean(DepartmentValidationPort.class);
                    assertThat(context.getBean(DepartmentValidationPort.class))
                            .isInstanceOf(LocalDepartmentValidationAdapter.class);
                });
    }

    @ParameterizedTest
    @CsvSource({"1,10000", "10000,1", "250,400"})
    void acceptsCustomTimeoutsAtBothAllowedBoundaries(int connectMillis, int readMillis) {
        contextRunner()
                .withPropertyValues(
                        "auth.master-data.connect-timeout-millis=" + connectMillis,
                        "auth.master-data.read-timeout-millis=" + readMillis,
                        "auth.master-data.enabled=true")
                .withUserConfiguration(LocalDepartmentValidationAdapter.class,
                        MasterDataDepartmentValidationAdapter.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AuthModuleProperties.MasterData masterData = context.getBean(AuthModuleProperties.class)
                            .getMasterData();
                    assertThat(masterData.validatedConnectTimeoutMillis()).isEqualTo(connectMillis);
                    assertThat(masterData.validatedReadTimeoutMillis()).isEqualTo(readMillis);
                    assertThat(context).hasSingleBean(DepartmentValidationPort.class);
                    assertThat(context.getBean(DepartmentValidationPort.class))
                            .isInstanceOf(MasterDataDepartmentValidationAdapter.class);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "10001", "synthetic-invalid-value"})
    void rejectsInvalidConnectTimeoutWithFieldOnlyError(String value) {
        assertRejectedTimeout("connect-timeout-millis", value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "10001", "synthetic-invalid-value"})
    void rejectsInvalidReadTimeoutWithFieldOnlyError(String value) {
        assertRejectedTimeout("read-timeout-millis", value);
    }

    @Test
    void noByteResponseStopsAtConfiguredReadTimeout() throws Exception {
        CountDownLatch releasePeer = new CountDownLatch(1);
        CountDownLatch accepted = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(5_000);
            Future<?> peer = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    accepted.countDown();
                    releasePeer.await(4, TimeUnit.SECONDS);
                    // Close the listener before the peer socket so a default client cannot retry indefinitely.
                    server.close();
                } catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            });

            try {
                contextRunner()
                        .withPropertyValues(
                                "auth.master-data.base-url=http://127.0.0.1:" + server.getLocalPort(),
                                "auth.master-data.read-timeout-millis=200")
                        .run(context -> {
                            assertThat(context).hasNotFailed();
                            var adapter = new MasterDataDepartmentValidationAdapter(context.getBean(RestClient.class));
                            long started = System.nanoTime();
                            assertThatThrownBy(() -> adapter.existsDepartmentCode("SYNTHETIC"))
                                    .isInstanceOf(DepartmentValidationUnavailableException.class)
                                    .hasMessage("Department validation is unavailable")
                                    .hasNoCause()
                                    .hasMessageNotContaining("127.0.0.1")
                                    .hasMessageNotContaining("SYNTHETIC");
                            assertThat(accepted.getCount()).isZero();
                            assertThat(Duration.ofNanos(System.nanoTime() - started))
                                    .isGreaterThanOrEqualTo(Duration.ofMillis(100))
                                    .isLessThan(Duration.ofSeconds(2));
                        });
            } finally {
                releasePeer.countDown();
                peer.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertRejectedTimeout(String fieldName, String value) {
        String token = UUID.randomUUID().toString();
        String inputUrl = "http://synthetic-private-host.invalid/" + UUID.randomUUID();
        new ApplicationContextRunner()
                .withUserConfiguration(AuthConfiguration.class)
                .withPropertyValues(
                        "auth.jwt.secret=" + UUID.randomUUID() + UUID.randomUUID(),
                        "auth.internal-api.token=" + token,
                        "auth.master-data.base-url=" + inputUrl,
                        "auth.master-data." + fieldName + "=" + value)
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    StringBuilder messages = new StringBuilder();
                    while (failure.getCause() != null) {
                        messages.append(failure.getMessage()).append('\n');
                        failure = failure.getCause();
                    }
                    messages.append(failure.getMessage());
                    assertThat(failure).isInstanceOf(IllegalStateException.class)
                            .hasMessage("Fail-Closed Security Violation: auth.master-data."
                                    + fieldName + " must be between 1 and 10000 milliseconds.");
                    assertThat(messages.toString()).doesNotContain(token, inputUrl);
                    if (value.startsWith("synthetic-")) {
                        assertThat(messages.toString()).doesNotContain(value);
                    }
                });
    }

    private static ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(AuthConfiguration.class)
                .withPropertyValues(
                        "auth.jwt.secret=" + UUID.randomUUID() + UUID.randomUUID(),
                        "auth.internal-api.token=" + UUID.randomUUID());
    }
}
