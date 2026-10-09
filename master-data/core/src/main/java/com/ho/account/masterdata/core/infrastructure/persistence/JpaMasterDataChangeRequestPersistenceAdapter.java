package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.MasterDataChangeRequestEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.MasterDataChangeRequestMapper;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaMasterDataChangeRequestPersistenceAdapter implements MasterDataChangeRequestPersistencePort {

    private final MasterDataChangeRequestJpaRepository repository;
    private final MasterDataChangeRequestMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

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
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<MasterDataChangeRequest> findByIdForUpdate(Long id) {
        return repository.findById(id).map(entity -> {
            // 이전 query의 자동 flush 의미를 보존해 같은 트랜잭션의 승인/반영 저장을 잃지 않습니다.
            entityManager.flush();
            // 기존 @Lock query는 이미 읽은 객체의 오래된 @Version을 refresh 전에 검사할 수 있습니다.
            // refresh 한 번으로 행 잠금과 최신 상태/lockVersion 재조회를 함께 수행합니다.
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return mapper.toDomain(entity);
        });
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
