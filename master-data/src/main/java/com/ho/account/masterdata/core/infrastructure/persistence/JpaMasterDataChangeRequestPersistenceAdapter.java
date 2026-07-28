package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
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
    public Optional<MasterDataChangeRequest> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id);
    }

    @Override
    public Optional<MasterDataChangeRequest> findBySourceReference(String sourceReference) {
        return repository.findBySourceReference(sourceReference);
    }

    @Override
    public List<MasterDataChangeRequest> findByStatus(ChangeStatus status) {
        return repository.findByStatusOrderByRequestedAtAsc(status);
    }

    @Override
    public List<MasterDataChangeRequest> findReadyToApply(LocalDate effectiveDate, int limit) {
        return repository.findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateAscRequestedAtAsc(
                ChangeStatus.APPROVED,
                effectiveDate,
                PageRequest.of(0, limit));
    }

    @Override
    public MasterDataChangeRequest save(MasterDataChangeRequest request) {
        return repository.save(request);
    }
}
