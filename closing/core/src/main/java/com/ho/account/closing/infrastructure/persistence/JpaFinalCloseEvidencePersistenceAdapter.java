package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.FinalCloseEvidencePersistencePort;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class JpaFinalCloseEvidencePersistenceAdapter implements FinalCloseEvidencePersistencePort {

    static final String PROVIDER_ID_UNIQUE_CONSTRAINT = "uk_final_close_evidence_set_provider_id";

    private final FinalCloseEvidencePersistenceTransactions transactions;

    public JpaFinalCloseEvidencePersistenceAdapter(FinalCloseEvidencePersistenceTransactions transactions) {
        this.transactions = transactions;
    }

    @Override
    public FinalCloseEvidenceSet append(FinalCloseEvidenceSet evidenceSet) {
        Objects.requireNonNull(evidenceSet, "evidenceSet must not be null");

        Optional<FinalCloseEvidenceSet> existing =
                transactions.findByEvidenceSetId(evidenceSet.evidenceSetId());
        if (existing.isPresent()) {
            return requireSameDigest(evidenceSet, existing.orElseThrow());
        }

        try {
            return transactions.insert(evidenceSet);
        } catch (DataIntegrityViolationException conflict) {
            if (!isProviderIdUniqueViolation(conflict)) {
                throw conflict;
            }
            // REQUIRES_NEW has rolled the failed insert back before this reload, which is required
            // on PostgreSQL because a constraint error aborts the transaction that observed it.
            return transactions.findByEvidenceSetId(evidenceSet.evidenceSetId())
                    .map(persisted -> requireSameDigest(evidenceSet, persisted))
                    .orElseThrow(() -> conflict);
        }
    }

    @Override
    public Optional<FinalCloseEvidenceSet> findByEvidenceSetId(String evidenceSetId) {
        Objects.requireNonNull(evidenceSetId, "evidenceSetId must not be null");
        return transactions.findByEvidenceSetId(evidenceSetId);
    }

    @Override
    public Optional<FinalCloseEvidenceSet> findLatestByCalendarId(Long calendarId) {
        Objects.requireNonNull(calendarId, "calendarId must not be null");
        return transactions.findLatestByCalendarId(calendarId);
    }

    private FinalCloseEvidenceSet requireSameDigest(
            FinalCloseEvidenceSet requested,
            FinalCloseEvidenceSet persisted) {
        if (!requested.contentDigest().equals(persisted.contentDigest())) {
            throw new IllegalStateException(
                    "evidenceSetId already exists with different content: " + requested.evidenceSetId());
        }
        return persisted;
    }

    private boolean isProviderIdUniqueViolation(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && containsConstraintName(constraintViolation.getConstraintName())) {
                return true;
            }
            if (cause instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())
                    && containsConstraintName(sqlException.getMessage())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsConstraintName(String value) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(PROVIDER_ID_UNIQUE_CONSTRAINT);
    }
}
