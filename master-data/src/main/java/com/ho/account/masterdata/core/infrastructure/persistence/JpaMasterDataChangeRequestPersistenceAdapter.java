package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.port.out.MasterDataChangeRequestPersistencePort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaMasterDataChangeRequestPersistenceAdapter implements MasterDataChangeRequestPersistencePort {

    private final MasterDataChangeRequestJpaRepository repository;

    public JpaMasterDataChangeRequestPersistenceAdapter(MasterDataChangeRequestJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MasterDataChangeRequest> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<MasterDataChangeRequest> findByStatus(ChangeStatus status) {
        return repository.findByStatusOrderByRequestedAtAsc(status);
    }

    @Override
    public MasterDataChangeRequest save(MasterDataChangeRequest request) {
        return repository.save(request);
    }
}
