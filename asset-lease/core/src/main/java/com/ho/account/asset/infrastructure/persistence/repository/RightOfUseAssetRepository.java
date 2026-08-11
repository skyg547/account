package com.ho.account.asset.infrastructure.persistence.repository;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.RightOfUseAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 인프라스트럭처 영속성 계층 (Infrastructure Persistence Repository)]
 * 사용권자산(RightOfUseAsset) 엔티티를 관리하는 JPA 리포지토리 인터페이스입니다.
 * 
 * 💡 [교육적 주석 - 영속성 기술 캡슐화]
 * 사용권자산 엔티티 조회/저장 메커니즘을 Spring Data JPA의 JpaRepository로 구현하며,
 * 이 구체적인 메커니즘은 인프라 어댑터 내부에 은닉됩니다.
 */
@Repository
public interface RightOfUseAssetRepository extends JpaRepository<RightOfUseAsset, Long> {
    /**
     * 특정 리스 계약(LeaseContract)에 연결된 사용권자산을 조회합니다.
     *
     * @param leaseContract 조회할 리스 계약 엔티티
     * @return Optional<RightOfUseAsset> 사용권자산 엔티티 (존재하지 않을 수 있음)
     */
    Optional<RightOfUseAsset> findByLeaseContract(LeaseContract leaseContract);
}
