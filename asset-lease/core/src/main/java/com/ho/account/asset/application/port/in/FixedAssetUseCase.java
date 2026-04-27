package com.ho.account.asset.application.port.in;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.masterdata.core.domain.model.Department;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface FixedAssetUseCase {
    FixedAsset registerAsset(FixedAsset asset);
    void processMonthlyDepreciation(LocalDate processDate);
    FixedAsset disposeFixedAsset(Long assetId, LocalDate disposalDate, BigDecimal salePrice);
    void changeDepartment(Long assetId, Department newDept, String reason);
}
