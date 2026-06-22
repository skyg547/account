package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayablePersistencePort {
    Payable save(Payable payable);
    Optional<Payable> findById(Long id);
    List<Payable> findByDueDateBeforeAndStatusNot(LocalDate date, PayableStatus status);
    List<Payable> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount);
}
