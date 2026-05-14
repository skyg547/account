package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PaymentRunPersistencePort;
import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.repository.PaymentRunRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentRunPersistenceAdapter implements PaymentRunPersistencePort {

    private final PaymentRunRepository paymentRunRepository;

    public PaymentRunPersistenceAdapter(PaymentRunRepository paymentRunRepository) {
        this.paymentRunRepository = paymentRunRepository;
    }

    @Override
    public PaymentRun save(PaymentRun paymentRun) {
        return paymentRunRepository.save(paymentRun);
    }

    @Override
    public Optional<PaymentRun> findById(Long id) {
        return paymentRunRepository.findById(id);
    }
}
