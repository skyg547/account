package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PaymentPersistencePort;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PaymentPersistenceAdapter implements PaymentPersistencePort {

    private final PaymentRepository paymentRepository;

    public PaymentPersistenceAdapter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Override
    public List<Payment> findByPaymentRunId(Long paymentRunId) {
        return paymentRepository.findByPaymentRunId(paymentRunId);
    }
}
