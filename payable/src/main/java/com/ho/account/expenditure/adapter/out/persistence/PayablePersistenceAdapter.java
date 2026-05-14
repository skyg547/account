package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.repository.PayableRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [PayablePersistenceAdapter]
 * Payable 도메인의 영속성 처리를 담당합니다.
 * 리팩토링된 Repository 메서드와 연결합니다.
 */
@Component
public class PayablePersistenceAdapter implements PayablePersistencePort {

    private final PayableRepository payableRepository;

    public PayablePersistenceAdapter(PayableRepository payableRepository) {
        this.payableRepository = payableRepository;
    }

    @Override
    public Payable save(Payable payable) {
        return payableRepository.save(payable);
    }

    @Override
    public Optional<Payable> findById(Long id) {
        return payableRepository.findById(id);
    }

    @Override
    public List<Payable> findByDueDateBeforeAndStatusNot(LocalDate date, PayableStatus status) {
        return payableRepository.findByDueDateBeforeAndStatusNot(date, status);
    }

    @Override
    public List<Payable> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount) {
        return payableRepository.findByVendorCodeAndOutstandingAmountGreaterThan(vendorCode, amount);
    }
}
