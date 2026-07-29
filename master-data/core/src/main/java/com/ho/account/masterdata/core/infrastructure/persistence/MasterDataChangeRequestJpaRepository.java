package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface MasterDataChangeRequestJpaRepository extends JpaRepository<MasterDataChangeRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM MasterDataChangeRequest request WHERE request.id = :id")
    Optional<MasterDataChangeRequest> findByIdForUpdate(Long id);

    Optional<MasterDataChangeRequest> findBySourceReference(String sourceReference);

    List<MasterDataChangeRequest> findByStatusOrderByRequestedAtAsc(ChangeStatus status);

    List<MasterDataChangeRequest> findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateAscRequestedAtAsc(
            ChangeStatus status,
            LocalDate effectiveDate,
            Pageable pageable);
}
