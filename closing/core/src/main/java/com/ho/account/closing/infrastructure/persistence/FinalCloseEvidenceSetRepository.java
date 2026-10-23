package com.ho.account.closing.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface FinalCloseEvidenceSetRepository extends JpaRepository<FinalCloseEvidenceSetEntity, Long> {

    Optional<FinalCloseEvidenceSetEntity> findByEvidenceSetId(String evidenceSetId);

    Optional<FinalCloseEvidenceSetEntity> findFirstByCalendarIdOrderByObservedAtDescIdDesc(Long calendarId);
}
