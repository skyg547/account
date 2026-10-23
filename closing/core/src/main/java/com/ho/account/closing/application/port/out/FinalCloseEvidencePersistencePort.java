package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import java.util.Optional;

/**
 * Append-only final-close evidence store.
 *
 * <p>{@link #append(FinalCloseEvidenceSet)} must return the existing row for the same provider set
 * ID and digest, and reject the same set ID with different content. Latest selection must order by
 * snapshot observation time and then the store's append sequence so an older PASS cannot mask a
 * newer FAIL.
 */
public interface FinalCloseEvidencePersistencePort {

    FinalCloseEvidenceSet append(FinalCloseEvidenceSet evidenceSet);

    /** Recover the immutable snapshot bound to an already prepared close intent. */
    Optional<FinalCloseEvidenceSet> findByEvidenceSetId(String evidenceSetId);

    Optional<FinalCloseEvidenceSet> findLatestByCalendarId(Long calendarId);
}
