package com.ho.account.internalaudit.core.infrastructure.persistence.adapter;

import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.domain.CommandReceipt;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC joins the application's JPA transaction through its shared DataSource.
 * Receipts must never commit independently of business changes and audit append.
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CommandReceiptPersistenceAdapter implements CommandReceiptPort {

    private static final int LOCK_BUCKET_COUNT = 256;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void lockKey(String key) {
        String normalizedKey = normalizeKey(key);
        // Preseeded rows also serialize the first use of a key. Hash collisions only
        // reduce concurrency; receipt identity always uses the complete original key.
        int bucket = Math.floorMod(normalizedKey.hashCode(), LOCK_BUCKET_COUNT);
        jdbcTemplate.queryForObject(
                "SELECT bucket FROM internal_audit_key_lock WHERE bucket = ? FOR UPDATE",
                Integer.class, bucket);
    }

    @Override
    public Optional<CommandReceipt> findByKey(String key) {
        return jdbcTemplate.query("""
                SELECT idempotency_key, fingerprint_version, fingerprint, snapshot_version, snapshot_json
                FROM internal_audit_command_receipt WHERE idempotency_key = ?
                """, (rs, rowNum) -> new CommandReceipt(
                        rs.getString("idempotency_key"), rs.getInt("fingerprint_version"),
                        rs.getString("fingerprint"), rs.getInt("snapshot_version"),
                        rs.getString("snapshot_json")), normalizeKey(key)).stream().findFirst();
    }

    @Override
    public void reserve(String key, int fingerprintVersion, String fingerprint) {
        String normalizedKey = normalizeKey(key);
        if (fingerprintVersion <= 0 || fingerprint == null || !fingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("A versioned SHA-256 command fingerprint is required");
        }
        // A uniqueness failure is a real transaction failure, never a retry signal
        // to recover by reading from a transaction the database has already aborted.
        jdbcTemplate.update("""
                INSERT INTO internal_audit_command_receipt
                    (idempotency_key, fingerprint_version, fingerprint, snapshot_version, snapshot_json)
                VALUES (?, ?, ?, 0, NULL)
                """, normalizedKey, fingerprintVersion, fingerprint);
    }

    @Override
    public void complete(String key, int snapshotVersion, String snapshotJson) {
        String normalizedKey = normalizeKey(key);
        if (snapshotVersion <= 0 || snapshotJson == null || snapshotJson.isBlank()) {
            throw new IllegalArgumentException("A versioned command result snapshot is required");
        }
        int changed = jdbcTemplate.update("""
                UPDATE internal_audit_command_receipt SET snapshot_version = ?, snapshot_json = ?
                WHERE idempotency_key = ? AND snapshot_version = 0 AND snapshot_json IS NULL
                """, snapshotVersion, snapshotJson, normalizedKey);
        if (changed != 1) {
            throw new IllegalStateException("Command receipt must be reserved and completed exactly once");
        }
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("A nonblank idempotency key is required");
        }
        String normalizedKey = key.trim();
        if (normalizedKey.isEmpty() || normalizedKey.length() > 255) {
            throw new IllegalArgumentException("Idempotency key must contain 1 to 255 characters");
        }
        return normalizedKey;
    }
}
