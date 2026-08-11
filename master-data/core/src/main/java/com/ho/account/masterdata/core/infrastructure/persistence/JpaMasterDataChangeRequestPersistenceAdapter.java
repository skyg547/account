package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.MasterDataChangeRequestEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.MasterDataChangeRequestMapper;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class JpaMasterDataChangeRequestPersistenceAdapter implements MasterDataChangeRequestPersistencePort {

    private final MasterDataChangeRequestJpaRepository repository;
    private final MasterDataChangeRequestMapper mapper;

    public JpaMasterDataChangeRequestPersistenceAdapter(
            MasterDataChangeRequestJpaRepository repository,
            MasterDataChangeRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<MasterDataChangeRequest> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MasterDataChangeRequest> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MasterDataChangeRequest> findBySourceReference(String sourceReference) {
        return repository.findBySourceReference(sourceReference).map(mapper::toDomain);
    }

    @Override
    public List<MasterDataChangeRequest> findByStatus(ChangeStatus status) {
        return repository.findByStatusOrderByRequestedAtAsc(status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MasterDataChangeRequest> findReadyToApply(LocalDate effectiveDate, int limit) {
        return repository.findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateAscRequestedAtAsc(
                ChangeStatus.APPROVED,
                effectiveDate,
                PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public MasterDataChangeRequest save(MasterDataChangeRequest request) {
        MasterDataChangeRequestEntity entity = mapper.toEntity(request);
        MasterDataChangeRequestEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}

