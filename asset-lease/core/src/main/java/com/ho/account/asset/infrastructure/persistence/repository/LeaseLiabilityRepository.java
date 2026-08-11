package com.ho.account.asset.infrastructure.persistence.repository;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 인프라스트럭처 영속성 계층 (Infrastructure Persistence Repository)]
 * 리스부채(LeaseLiability) 엔티티를 관리하는 JPA 리포지토리 인터페이스입니다.
 * 
 * 💡 [교육적 주석 - 영속성 기술 캡슐화]
 * JPA 종속 기술(JpaRepository)을 infrastructure 계층에 위치시킴으로써 
 * Core 도메인이 특정 데이터 접근 기술에 오염되지 않도록 격리합니다.
 */
@Repository
public interface LeaseLiabilityRepository extends JpaRepository<LeaseLiability, Long> {
    /**
     * 특정 리스 계약(LeaseContract)에 연결된 리스부채를 조회합니다.
     *
     * @param leaseContract 조회할 리스 계약 엔티티
     * @return Optional<LeaseLiability> 리스부채 엔티티 (존재하지 않을 수 있음)
     */
    Optional<LeaseLiability> findByLeaseContract(LeaseContract leaseContract);
}
