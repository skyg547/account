package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.MasterDataChangeRequestEntity;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface MasterDataChangeRequestJpaRepository extends JpaRepository<MasterDataChangeRequestEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM MasterDataChangeRequestEntity request WHERE request.id = :id")
    Optional<MasterDataChangeRequestEntity> findByIdForUpdate(Long id);

    Optional<MasterDataChangeRequestEntity> findBySourceReference(String sourceReference);

    List<MasterDataChangeRequestEntity> findByStatusOrderByRequestedAtAsc(ChangeStatus status);

    List<MasterDataChangeRequestEntity> findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateAscRequestedAtAsc(
            ChangeStatus status,
            LocalDate effectiveDate,
            Pageable pageable);
}

