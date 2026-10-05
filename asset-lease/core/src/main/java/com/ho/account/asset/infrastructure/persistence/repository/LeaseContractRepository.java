package com.ho.account.asset.infrastructure.persistence.repository;

import com.ho.account.asset.domain.LeaseContract;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 인프라스트럭처 영속성 계층 (Infrastructure Persistence Repository)]
 * 리스 계약(LeaseContract) 엔티티를 관리하는 JPA 리포지토리 인터페이스입니다.
 * 
 * 💡 [교육적 주석 - 헥사고날/DIP 원칙]
 * 본 인터페이스는 인프라스트럭처 레이어에 위치하며, LeasePersistenceAdapter에서만 직접 주입받아 사용합니다.
 * 핵심 도메인 로직 및 서비스는 본 인터페이스 대신 LeasePersistencePort 인터페이스에 의존합니다.
 */
@Repository
public interface LeaseContractRepository extends JpaRepository<LeaseContract, Long> {
    // Serialize remeasurements before reading balances and future rows, including extension inserts.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LeaseContract c where c.id = :id")
    Optional<LeaseContract> findByIdForUpdate(@Param("id") Long id);

    List<LeaseContract> findByStatus(String status);
    List<LeaseContract> findByStatusAndEndDateBefore(String status, LocalDate date);

    /**
     * IFRS 16 적용 대상이며 특정 상태인 모든 리스 계약을 조회합니다.
     *
     * @param status 조회할 리스 계약의 상태 (예: "ACTIVE")
     * @return IFRS 16 적용 대상인 리스 계약 리스트
     */
    List<LeaseContract> findByIfrs16ApplicableTrueAndStatus(String status);
}
