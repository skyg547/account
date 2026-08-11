package com.ho.account.asset.infrastructure.persistence.repository;

import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * [헥사고날 아키텍처 - 인프라스트럭처 영속성 계층 (Infrastructure Persistence Repository)]
 * 자산 이력(AssetHistory) 엔티티에 대한 Spring Data JPA 영속성 인터페이스입니다.
 * 
 * 💡 [교육적 주석 - 헥사고날/DIP 이점]
 * JpaRepository는 Spring Data JPA라는 특정 프레임워크 기술에 종속된 인터페이스입니다.
 * 도메인과 애플리케이션 서비스가 프레임워크 변경이나 DB 기술 교체 시 영향을 받지 않도록
 * infrastructure 패키지에 격리되어 인프라 어댑터(FixedAssetPersistenceAdapter)에 의해 캡슐화됩니다.
 */
@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistory, Long> {
    List<AssetHistory> findByFixedAssetOrderByEventAtDesc(FixedAsset asset);
}
