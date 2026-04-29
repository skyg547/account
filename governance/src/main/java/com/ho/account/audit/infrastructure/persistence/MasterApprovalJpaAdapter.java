package com.ho.account.audit.infrastructure.persistence;

import com.ho.account.audit.application.port.out.MasterApprovalPersistencePort;
import com.ho.account.audit.domain.MasterApproval;
import com.ho.account.audit.repository.MasterApprovalRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MasterApprovalJpaAdapter implements MasterApprovalPersistencePort {

    private final MasterApprovalRepository masterApprovalRepository;

    @Override
    public MasterApproval save(MasterApproval approval) {
        return masterApprovalRepository.save(approval);
    }

    @Override
    public Optional<MasterApproval> findById(Long id) {
        return masterApprovalRepository.findById(id);
    }

    @Override
    public List<MasterApproval> findByStatus(MasterApproval.ApprovalStatus status) {
        return masterApprovalRepository.findByStatus(status);
    }
}

