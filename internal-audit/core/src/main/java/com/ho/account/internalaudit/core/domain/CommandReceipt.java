package com.ho.account.internalaudit.core.domain;

/**
 * Durable command identity and first result. Version zero with a null snapshot is
 * a reservation visible only inside the transaction that executes the command.
 */
public record CommandReceipt(
        String idempotencyKey,
        int fingerprintVersion,
        String fingerprint,
        int snapshotVersion,
        String snapshotJson) {}
