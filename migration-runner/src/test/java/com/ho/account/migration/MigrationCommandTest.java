package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
                        "internal-audit", "READY",
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

        assertInvalidInputResult(exitCode, output, errors);
        assertThat(errors.toString(StandardCharsets.UTF_8))
                .doesNotContain("#255")
                .doesNotContain("MIGRATION_DB_PASSWORD");
    }

    @Test
    void invalidArgumentsDoNotEchoValues() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        String sensitiveValue = "do-not-echo-this";

        int exitCode = new MigrationCommand(stream(output), stream(errors)).run(
                new String[]{"--password=" + sensitiveValue},
                Map.of());

        assertInvalidInputResult(exitCode, output, errors);
        assertThat(errors.toString(StandardCharsets.UTF_8)).doesNotContain(sensitiveValue);
    }

    @ParameterizedTest
    @ValueSource(strings = {"%zz", "%", "%2"})
    void malformedUrlDoesNotExposeAnyInputInEitherStream(String malformedEscape) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        String host = "synthetic-private-host.example";
        String database = "auth_synthetic_private_database";
        String queryMarker = "synthetic-private-query";
        String url = "jdbc:postgresql://" + host + "/" + database
                + "?sslmode=verify-full&applicationName=" + queryMarker + malformedEscape;

        // Canonical TLS and matching DB binding reach URI parsing, which fails before any connection.
        int exitCode = new MigrationCommand(stream(output), stream(errors)).run(
                new String[]{"--context=auth", "--action=validate"},
                Map.of("MIGRATION_DB_URL", url,
                        "MIGRATION_TARGET_ENV", "production",
                        "MIGRATION_EXPECTED_DATABASE", database,
                        "MIGRATION_DB_USER", "synthetic_migrator",
                        "MIGRATION_DB_PASSWORD", "synthetic-password"));

        assertThat(output.toString(StandardCharsets.UTF_8))
                .doesNotContain(url, host, database, queryMarker);
        assertThat(errors.toString(StandardCharsets.UTF_8))
                .doesNotContain(url, host, database, queryMarker);
        assertInvalidInputResult(exitCode, output, errors);
    }

    @ParameterizedTest
    @MethodSource("invalidCommandArguments")
    void invalidArgumentShapesUseOnlyTheSafeDiagnosticAndUsage(String[] arguments) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int exitCode = new MigrationCommand(stream(output), stream(errors)).run(arguments, Map.of());

        assertInvalidInputResult(exitCode, output, errors);
    }

    private static Stream<Arguments> invalidCommandArguments() {
        return Stream.of(
                Arguments.of((Object) new String[]{"invalid-syntax-marker"}),
                Arguments.of((Object) new String[]{"--context=auth", "--context=auth"}),
                Arguments.of((Object) new String[]{"--action=validate", "--action=validate"}),
                Arguments.of((Object) new String[]{"--context=", "--action=validate"}),
                Arguments.of((Object) new String[]{"--context=auth", "--action= "}),
                Arguments.of((Object) new String[]{"--action=validate"}),
                Arguments.of((Object) new String[]{"--context=auth"}),
                Arguments.of((Object) new String[]{"--context=auth", "--action=invalid-action-marker"}),
                Arguments.of((Object) new String[]{"--context=invalid-context-marker", "--action=validate"}));
    }

    @Test
    void runtimeFailureKeepsItsSeparateSafeDiagnosticWithoutUsageOrExceptionDetails() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        String marker = "synthetic-runtime-exception-marker";
        String causeMarker = "synthetic-runtime-cause-marker";
        // Fail at the environment boundary to exercise the real catch without contacting a database.
        Map<String, String> failingEnvironment = new HashMap<>() {
            @Override
            public String get(Object key) {
                throw new IllegalStateException(marker, new IllegalStateException(causeMarker));
            }
        };

        int exitCode = new MigrationCommand(stream(output), stream(errors)).run(
                new String[]{"--context=auth", "--action=validate"}, failingEnvironment);

        assertThat(exitCode).isEqualTo(4);
        assertThat(output.toString(StandardCharsets.UTF_8)).isEmpty();
        assertThat(errors.toString(StandardCharsets.UTF_8))
                .isEqualTo("Migration operation failed; credentials and database address were not logged."
                        + System.lineSeparator())
                .doesNotContain(marker, causeMarker, "Usage:", "IllegalStateException", "\tat ");
    }

    private static void assertInvalidInputResult(
            int exitCode, ByteArrayOutputStream output, ByteArrayOutputStream errors) {
        assertThat(exitCode).isEqualTo(2);
        assertThat(output.toString(StandardCharsets.UTF_8)).isEmpty();
        assertThat(errors.toString(StandardCharsets.UTF_8)).isEqualTo(String.join(System.lineSeparator(),
                "Invalid migration arguments or configuration; values were not logged.",
                "Usage: java -jar account-migration-runner.jar --list",
                "   or: java -jar account-migration-runner.jar --context=<slug> --action=validate|migrate",
                ""));
    }

    private static PrintStream stream(ByteArrayOutputStream target) {
        return new PrintStream(target, true, StandardCharsets.UTF_8);
    }
}
