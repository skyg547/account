package com.ho.account.journalledger.application.port.out;

import java.time.LocalDate;

/** Retains the journal revision lock until the caller's actual transaction completes. */
public interface JournalMonetaryAdmissionPort {
    void admit(LocalDate accountingDate);
    void beginReaggregation(LocalDate startDate);
    void completeReaggregation();
}
