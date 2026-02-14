package com.ho.account.asset.repository;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 리스 상환 스케줄(LeasePaymentSchedule) 엔티티를 관리하는 JPA 리포지토리.
 */
@Repository
public interface LeasePaymentScheduleRepository extends JpaRepository<LeasePaymentSchedule, Long> {
    /**
     * 특정 리스 계약(LeaseContract)에 대한 모든 상환 스케줄을 조회합니다.
     *
     * @param leaseContract 조회할 리스 계약 엔티티
     * @return 해당 리스 계약의 상환 스케줄 리스트
     */
    List<LeasePaymentSchedule> findByLeaseContractOrderByPaymentDateAsc(LeaseContract leaseContract);

    /**
     * 특정 리스 계약과 지급 예정일 이전에 해당하는 모든 상환 스케줄을 조회합니다.
     *
     * @param leaseContract 조회할 리스 계약 엔티티
     * @param paymentDate 기준 지급 예정일
     * @return 해당 조건의 상환 스케줄 리스트
     */
    List<LeasePaymentSchedule> findByLeaseContractAndPaymentDateBeforeOrderByPaymentDateAsc(LeaseContract leaseContract, LocalDate paymentDate);
}
