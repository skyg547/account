package com.ho.account.migration;

/**
 * Release-time database migration entry point.
 *
 * <p>This application deliberately does not start a Spring container or an HTTP server. The
 * executable Boot jar only supplies an isolated classpath for Flyway, the PostgreSQL driver and
 * context-owned SQL resources.</p>
 */
public final class DatabaseMigrationApplication {

    private DatabaseMigrationApplication() {
    }

    public static void main(String[] args) {
        int exitCode = new MigrationCommand(System.out, System.err).run(args, System.getenv());
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
