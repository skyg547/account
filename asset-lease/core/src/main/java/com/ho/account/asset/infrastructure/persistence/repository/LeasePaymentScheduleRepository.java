package com.ho.account.asset.infrastructure.persistence.repository;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [헥사고날 아키텍처 - 인프라스트럭처 영속성 계층 (Infrastructure Persistence Repository)]
 * 리스 상환 스케줄(LeasePaymentSchedule) 엔티티를 관리하는 JPA 리포지토리 인터페이스입니다.
 * 
 * 💡 [교육적 주석 - 영속성 기술 캡슐화]
 * JpaRepository의 구체적인 조회 메서드들은 인프라스트럭처 영속성 어댑터 내부에서 호출되며,
 * 애플리케이션 서비스는 필요에 따라 포트(LeasePersistencePort) 인터페이스 메서드를 통해 접근합니다.
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
