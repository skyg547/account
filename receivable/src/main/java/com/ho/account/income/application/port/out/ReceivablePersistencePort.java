package com.ho.account.income.application.port.out;

import com.ho.account.income.domain.Receivable;
import com.ho.account.income.domain.ReceivableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReceivablePersistencePort {
    Receivable save(Receivable receivable);
    Optional<Receivable> findById(Long id);
    List<Receivable> findByDueDateBeforeAndStatusNot(LocalDate date, ReceivableStatus status);
    List<Receivable> findByCustomerCodeAndStatus(String customerCode, ReceivableStatus status);
}
