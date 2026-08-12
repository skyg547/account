package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.AdvancePaymentPersistencePort;
import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.infrastructure.persistence.entity.AdvancePaymentJpaEntity;
import com.ho.account.expenditure.infrastructure.persistence.mapper.AdvancePaymentMapper;
import com.ho.account.expenditure.repository.AdvancePaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * [AdvancePaymentPersistenceAdapter]
 * 선급금 영속성 처리를 담당합니다.
 */
@Component
public class AdvancePaymentPersistenceAdapter implements AdvancePaymentPersistencePort {

    private final AdvancePaymentRepository advancePaymentRepository;
    private final AdvancePaymentMapper advancePaymentMapper;

    public AdvancePaymentPersistenceAdapter(AdvancePaymentRepository advancePaymentRepository,
                                            AdvancePaymentMapper advancePaymentMapper) {
        this.advancePaymentRepository = advancePaymentRepository;
        this.advancePaymentMapper = advancePaymentMapper;
    }

    @Override
    public AdvancePayment save(AdvancePayment advancePayment) {
        AdvancePaymentJpaEntity entity = advancePaymentMapper.toEntity(advancePayment);
        AdvancePaymentJpaEntity savedEntity = advancePaymentRepository.save(entity);
        return advancePaymentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AdvancePayment> findById(Long id) {
        return advancePaymentRepository.findById(id)
                .map(advancePaymentMapper::toDomain);
    }
}
