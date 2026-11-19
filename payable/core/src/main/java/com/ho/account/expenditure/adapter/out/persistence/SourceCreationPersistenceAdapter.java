package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.SourceCreationPersistencePort;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component("payableSourceCreationPersistenceAdapter")
@Transactional(propagation = Propagation.MANDATORY)
public class SourceCreationPersistenceAdapter implements SourceCreationPersistencePort {
    private final JdbcTemplate jdbc;

    public SourceCreationPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void acquireLock(String sourceType, String businessKey) {
        // Preseeded stripes serialize even the first request; the fingerprint never selects the lock.
        int stripe = Math.floorMod(Objects.hash(sourceType, businessKey), 64);
        jdbc.queryForObject("SELECT stripe_id FROM payable_source_creation_locks WHERE stripe_id = ? FOR UPDATE",
                Integer.class, stripe);
    }

    @Override
    public Optional<Receipt> find(String sourceType, String businessKey) {
        return jdbc.query("SELECT request_fingerprint, source_id FROM payable_source_creations"
                        + " WHERE source_type = ? AND business_key = ?",
                (rs, row) -> new Receipt(rs.getString(1), rs.getLong(2)), sourceType, businessKey)
                .stream().findFirst();
    }

    @Override
    public void save(String sourceType, String businessKey, String requestFingerprint, Long sourceId) {
        jdbc.update("INSERT INTO payable_source_creations"
                        + " (source_type, business_key, request_fingerprint, source_id) VALUES (?, ?, ?, ?)",
                sourceType, businessKey, requestFingerprint, sourceId);
    }
}
