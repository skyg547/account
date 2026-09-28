package com.ho.account.closing.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface FinalCloseEvidenceControlRepository extends JpaRepository<FinalCloseEvidenceControlEntity, Long> {

    List<FinalCloseEvidenceControlEntity>
            findByEvidenceSetDbIdOrderByTypeAscSourceSystemAscSourceRunIdAscIdAsc(Long evidenceSetDbId);
}
