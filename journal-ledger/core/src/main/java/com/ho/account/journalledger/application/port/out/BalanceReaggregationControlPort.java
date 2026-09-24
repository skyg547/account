package com.ho.account.journalledger.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Persistent publication barrier and compact reconciliation reads for balance reaggregation. */
public interface BalanceReaggregationControlPort {

    ControlSnapshot snapshot();

    ControlSnapshot start(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate);

    void assertOpen();

    void assertOwner(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate);

    ReconciliationData loadReconciliationData(LocalDate startDate, LocalDate endDate);

    void release(long ownerJobInstanceId, LocalDate startDate, LocalDate endDate);

    enum Status { OPEN, REBUILDING }

    record ControlSnapshot(Status status, Long ownerJobInstanceId,
                           LocalDate startDate, LocalDate endDate, long epoch) {
        public boolean isOpen() {
            return status == Status.OPEN;
        }
    }

    record GlKey(String accountCode, String currencyCode) {}
    record SlKey(String accountCode, String businessPartnerCode, String departmentCode, String currencyCode) {}

    record GlMovement(LocalDate date, GlKey key, BigDecimal debit, BigDecimal credit) {}
    record SlMovement(LocalDate date, SlKey key, BigDecimal debit, BigDecimal credit) {}
    record GlActual(LocalDate date, GlKey key, BigDecimal beginning, BigDecimal debit,
                    BigDecimal credit, BigDecimal ending) {}
    record SlActual(LocalDate date, SlKey key, BigDecimal beginning, BigDecimal debit,
                    BigDecimal credit, BigDecimal ending) {}
    record GlPrior(GlKey key, BigDecimal ending) {}
    record SlPrior(SlKey key, BigDecimal ending) {}

    record ReconciliationData(List<GlMovement> glMovements, List<SlMovement> slMovements,
                              List<GlActual> glActuals, List<SlActual> slActuals,
                              List<GlPrior> glPriors, List<SlPrior> slPriors) {}
}
