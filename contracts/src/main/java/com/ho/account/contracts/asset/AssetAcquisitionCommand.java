package com.ho.account.contracts.asset;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetAcquisitionCommand(
        String assetCode,
        String assetName,
        String accountCode,
        LocalDate acquisitionDate,
        BigDecimal acquisitionCost,
        String departmentCode,
        Integer usefulLife,
        String depreciationMethod,
        String status
) {
}
