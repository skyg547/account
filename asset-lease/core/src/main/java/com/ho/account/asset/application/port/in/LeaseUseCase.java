package com.ho.account.asset.application.port.in;

import com.ho.account.asset.domain.LeaseContract;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaseUseCase {
    LeaseContract registerLeaseContract(LeaseContract contract);
    void processMonthlyLeaseAccounting(LocalDate processDate);
    void processMonthlyLeasePayment(LocalDate paymentDate);
    LeaseContract remeasureLease(Long contractId, LocalDate remeasureDate, BigDecimal newPayment, LocalDate newEndDate, BigDecimal newRate);
    Optional<LeaseContract> getLeaseContract(Long id);
    List<LeaseContract> getAllActiveLeaseContracts();
}
