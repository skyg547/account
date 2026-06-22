package com.ho.account.receivable.application.port.out;

import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.ReceivableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReceivablePersistencePort {
    Receivable save(Receivable receivable);
    Optional<Receivable> findById(Long id);
    List<Receivable> findByDueDateBeforeAndStatusNot(LocalDate date, ReceivableStatus status);
    List<Receivable> findByCustomerCodeAndStatus(String customerCode, ReceivableStatus status);
    List<Receivable> findOpenItemsByCustomerCode(String customerCode);
}
