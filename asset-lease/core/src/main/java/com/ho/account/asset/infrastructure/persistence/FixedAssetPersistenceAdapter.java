package com.ho.account.asset.infrastructure.persistence;

import com.ho.account.asset.application.port.out.FixedAssetPersistencePort;
import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.infrastructure.persistence.repository.AssetHistoryRepository;
import com.ho.account.asset.infrastructure.persistence.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 영속성 아웃바운드 어댑터 (Persistence Outbound Adapter)]
 * FixedAssetPersistencePort를 구현하여 JPA Repository를 통해 DB에 영속화하는 어댑터 클래스입니다.
 * 
 * 💡 [교육적 주석 - 영속성 메커니즘 캡슐화 & DIP 구현]
 * 1. 영속성 메커니즘 캡슐화:
 *    Spring Data JPA의 FixedAssetRepository, AssetHistoryRepository 구현체 조작은
 *    이 Adapter 클래스 내부로 격리(Encapsulated)됩니다.
 *    애플리케이션 서비스와 배치 어케스트레이터는 JpaRepository를 몰라도 Port 인터페이스만을 통해 DB 조회를 수행할 수 있습니다.
 * 
 * 2. DIP 구현:
 *    저수준 데이터베이스 액세스 구현인 FixedAssetRepository에 의존하는 고수준 호출부가 직접 결합되는 것을 막고,
 *    FixedAssetPersistencePort라는 고수준 인터페이스를 통해서만 협력하도록 의존관계를 역전(Inversion)시켰습니다.
 */
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
    public Page<FixedAsset> findByStatus(String status, Pageable pageable) {
        return fixedAssetRepository.findByStatus(status, pageable);
    }

    @Override
    public void saveHistory(AssetHistory history) {
        assetHistoryRepository.save(history);
    }
}

