package com.ho.account.asset.repository;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.RightOfUseAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용권자산(RightOfUseAsset) 엔티티를 관리하는 JPA 리포지토리.
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
