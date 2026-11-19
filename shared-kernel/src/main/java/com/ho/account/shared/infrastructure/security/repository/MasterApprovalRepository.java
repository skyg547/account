package com.ho.account.shared.infrastructure.security.repository;

import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterApprovalRepository extends JpaRepository<MasterApproval, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from MasterApproval a where a.id = :id")
    java.util.Optional<MasterApproval> findByIdForDecision(@org.springframework.data.repository.query.Param("id") Long id);

    List<MasterApproval> findByStatus(MasterApproval.ApprovalStatus status);

    List<MasterApproval> findByMasterTypeAndMasterKey(String masterType, String masterKey);
}
