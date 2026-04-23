package com.ho.account.asset.repository;

import com.ho.account.asset.domain.LeaseContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaseContractRepository extends JpaRepository<LeaseContract, Long> {
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
