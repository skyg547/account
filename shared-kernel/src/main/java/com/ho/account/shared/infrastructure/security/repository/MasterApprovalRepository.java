package com.ho.account.shared.infrastructure.security.repository;

import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterApprovalRepository extends JpaRepository<MasterApproval, Long> {
    List<MasterApproval> findByStatus(MasterApproval.ApprovalStatus status);

    List<MasterApproval> findByMasterTypeAndMasterKey(String masterType, String masterKey);
}
