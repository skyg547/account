package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.AdvancePaymentPersistencePort;
import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.repository.AdvancePaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdvancePaymentPersistenceAdapter implements AdvancePaymentPersistencePort {

    private final AdvancePaymentRepository advancePaymentRepository;

    public AdvancePaymentPersistenceAdapter(AdvancePaymentRepository advancePaymentRepository) {
        this.advancePaymentRepository = advancePaymentRepository;
    }

    @Override
    public AdvancePayment save(AdvancePayment advancePayment) {
        return advancePaymentRepository.save(advancePayment);
    }

    @Override
    public Optional<AdvancePayment> findById(Long id) {
        return advancePaymentRepository.findById(id);
    }
}
