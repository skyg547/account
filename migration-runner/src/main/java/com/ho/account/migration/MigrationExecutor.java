package com.ho.account.migration;

import org.flywaydb.core.Flyway;

final class MigrationExecutor {

    private static final String SHARED_LOCATION = "classpath:db/shared/postgresql";
    private static final String BATCH_HISTORY_TABLE = "flyway_schema_history_batch";

    int execute(
            MigrationContext context,
            MigrationAction action,
            MigrationConfiguration configuration) {
        Flyway contextFlyway = configureContext(configuration, context);
        Flyway batchFlyway = configureBatch(configuration);

        if (action == MigrationAction.VALIDATE) {
            validateCurrent(contextFlyway, "context");
            validateCurrent(batchFlyway, "Spring Batch metadata");
            new BatchMetadataVerifier().verify(configuration);
            return 0;
        }
        int contextMigrations = contextFlyway.migrate().migrationsExecuted;
        int batchMigrations = batchFlyway.migrate().migrationsExecuted;
        new BatchMetadataVerifier().verify(configuration);
        return contextMigrations + batchMigrations;
    }

    private Flyway configureContext(
            MigrationConfiguration configuration,
            MigrationContext context) {
        return Flyway.configure()
                .dataSource(configuration.url(), configuration.user(), configuration.password())
                .locations(context.location())
                .table(context.historyTable())
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .load();
    }

    private Flyway configureBatch(MigrationConfiguration configuration) {
        return Flyway.configure()
                .dataSource(configuration.url(), configuration.user(), configuration.password())
                .locations(SHARED_LOCATION)
                .table(BATCH_HISTORY_TABLE)
                // The isolated Batch history starts at zero even when domain tables already exist.
                // V1 is therefore always applied and is never mistaken for an existing domain V1.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .baselineDescription("Spring Batch metadata history initialization")
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .load();
    }

    private void validateCurrent(Flyway flyway, String migrationSet) {
        flyway.validate();
        int pendingCount = flyway.info().pending().length;
        if (pendingCount > 0) {
            throw new IllegalStateException(
                    migrationSet + " validation found " + pendingCount + " pending migrations");
        }
    }
}
