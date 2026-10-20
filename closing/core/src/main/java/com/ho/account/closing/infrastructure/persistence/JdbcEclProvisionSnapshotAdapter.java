package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.EclProvisionSnapshotPort;
import com.ho.account.closing.domain.EclProvisionSnapshot;
import java.sql.Date;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Closing-owned durable binding; its independent commit survives a later remote Journal failure. */
public final class JdbcEclProvisionSnapshotAdapter implements EclProvisionSnapshotPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public JdbcEclProvisionSnapshotAdapter(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.transaction = new TransactionTemplate(Objects.requireNonNull(manager));
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void recordAll(List<EclProvisionSnapshot> snapshots, Map<String, String> postedNoopReferences) {
        Objects.requireNonNull(snapshots);
        Objects.requireNonNull(postedNoopReferences);
        if (snapshots.isEmpty()) throw new IllegalArgumentException("ECL snapshots must not be empty");
        EclProvisionSnapshot first = snapshots.get(0);
        if (snapshots.stream().anyMatch(snapshot -> !snapshot.baseDate().equals(first.baseDate())
                || snapshot.provisionBatchId() != first.provisionBatchId())) {
            throw new IllegalArgumentException("ECL snapshot reservation must contain one date and batch");
        }
        String setDigest = snapshotSetDigest(snapshots);
        transaction.executeWithoutResult(status -> {
            List<String> runBinding = jdbc.query("""
                    SELECT snapshot_set_digest FROM ecl_provision_run_bindings
                     WHERE base_date = ? AND provision_batch_id = ?
                    """, (rs, row) -> rs.getString(1), Date.valueOf(first.baseDate()), first.provisionBatchId());
            if (!runBinding.isEmpty() && !runBinding.get(0).equals(setDigest)) {
                throw new IllegalStateException("ECL operation key is bound to a different confirmed snapshot set");
            }
            if (runBinding.isEmpty()) jdbc.update("""
                    INSERT INTO ecl_provision_run_bindings
                        (base_date, provision_batch_id, snapshot_set_digest) VALUES (?, ?, ?)
                    """, Date.valueOf(first.baseDate()), first.provisionBatchId(), setDigest);
            for (EclProvisionSnapshot snapshot : snapshots) {
                List<ExistingEvidence> existing = jdbc.query("""
                        SELECT s.snapshot_reference, s.source_fingerprint,
                               s.existing_transaction_amount, s.existing_base_amount,
                               s.adjustment_transaction_amount, s.adjustment_base_amount, s.closing_rate
                          FROM ecl_provision_snapshot_bindings b
                          JOIN ecl_provision_snapshots s ON s.snapshot_reference = b.snapshot_reference
                         WHERE b.operation_key = ?
                        """, (rs, row) -> new ExistingEvidence(
                        rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4),
                        rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7)), snapshot.operationKey());
                if (existing.isEmpty() && postedNoopReferences.containsKey(snapshot.operationKey())) {
                    throw new IllegalStateException("POSTED ECL slip has no original snapshot binding: "
                            + snapshot.operationKey());
                }
                if (!existing.isEmpty()) {
                    if (!existing.get(0).reference().equals(snapshot.reference())
                            && !isOriginalPostedNoop(snapshot, existing.get(0), postedNoopReferences)) {
                        throw new IllegalStateException("ECL operation key is bound to a different confirmed snapshot: "
                                + snapshot.operationKey());
                    }
                    continue;
                }
                // The primary key also fails closed under concurrent first writers. A racing retry
                // may fail once, then succeed after the winner commits its matching identity.
                List<String> source = jdbc.query(
                        "SELECT snapshot_reference FROM ecl_provision_snapshots WHERE snapshot_reference = ?",
                        (rs, row) -> rs.getString(1), snapshot.reference());
                if (source.isEmpty()) jdbc.update("""
                        INSERT INTO ecl_provision_snapshots (
                            snapshot_reference, source_fingerprint, base_date,
                            run_id, model_version, legal_entity_code, currency_code, allowance_account_code,
                            target_allowance_amount, source_exposure_amount, stage1_allowance_amount,
                            stage2_allowance_amount, stage3_allowance_amount,
                            existing_transaction_amount, existing_base_amount, closing_rate,
                            adjustment_transaction_amount, adjustment_base_amount)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, snapshot.reference(), snapshot.sourceFingerprint(), Date.valueOf(snapshot.baseDate()),
                        snapshot.runId(), snapshot.modelVersion(),
                        snapshot.legalEntityCode(), snapshot.currencyCode(), snapshot.allowanceAccountCode(),
                        snapshot.targetAllowanceAmount(), snapshot.sourceExposureAmount(),
                        snapshot.stage1AllowanceAmount(), snapshot.stage2AllowanceAmount(),
                        snapshot.stage3AllowanceAmount(), snapshot.existingTransactionAmount(),
                        snapshot.existingBaseAmount(), snapshot.closingRate(),
                        snapshot.adjustmentTransactionAmount(), snapshot.adjustmentBaseAmount());
                jdbc.update("""
                        INSERT INTO ecl_provision_snapshot_bindings
                            (operation_key, provision_batch_id, snapshot_reference)
                        VALUES (?, ?, ?)
                        """, snapshot.operationKey(), snapshot.provisionBatchId(), snapshot.reference());
            }
        });
    }

    private String snapshotSetDigest(List<EclProvisionSnapshot> snapshots) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            snapshots.stream().map(EclProvisionSnapshot::sourceFingerprint).sorted()
                    .forEach(reference -> sha.update(reference.getBytes(StandardCharsets.US_ASCII)));
            return HexFormat.of().formatHex(sha.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private boolean isOriginalPostedNoop(EclProvisionSnapshot current, ExistingEvidence original,
                                         Map<String, String> postedNoopReferences) {
        // Preserve the first source/reconciliation row. A posted retry is explained only when
        // that exact Journal lineage accounts for both current ledger balances and no new delta.
        return original.reference().equals(postedNoopReferences.get(current.operationKey()))
                && original.sourceFingerprint().equals(current.sourceFingerprint())
                && original.adjustmentTransaction().signum() != 0
                && original.adjustmentBase().signum() != 0
                && current.adjustmentTransactionAmount().signum() == 0
                && current.adjustmentBaseAmount().signum() == 0
                && original.closingRate().compareTo(current.closingRate()) == 0
                && original.existingTransaction().add(original.adjustmentTransaction())
                        .compareTo(current.existingTransactionAmount()) == 0
                && original.existingBase().add(original.adjustmentBase())
                        .compareTo(current.existingBaseAmount()) == 0;
    }

    private record ExistingEvidence(String reference, String sourceFingerprint,
                                    BigDecimal existingTransaction, BigDecimal existingBase,
                                    BigDecimal adjustmentTransaction, BigDecimal adjustmentBase,
                                    BigDecimal closingRate) { }
}
