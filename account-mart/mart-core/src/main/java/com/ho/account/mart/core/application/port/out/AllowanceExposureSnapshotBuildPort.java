package com.ho.account.mart.core.application.port.out;

import java.time.LocalDate;

public interface AllowanceExposureSnapshotBuildPort {

    int countSourcePositions(LocalDate baseDate);

    void deleteByBaseDate(LocalDate baseDate);

    int insertFromIntegratedPositions(LocalDate baseDate);
}
