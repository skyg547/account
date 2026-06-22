package com.ho.account.receivable.adapter.out.persistence;

import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.ReceivableStatus;
import com.ho.account.receivable.repository.ReceivableRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ReceivablePersistenceAdapter implements ReceivablePersistencePort {

    private final ReceivableRepository receivableRepository;

    public ReceivablePersistenceAdapter(ReceivableRepository receivableRepository) {
        this.receivableRepository = receivableRepository;
    }

    @Override
    public Receivable save(Receivable receivable) {
        return receivableRepository.save(receivable);
    }

    @Override
    public Optional<Receivable> findById(Long id) {
        return receivableRepository.findById(id);
    }

    @Override
    public List<Receivable> findByDueDateBeforeAndStatusNot(LocalDate date, ReceivableStatus status) {
        return receivableRepository.findByDueDateBeforeAndStatusNot(date, status);
    }

    @Override
    public List<Receivable> findByCustomerCodeAndStatus(String customerCode, ReceivableStatus status) {
        return receivableRepository.findByCustomerCodeAndStatus(customerCode, status);
    }

    @Override
    public List<Receivable> findOpenItemsByCustomerCode(String customerCode) {
        return receivableRepository.findByCustomerCodeAndStatusIn(
                customerCode,
                List.of(ReceivableStatus.OPEN, ReceivableStatus.PARTIAL_PAID, ReceivableStatus.OVERDUE));
    }
}
