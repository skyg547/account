package com.ho.account.migration;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

final class MigrationCommand {

    private final PrintStream out;
    private final PrintStream error;

    MigrationCommand(PrintStream out, PrintStream error) {
        this.out = out;
        this.error = error;
    }

    int run(String[] args, Map<String, String> environment) {
        if (args.length == 1 && "--list".equals(args[0])) {
            listContexts();
            return 0;
        }

        try {
            Map<String, String> options = parseOptions(args);
            MigrationContext context = MigrationContext.require(options.get("context"));
            MigrationAction action = MigrationAction.parse(options.get("action"));

            if (!context.cleanDatabaseReady()) {
                error.printf(
                        "Context %s is blocked until GitHub Issue #%d supplies a clean PostgreSQL baseline.%n",
                        context.slug(),
                        context.blockerIssue());
                return 3;
            }
            if (action == MigrationAction.MIGRATE) {
                MigrationConfiguration.requireMigrateApproval(environment);
            }

            MigrationConfiguration configuration = MigrationConfiguration.from(environment, context);
            new DatabaseTargetVerifier().verify(configuration);
            int migrationsExecuted = new MigrationExecutor().execute(context, action, configuration);
            out.printf(
                    "Migration %s completed for %s; executed=%d.%n",
                    action.name().toLowerCase(),
                    context.slug(),
                    migrationsExecuted);
            return 0;
        } catch (IllegalArgumentException exception) {
            error.println(exception.getMessage());
            usage();
            return 2;
        } catch (RuntimeException exception) {
            error.println("Migration operation failed; credentials and database address were not logged.");
            return 4;
        }
    }

    private Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String argument : args) {
            if (!argument.startsWith("--") || !argument.contains("=")) {
                throw new IllegalArgumentException("Arguments must use --name=value syntax");
            }
            String[] pair = argument.substring(2).split("=", 2);
            if (!("context".equals(pair[0]) || "action".equals(pair[0])) || pair[1].isBlank()) {
                throw new IllegalArgumentException("Only non-empty --context and --action are supported");
            }
            if (options.put(pair[0], pair[1]) != null) {
                throw new IllegalArgumentException("Duplicate option: --" + pair[0]);
            }
        }
        return options;
    }

    private void listContexts() {
        MigrationContext.all().forEach(context -> {
            String state = context.cleanDatabaseReady()
                    ? "READY"
                    : "BLOCKED (#" + context.blockerIssue() + ")";
            out.printf("%-24s %s%n", context.slug(), state);
        });
    }

    private void usage() {
        error.println("Usage: java -jar account-migration-runner.jar --list");
        error.println("   or: java -jar account-migration-runner.jar --context=<slug> --action=validate|migrate");
    }
}
