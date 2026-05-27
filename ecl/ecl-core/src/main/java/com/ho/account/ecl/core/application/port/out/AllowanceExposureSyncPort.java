package com.ho.account.ecl.core.application.port.out;

import java.time.LocalDate;

public interface AllowanceExposureSyncPort {

    int countSourceSnapshots(LocalDate baseDate);

    int upsertCustomersFromSnapshot(LocalDate baseDate);

    int upsertAccountsFromSnapshot(LocalDate baseDate);
}
