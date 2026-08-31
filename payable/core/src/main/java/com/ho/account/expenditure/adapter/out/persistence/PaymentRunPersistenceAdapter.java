package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PaymentRunPersistencePort;
import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.infrastructure.persistence.entity.PaymentRunJpaEntity;
import com.ho.account.expenditure.infrastructure.persistence.mapper.PaymentRunMapper;
import com.ho.account.expenditure.repository.PaymentRunRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [PaymentRunPersistenceAdapter]
 * 지급 실행 영속성 처리를 담당합니다.
 */
@Component
public class PaymentRunPersistenceAdapter implements PaymentRunPersistencePort {

    private final PaymentRunRepository paymentRunRepository;
    private final PaymentRunMapper paymentRunMapper;

    public PaymentRunPersistenceAdapter(PaymentRunRepository paymentRunRepository,
                                         PaymentRunMapper paymentRunMapper) {
        this.paymentRunRepository = paymentRunRepository;
        this.paymentRunMapper = paymentRunMapper;
    }

    @Override
    public PaymentRun save(PaymentRun paymentRun) {
        PaymentRunJpaEntity entity = paymentRunMapper.toEntity(paymentRun);
        PaymentRunJpaEntity savedEntity = paymentRunRepository.save(entity);
        return paymentRunMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PaymentRun> findById(Long id) {
        return paymentRunRepository.findById(id)
                .map(paymentRunMapper::toDomain);
    }

    @Override
    public Optional<PaymentRun> findByRunDateAndDescriptionAndCreatedBy(
            LocalDate runDate, String description, String createdBy) {
        List<PaymentRunJpaEntity> matches = paymentRunRepository.findMatchingPaymentRuns(runDate, description, createdBy);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(paymentRunMapper.toDomain(matches.get(0)));
    }
}
