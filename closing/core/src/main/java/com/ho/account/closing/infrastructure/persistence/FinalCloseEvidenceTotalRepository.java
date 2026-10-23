package com.ho.account.closing.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface FinalCloseEvidenceTotalRepository extends JpaRepository<FinalCloseEvidenceTotalEntity, Long> {

    List<FinalCloseEvidenceTotalEntity> findByControlIdInOrderByControlIdAscAccountCodeAscCurrencyCodeAscIdAsc(
            Collection<Long> controlIds);
}
