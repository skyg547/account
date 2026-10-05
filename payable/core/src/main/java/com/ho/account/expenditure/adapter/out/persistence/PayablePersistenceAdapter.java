package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableJpaEntity;
import com.ho.account.expenditure.infrastructure.persistence.mapper.PayableMapper;
import com.ho.account.expenditure.repository.PayableRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [PayablePersistenceAdapter]
 * 매입채무 영속성 처리를 담당합니다.
 */
@Component
public class PayablePersistenceAdapter implements PayablePersistencePort {

    private static final List<PayableStatus> PAYMENT_ELIGIBLE_STATUSES = List.of(
            PayableStatus.OPEN,
            PayableStatus.APPROVED,
            PayableStatus.UNPAID,
            PayableStatus.PARTIAL_PAID,
            PayableStatus.OVERDUE);

    private final PayableRepository payableRepository;
    private final PayableMapper payableMapper;

    public PayablePersistenceAdapter(PayableRepository payableRepository,
                                     PayableMapper payableMapper) {
        this.payableRepository = payableRepository;
        this.payableMapper = payableMapper;
    }

    @Override
    public Payable save(Payable payable) {
        PayableJpaEntity entity = payableMapper.toEntity(payable);
        PayableJpaEntity savedEntity = payableRepository.save(entity);
        return payableMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Payable> findById(Long id) {
        return payableRepository.findById(id)
                .map(payableMapper::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int claimForPayment(Long payableId) {
        // The claim and Payment insert must commit or roll back together.
        return payableRepository.claimForPayment(
                payableId, PayableStatus.IN_PAYMENT, PAYMENT_ELIGIBLE_STATUSES, BigDecimal.ZERO);
    }

    @Override
    public List<Payable> findByDueDateBeforeAndStatusNot(LocalDate date, PayableStatus status) {
        return payableRepository.findByDueDateBeforeAndStatusNot(date, status).stream()
                .map(payableMapper::toDomain)
                .toList();
    }

    @Override
    public List<Payable> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount) {
        return payableRepository.findByVendorCodeAndOutstandingAmountGreaterThan(vendorCode, amount).stream()
                .map(payableMapper::toDomain)
                .toList();
    }
}
