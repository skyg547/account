package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayableRepository extends JpaRepository<PayableJpaEntity, Long> {
    Optional<PayableJpaEntity> findByPurchaseInvoiceNoAndPurchaseInvoiceVendorCode(String purchaseInvoiceNo,
            String purchaseInvoiceVendorCode);
    List<PayableJpaEntity> findByVendorCodeAndStatus(String vendorCode, PayableStatus status);
    List<PayableJpaEntity> findByDueDateBeforeAndStatusNot(LocalDate dueDate, PayableStatus status);
    List<PayableJpaEntity> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount);

    // The primary-key update serializes competing claims; the loser sees IN_PAYMENT and updates zero rows.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PayableJpaEntity p SET p.status = :claimedStatus "
            + "WHERE p.id = :payableId AND p.status IN :eligibleStatuses "
            + "AND p.outstandingAmount > :zero")
    int claimForPayment(@Param("payableId") Long payableId,
                        @Param("claimedStatus") PayableStatus claimedStatus,
                        @Param("eligibleStatuses") List<PayableStatus> eligibleStatuses,
                        @Param("zero") BigDecimal zero);
}
