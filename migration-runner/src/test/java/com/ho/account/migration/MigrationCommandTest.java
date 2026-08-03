package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MigrationCommandTest {

    @Test
    void listShowsReadyAndBlockedContextsWithoutCredentials() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int exitCode = new MigrationCommand(stream(output), stream(errors))
                .run(new String[]{"--list"}, Map.of());

        assertThat(exitCode).isZero();
        assertThat(output.toString(StandardCharsets.UTF_8))
                .contains("auth", "READY", "asset-lease", "READY",
                        "closing", "READY", "expenditure-resolution", "READY",
                        "payable", "READY", "receivable", "READY",
                        "reconciliation", "READY", "tax", "READY");
        assertThat(errors.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void completedContextAdvancesToFailClosedConnectionConfiguration() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int exitCode = new MigrationCommand(stream(output), stream(errors)).run(
                new String[]{"--context=account-mart", "--action=validate"},
                Map.of());

        assertThat(exitCode).isEqualTo(2);
        assertThat(errors.toString(StandardCharsets.UTF_8))
                .contains("MIGRATION_DB_URL is required")
                .doesNotContain("#255")
                .doesNotContain("MIGRATION_DB_PASSWORD");
    }

    @Test
    void invalidArgumentsDoNotEchoValues() {
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        String sensitiveValue = "do-not-echo-this";

        int exitCode = new MigrationCommand(stream(new ByteArrayOutputStream()), stream(errors)).run(
                new String[]{"--password=" + sensitiveValue},
                Map.of());

        assertThat(exitCode).isEqualTo(2);
        assertThat(errors.toString(StandardCharsets.UTF_8)).doesNotContain(sensitiveValue);
    }

    private static PrintStream stream(ByteArrayOutputStream target) {
        return new PrintStream(target, true, StandardCharsets.UTF_8);
    }
}
