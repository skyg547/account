package com.ho.account.contracts.closing;

import java.time.LocalDate;

public interface AccountingPeriodStatusPort {

    boolean isClosed(LocalDate accountingDate);
}
