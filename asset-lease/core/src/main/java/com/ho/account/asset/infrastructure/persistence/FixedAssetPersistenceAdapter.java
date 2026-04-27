package com.ho.account.asset.infrastructure.persistence;

import com.ho.account.asset.application.port.out.FixedAssetPersistencePort;
import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.repository.AssetHistoryRepository;
import com.ho.account.asset.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FixedAssetPersistenceAdapter implements FixedAssetPersistencePort {

    private final FixedAssetRepository fixedAssetRepository;
    private final AssetHistoryRepository assetHistoryRepository;

    @Override
    public FixedAsset save(FixedAsset asset) {
        return fixedAssetRepository.save(asset);
    }

    @Override
    public Optional<FixedAsset> findById(Long id) {
        return fixedAssetRepository.findById(id);
    }

    @Override
    public List<FixedAsset> findByStatus(String status) {
        return fixedAssetRepository.findByStatus(status);
    }

    @Override
    public void saveHistory(AssetHistory history) {
        assetHistoryRepository.save(history);
    }
}
