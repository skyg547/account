package com.ho.account.asset.application.port.in;

import com.ho.account.asset.domain.FixedAsset;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FixedAssetUseCase {
    FixedAsset registerAsset(FixedAsset asset);
    void processMonthlyDepreciation(LocalDate processDate);
    FixedAsset disposeFixedAsset(Long assetId, LocalDate disposalDate, BigDecimal salePrice);
    void changeDepartment(Long assetId, String newDeptCode, String reason);
    List<FixedAsset> findByStatus(String status);
    Optional<FixedAsset> findById(Long id);
}
