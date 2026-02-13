package com.ho.account.audit.repository;

import com.ho.account.audit.domain.MasterApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterApprovalRepository extends JpaRepository<MasterApproval, Long> {
    List<MasterApproval> findByStatus(MasterApproval.ApprovalStatus status);

    List<MasterApproval> findByMasterTypeAndMasterKey(String masterType, String masterKey);
}
