package com.ho.account.migration;

import java.util.Locale;

enum MigrationAction {
    VALIDATE,
    MIGRATE;

    static MigrationAction parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("--action must be validate or migrate");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("--action must be validate or migrate");
        }
    }
}
