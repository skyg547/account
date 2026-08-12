package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PaymentPersistencePort;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.infrastructure.persistence.entity.PaymentJpaEntity;
import com.ho.account.expenditure.infrastructure.persistence.mapper.PaymentMapper;
import com.ho.account.expenditure.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * [PaymentPersistenceAdapter]
 * 지급 영속성 처리를 담당합니다.
 */
@Component
public class PaymentPersistenceAdapter implements PaymentPersistencePort {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public PaymentPersistenceAdapter(PaymentRepository paymentRepository,
                                     PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = paymentMapper.toEntity(payment);
        PaymentJpaEntity savedEntity = paymentRepository.save(entity);
        return paymentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toDomain);
    }

    @Override
    public List<Payment> findByPaymentRunId(Long paymentRunId) {
        return paymentRepository.findByPaymentRunId(paymentRunId).stream()
                .map(paymentMapper::toDomain)
                .toList();
    }
}
