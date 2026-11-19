package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.ReceivableStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.ReceivableJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.mapper.ReceivableMapper;
import com.ho.account.receivable.repository.ReceivableRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ReceivablePersistenceAdapter implements ReceivablePersistencePort {

    private final ReceivableRepository receivableRepository;
    private final ReceivableMapper receivableMapper;
    private final EntityManager entityManager;

    public ReceivablePersistenceAdapter(ReceivableRepository receivableRepository,
                                         ReceivableMapper receivableMapper,
                                         EntityManager entityManager) {
        this.receivableRepository = receivableRepository;
        this.receivableMapper = receivableMapper;
        this.entityManager = entityManager;
    }

    @Override
    public Receivable save(Receivable receivable) {
        ReceivableJpaEntity entity = receivableMapper.toEntity(receivable);
        ReceivableJpaEntity savedEntity = receivableRepository.save(entity);
        return receivableMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Receivable> findById(Long id) {
        return receivableRepository.findById(id)
                .map(receivableMapper::toDomain);
    }

    @Override
    public Optional<Receivable> findByIdForUpdate(Long id) {
        return receivableRepository.findByIdForUpdate(id).map(entity -> {
            // Auto matching queried candidates earlier in this transaction; refresh after the lock
            // so an already-managed JPA entity cannot retain a stale outstanding balance.
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return receivableMapper.toDomain(entity);
        });
    }

    @Override
    public List<Receivable> findByDueDateBeforeAndStatusNot(LocalDate date, ReceivableStatus status) {
        return receivableRepository.findByDueDateBeforeAndStatusNot(date, status).stream()
                .map(receivableMapper::toDomain)
                .toList();
    }

    @Override
    public List<Receivable> findByCustomerCodeAndStatus(String customerCode, ReceivableStatus status) {
        return receivableRepository.findByCustomerCodeAndStatus(customerCode, status).stream()
                .map(receivableMapper::toDomain)
                .toList();
    }

    @Override
    public List<Receivable> findOpenItemsByCustomerCode(String customerCode) {
        return receivableRepository.findByCustomerCodeAndStatusIn(
                customerCode,
                List.of(ReceivableStatus.OPEN, ReceivableStatus.PARTIAL_PAID, ReceivableStatus.OVERDUE)).stream()
                .map(receivableMapper::toDomain)
                .toList();
    }
}
