package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface MasterDataChangeRequestJpaRepository extends JpaRepository<MasterDataChangeRequest, Long> {

    List<MasterDataChangeRequest> findByStatusOrderByRequestedAtAsc(ChangeStatus status);

    List<MasterDataChangeRequest> findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateAscRequestedAtAsc(
            ChangeStatus status,
            LocalDate effectiveDate,
            Pageable pageable);
}
