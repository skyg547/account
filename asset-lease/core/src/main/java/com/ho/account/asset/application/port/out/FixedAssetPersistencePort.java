package com.ho.account.asset.application.port.out;

import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import java.util.List;
import java.util.Optional;

public interface FixedAssetPersistencePort {
    FixedAsset save(FixedAsset asset);
    Optional<FixedAsset> findById(Long id);
    List<FixedAsset> findByStatus(String status);
    void saveHistory(AssetHistory history);
}
