package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.APPaymentPersistencePort;
import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.repository.APPaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class APPaymentPersistenceAdapter implements APPaymentPersistencePort {

    private final APPaymentRepository repository;

    public APPaymentPersistenceAdapter(APPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public APPayment save(APPayment apPayment) {
        return repository.save(apPayment);
    }

    @Override
    public Optional<APPayment> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<APPayment> findByExpenditureResolutionId(Long expenditureResolutionId) {
        return repository.findByExpenditureResolutionId(expenditureResolutionId);
    }

    @Override
    public void delete(APPayment apPayment) {
        repository.delete(apPayment);
    }
}
