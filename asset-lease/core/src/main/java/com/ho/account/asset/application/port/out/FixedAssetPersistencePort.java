package com.ho.account.asset.application.port.out;

import com.ho.account.asset.domain.AssetHistory;
import com.ho.account.asset.domain.FixedAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 아웃바운드 포트 (Outbound Port)]
 * 고정자산(FixedAsset) 영속성을 위한 추상화 포트 인터페이스입니다.
 * 
 * 💡 [교육적 주석 - 헥사고날 아키텍처 & DIP 이점]
 * 1. 헥사고날 아키텍처 (Ports and Adapters):
 *    - 애플리케이션 코어(Application Core)는 내부 비즈니스 로직에만 집중하며, DB나 외부 기술에 종속되지 않습니다.
 *    - 코어가 외부 영속성 저장소에 접근해야 할 때는 직접 DB나 ORM(JPA)에 접근하지 않고 이 포트(FixedAssetPersistencePort)를 호출합니다.
 *    - 실제 DB 구현체(FixedAssetPersistenceAdapter, FixedAssetRepository)는 인프라스트럭처 계층에서 이 포트를 구현(Implement)합니다.
 * 
 * 2. 의존관계 역전 원칙 (DIP - Dependency Inversion Principle):
 *    - "고수준 모듈(Domain/UseCase/Batch Orchestrator)은 저수준 모듈(JPA Repository/Infra Adapter)에 의존해서는 안 되며,
 *       둘 다 추상화(Port Interface)에 의존해야 한다."
 *    - 이 포트 인터페이스를 통해 Spring Batch 등 외부 모듈도 JPA Repository 인터페이스에 직접 의존하지 않고
 *      추상화된 포트에만 의존함으로써 결합도를 극대화하여 낮추고, 영속성 메커니즘을 온전히 캡슐화합니다.
 */
public interface FixedAssetPersistencePort {
    FixedAsset save(FixedAsset asset);
    Optional<FixedAsset> findById(Long id);
    List<FixedAsset> findByStatus(String status);
    Page<FixedAsset> findByStatus(String status, Pageable pageable);
    void saveHistory(AssetHistory history);
}

