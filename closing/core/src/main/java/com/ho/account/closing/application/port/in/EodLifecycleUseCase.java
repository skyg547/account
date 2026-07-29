package com.ho.account.closing.application.port.in;

import com.ho.account.closing.domain.DailyClosingStatus;

import java.time.LocalDate;

public interface EodLifecycleUseCase {

    DailyClosingStatus findStatus(LocalDate businessDate);

    DailyClosingStatus bootstrap(LocalDate businessDate, String actor);

    DailyClosingStatus prepareEod(LocalDate businessDate, String actor);

    DailyClosingStatus cancelEodPreparation(LocalDate businessDate, String actor);

    DailyClosingStatus startEod(LocalDate businessDate, String actor);

    DailyClosingStatus completeEod(LocalDate businessDate, String actor);

    DailyClosingStatus startBod(LocalDate closedDate, LocalDate nextBusinessDate, String actor);

    DailyClosingStatus completeBod(LocalDate businessDate, String actor);
}
